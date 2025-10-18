package com.solarwinds;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solarwinds.domain.*;
import com.solarwinds.repo.*;
import com.solarwinds.service.ApprovalService;
import com.solarwinds.service.TemplateService;

import java.io.InputStream;
import java.util.*;

/**
 * Compact demo covering:
 *  - create/submit requests (metadata)
 *  - approve/reject flows and skipped conditional steps
 *  - security: tenant isolation & requester can't approve self
 *  - preflight validation (missing approver)
 *  - versioned templates: in-flight instances continue on old version
 */
public class Main {
    public static void main(String[] args) throws Exception {
        var userRepo = new InMemoryUserRepository();
        var templateRepo = new InMemoryTemplateRepository();
        var reqRepo = new InMemoryRequestRepository();
        var service = new ApprovalService(userRepo, templateRepo, reqRepo);
        var tplService = new TemplateService(templateRepo);

        loadJson(userRepo, templateRepo);

        // Scenario 1: Normal flow (Legal skipped)
        System.out.println("\n--- Scenario 1: Normal approval flow (Legal skipped) ---");
        var req1 = createAndSubmit(service, "tenantA", "u-requester", Map.of("amount", "1200", "requiresLegal", "no"));
        service.act("u-mahesh", req1.id, Action.APPROVE, "Manager OK");
        service.act("u-suresh", req1.id, Action.APPROVE, "Finance OK");
        printSimpleStatus(req1, reqRepo);

        // Scenario 2: Rejection
        System.out.println("\n--- Scenario 2: Rejection at Finance ---");
        var req2 = createAndSubmit(service, "tenantA", "u-requester", Map.of("amount", "3000", "requiresLegal", "yes"));
        service.act("u-mahesh", req2.id, Action.APPROVE, "Manager OK");
        service.act("u-suresh", req2.id, Action.REJECT, "Docs missing");
        printSimpleStatus(req2, reqRepo);

        // Scenario 3: Security checks (requester can't approve; tenant isolation)
        System.out.println("\n--- Scenario 3: Security checks ---");
        try {
            service.act("u-requester", req1.id, Action.APPROVE, "Self-approve");
        } catch (Exception e) {
            System.out.println("Expected: " + e.getMessage());
        }
        System.out.println("Tenant isolation: tasks for u-aditya = " + service.tasksForApprover("u-aditya").size() + " (expected 0)");

        // Scenario 4: Preflight missing approver
        System.out.println("\n--- Scenario 4: Preflight validation ---");
        var req3 = service.createRequest("tenantA", "u-requester", "EXPENSE", Map.of());
        try {
            service.submitRequest(req3.id, "tmpl-missing");
            System.out.println("ERROR: submit succeeded but should have failed preflight");
        } catch (Exception e) {
            System.out.println("Expected preflight failure: " + e.getMessage());
        }

        // Scenario 5: Versioning — in-flight uses old template version
        System.out.println("\n--- Scenario 5: Template versioning (in-flight keeps old version) ---");
        var metaV = Map.of("amount", "1000", "requiresLegal", "no");
        var reqV = service.createRequest("tenantA", "u-requester", "EXPENSE", new HashMap<>(metaV));
        service.submitRequest(reqV.id, "tmpl-expense-basic");
        System.out.println("Submitted req " + shortId(reqV.id) + " using template v" + reqV.workflowInstance.templateVersion);

        // publish new version (add Auditor step)
        tplService.publishNewVersion("tmpl-expense-basic", tpl -> {
            var extra = new StepDef();
            extra.id = "s-auditor"; extra.name = "Auditor Check"; extra.role = Role.AUDITOR;
            tpl.steps.add(extra);
            return tpl;
        });

        var latest = templateRepo.findLatestById("tmpl-expense-basic").orElseThrow();
        System.out.println("Published template latest v" + latest.version + " (steps=" + latest.steps.size() + ")");

        // show in-flight still uses old version
        var loadedV = reqRepo.findById(reqV.id).orElseThrow();
        System.out.println("In-flight req uses template v" + loadedV.workflowInstance.templateVersion
                + " (steps=" + loadedV.workflowInstance.steps.size() + ")");

        // finish in-flight using original steps
        service.act("u-mahesh", reqV.id, Action.APPROVE, "Manager OK");
        service.act("u-suresh", reqV.id, Action.APPROVE, "Finance OK");
        System.out.println("Final status (in-flight) = " + reqRepo.findById(reqV.id).orElseThrow().status);

        System.out.println("\nDemo complete.");
    }

    // minimal helpers
    private static Request createAndSubmit(ApprovalService svc, String tenant, String requester, Map<String, String> meta) {
        var r = svc.createRequest(tenant, requester, "EXPENSE", new HashMap<>(meta));
        svc.submitRequest(r.id, "tmpl-expense-basic");
        System.out.println("Created: " + shortId(r.id) + " status=" + r.status);
        return r;
    }

    private static void printSimpleStatus(Request req, InMemoryRequestRepository repo) {
        var r = repo.findById(req.id).orElseThrow();
        System.out.println("Request " + shortId(r.id) + " -> " + r.status);
        if (r.workflowInstance != null) {
            r.workflowInstance.steps.forEach(s ->
                    System.out.println("  " + s.stepName + " -> " + s.status + " (" + s.role + ")"));
        }
    }

    private static void loadJson(InMemoryUserRepository userRepo, InMemoryTemplateRepository templateRepo) throws Exception {
        var mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("users.json")) {
            var users = mapper.readValue(is, new TypeReference<List<User>>() {});
            users.forEach(userRepo::save);
        }
        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("templates.json")) {
            var templates = mapper.readValue(is, new TypeReference<List<WorkflowTemplate>>() {});
            templates.forEach(templateRepo::save);
        }
    }

    private static String shortId(String id) {
        return id == null ? "n/a" : id.split("-")[0];
    }
}
