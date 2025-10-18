package com.solarwinds.service;

import com.solarwinds.domain.*;
import com.solarwinds.repo.RequestRepository;
import com.solarwinds.repo.TemplateRepository;
import com.solarwinds.repo.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApprovalService {

    private final UserRepository users;
    private final TemplateRepository templates;
    private final RequestRepository requests;

    public ApprovalService(UserRepository users, TemplateRepository templates, RequestRepository requests) {
        this.users = users;
        this.templates = templates;
        this.requests = requests;
    }

    public Request createRequest(String tenantId, String requesterId, String category, Map<String, String> metadata) {
        var id = UUID.randomUUID().toString();
        var request = new Request(id, tenantId, requesterId, category, metadata);
        requests.save(request);
        return request;
    }

    // Submit with validation (ensures every step has at least one approver)
    public Request submitRequest(String requestId, String templateId) {
        var req = requests.findById(requestId).orElseThrow(() -> new IllegalArgumentException("req not found"));
        var template = templates.findLatestById(templateId).orElseThrow(() -> new IllegalArgumentException("template ot found"));

        var stepInstances = new ArrayList<StepInstance>();
        for (var sd : template.steps) {
            if (sd.condition != null && !sd.condition.isBlank()) {
                var parts = sd.condition.split("=", 2);
                if (parts.length != 2) continue;

                var val = req.metadata.get(parts[0]);
                if (val == null || !val.equals(parts[1])) continue;

                stepInstances.add(new StepInstance(sd.id, sd.name, sd.role));
            }
        }

        // preflight: ensure at least one user per role in tenant (fail fast)

        var rolesNeeded = stepInstances.stream().map(si -> si.role).collect(Collectors.groupingBy(r -> r));

        for (var role : rolesNeeded.keySet()) {
            var candidates = users.findByTenantAndRole(req.tenantId, role);
            if (candidates.isEmpty()) {
                throw new IllegalArgumentException("no approvers for role" + role + " in tenant " + req.tenantId);
            }
        }

        var wi = new WorkFlowInstance(template.id, template.version, stepInstances);
        req.workFlowInstance = wi;
        req.status = wi.steps.isEmpty() ? RequestStatus.APPROVED : RequestStatus.IN_REVIEW;
        requests.save(req);
        return req;
    }

    public Request act(String approverId, String requestId, Action action, String comment) {
        Request request = null;
        synchronized (("REQ_LOCK_" + requestId).intern()) {
            var user = users.findById(approverId).orElseThrow();
            var req = requests.findById(requestId).orElseThrow();

            if (!user.tenantId().equals(approverId)) {
                throw new SecurityException("Tenantt mismatch");
            }

            if (req.requesterId.equals(approverId)) {
                throw new SecurityException("requester can not act");
            }

            var wi = req.workFlowInstance;
            if (wi == null) {
                throw new IllegalStateException("not submitted");
            }
            if (req.status != RequestStatus.IN_REVIEW) {
                throw new IllegalStateException("not in review");
            }

            int idx = wi.currentIndex;
            if (idx < 0 || idx >= wi.steps.size()) {
                throw new IllegalStateException("no active step");
            }
            var si = wi.steps.get(idx);
            if (!user.roles().contains(si.role)) {
                throw new SecurityException("user role mismatch");
            }

            si.mark(action, approverId, comment);
            if (action == Action.REJECT) {
                req.status = RequestStatus.REJECTED;
                requests.save(req);
                request = req;
            }

            int next = idx + 1;

            while (next < wi.steps.size() && wi.steps.get(next).status != StepStatus.PENDING) {
                next++;
            }
            wi.currentIndex = next;
            if (next >= wi.steps.size()) {
                req.status = RequestStatus.APPROVED;
            }
            else {
                req.status = RequestStatus.IN_REVIEW;
                requests.save(req);
                request = req;
            }
        }
        return request;
    }

    public List<Request> tasksForApprover(String approverId) {
        var user = users.findById(approverId).orElseThrow();
        var tenant = user.tenantId();
        var results = new ArrayList<Request>();

        for (var r : requests.findByTenant(tenant)) {
            if (r.workFlowInstance == null || r.status != RequestStatus.IN_REVIEW) continue;
            var idx = r.workFlowInstance.currentIndex;
            if (idx < 0 || idx >= r.workFlowInstance.steps.size()) continue;
            var si = r.workFlowInstance.steps.get(idx);
            if (user.roles().contains(si.role)) {
                results.add(r);
            }
        }
        return results;
    }
}
