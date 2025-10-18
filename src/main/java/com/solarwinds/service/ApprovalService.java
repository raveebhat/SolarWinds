package com.solarwinds.service;

import com.solarwinds.domain.*;
import com.solarwinds.repo.RequestRepository;
import com.solarwinds.repo.TemplateRepository;
import com.solarwinds.repo.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApprovalService {

    private final UserRepository userRepo;
    private final TemplateRepository templateRepo;
    private final RequestRepository requestRepo;

    public ApprovalService(UserRepository userRepo, TemplateRepository templateRepo, RequestRepository requestRepo) {
        this.userRepo = userRepo;
        this.templateRepo = templateRepo;
        this.requestRepo = requestRepo;
    }

    public Request createRequest(String tenantId, String requesterId, String category, Map<String, String> metadata) {
        var id = UUID.randomUUID().toString();
        var request = new Request(id, tenantId, requesterId, category, metadata);
        requestRepo.save(request);
        return request;
    }

    // Submit with validation (ensures every step has at least one approver)
    public Request submitRequest(String requestId, String templateId) {
        var reqOpt = requestRepo.findById(requestId);
        if (reqOpt == null || reqOpt.isEmpty()) throw new IllegalArgumentException("Request not found: " + requestId);
        var req = reqOpt.get();

        var tplOpt = templateRepo.findLatestById(templateId);
        if (tplOpt == null || tplOpt.isEmpty()) throw new IllegalArgumentException("Template not found: " + templateId);
        var tpl = tplOpt.get();

        var stepInstances = new ArrayList<StepInstance>();
        for (var sd : tpl.steps) {
            // create the step instance always so we keep a stable step list
            var si = new StepInstance(sd.id, sd.name, sd.role);

            // evaluate simple condition key=value if present
            if (sd.condition != null && !sd.condition.isBlank()) {
                var parts = sd.condition.split("=", 2);
                if (parts.length == 2) {
                    var key = parts[0].trim();
                    var expected = parts[1].trim();
                    var actual = req.metadata.get(key);
                    if (actual == null || !actual.equals(expected)) {
                        // mark SKIPPED — keep step in list but no action required
                        si.markSkipped();
                        stepInstances.add(si);
                        continue;
                    }
                }
            }
            stepInstances.add(si);
        }

        // preflight: ensure at least one approver exists for each role that is NOT SKIPPED
        var rolesNeeded = stepInstances.stream()
                .filter(s -> s.status == StepStatus.PENDING) // only pending steps need approvers
                .map(s -> s.role)
                .distinct()
                .toList();

        for (var role : rolesNeeded) {
            var candidates = userRepo.findByTenantAndRole(req.tenantId, role);
            if (candidates == null || candidates.isEmpty())
                throw new IllegalStateException("No approvers in tenant '" + req.tenantId + "' for role: " + role);
        }

        var wi = new WorkflowInstance(tpl.id, tpl.version, stepInstances);
        // set currentIndex to first non-skipped step (or 0 if none)
        int firstPending = -1;
        for (int i = 0; i < stepInstances.size(); i++) {
            if (stepInstances.get(i).status == StepStatus.PENDING) { firstPending = i; break; }
        }
        if (firstPending == -1) {
            // no pending steps -> auto-approved
            wi.currentIndex = stepInstances.size(); // point past last
            req.workflowInstance = wi;
            req.status = RequestStatus.APPROVED;
        } else {
            wi.currentIndex = firstPending;
            req.workflowInstance = wi;
            req.status = RequestStatus.IN_REVIEW;
        }

        requestRepo.save(req);
        return req;
    }


    public Request act(String approverId, String requestId, Action action, String comment) {
        synchronized (("REQ_LOCK_" + requestId).intern()) {
            var userOpt = userRepo.findById(approverId);
            if (userOpt == null || userOpt.isEmpty()) throw new IllegalArgumentException("Approver not found: " + approverId);
            var user = userOpt.get();

            var reqOpt = requestRepo.findById(requestId);
            if (reqOpt == null || reqOpt.isEmpty()) throw new IllegalArgumentException("Request not found: " + requestId);
            var req = reqOpt.get();

            // tenant guard
            if (!user.tenantId().equals(req.tenantId)) {
                String msg = String.format("Tenant mismatch: user[%s]=%s request[%s]=%s", user.id(), user.tenantId(), req.id, req.tenantId);
//                System.out.println("[DEBUG] " + msg);
                throw new SecurityException(msg);
            }

            // requester can't act
            if (req.requesterId.equals(approverId)) throw new SecurityException("Requester cannot act on their own request: " + approverId);

            var wi = req.workflowInstance;
            if (wi == null) throw new IllegalStateException("Request not submitted to workflow: " + requestId);

            if (req.status != RequestStatus.IN_REVIEW) {
                throw new IllegalStateException("Request not in review: " + req.status);
            }

            int idx = wi.currentIndex;
            if (idx < 0 || idx >= wi.steps.size()) {
                throw new IllegalStateException("No active step for request: " + requestId + " (currentIndex=" + idx + ", steps=" + wi.steps.size() + ")");
            }

            var si = wi.steps.get(idx);

            // debug: print current step status before action (optional)
//            System.out.println("[DEBUG] Acting on request=" + requestId + " currentStepIndex=" + idx + " stepId=" + si.stepId + " stepStatus=" + si.status);

            if (!user.roles().contains(si.role)) {
                throw new SecurityException("User role mismatch. Required: " + si.role + ", user roles: " + user.roles());
            }

            // mark action
            si.mark(action, approverId, comment);
//            System.out.println("[DEBUG] Step " + si.stepId + " marked " + si.status + " by " + approverId);

            if (action == Action.REJECT) {
                req.status = RequestStatus.REJECTED;
                requestRepo.save(req);
                return req;
            }

            // APPROVE: find next PENDING step (skip SKIPPED)
            int nextPending = -1;
            for (int i = idx + 1; i < wi.steps.size(); i++) {
                var s = wi.steps.get(i);
                if (s.status == StepStatus.PENDING) { nextPending = i; break; }
            }

            if (nextPending == -1) {
                // no more pending steps -> fully approved
                wi.currentIndex = wi.steps.size(); // point past last
                req.status = RequestStatus.APPROVED;
            } else {
                // move to next pending step
                wi.currentIndex = nextPending;
                req.status = RequestStatus.IN_REVIEW;
            }

            requestRepo.save(req);
            return req;
        }
    }


    public List<Request> tasksForApprover(String approverId) {
        var user = userRepo.findById(approverId).orElseThrow();
        var tenant = user.tenantId();
        var results = new ArrayList<Request>();

        for (var r : requestRepo.findByTenant(tenant)) {
            if (r.workflowInstance == null || r.status != RequestStatus.IN_REVIEW) continue;
            var idx = r.workflowInstance.currentIndex;
            if (idx < 0 || idx >= r.workflowInstance.steps.size()) continue;
            var si = r.workflowInstance.steps.get(idx);
            if (user.roles().contains(si.role)) {
                results.add(r);
            }
        }
        return results;
    }
}
