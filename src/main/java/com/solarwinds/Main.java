package com.solarwinds;

import com.solarwinds.domain.*;
import com.solarwinds.repo.InMemoryRequestRepository;
import com.solarwinds.repo.InMemoryTemplateRepository;
import com.solarwinds.repo.InMemoryUserRepo;
import com.solarwinds.service.ApprovalService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        var userRepo = new InMemoryUserRepo();
        var templateRepo = new InMemoryTemplateRepository();
        var reqRepo = new InMemoryRequestRepository();

        // Users: tenantA and tenantB

        var mahesh = new User("u-mahesh", "Mahesh Manager", "tenantA", Set.of(Role.MANAGER));
        var suresh = new User("u-suresh", "Suresh Finance", "tenantA", Set.of(Role.FINANCE));
        var kumar = new User("u-kumar", "Kumar Legal", "tenantA", Set.of(Role.LEGAL));
        var aditya = new User("u-aditya", "Aditya Manager", "tenantB", Set.of(Role.MANAGER));

        userRepo.save(mahesh);
        userRepo.save(suresh);
        userRepo.save(kumar);
        userRepo.save(aditya);

        var requester = new User("u-requester", "Renter", "tenantA", Set.of(Role.REQUESTER));
        userRepo.save(requester);


        var t = new WorkflowTemplate();
        t.id = "tmpl-expense-basic";
        t.version = 1;
        t.name = "Basic Expense Approval";

        var s1 = new StepDef();
        s1.id = "s1";
        s1.name = "Manager Review";
        s1.role = Role.MANAGER;

        var s2 = new StepDef();
        s2.id = "s2";
        s2.name = "Finance Review";
        s2.role = Role.FINANCE;

        var s3 = new StepDef();
        s3.id = "s3";
        s3.name = "Legal Review";
        s3.role = Role.LEGAL;
        s3.condition = "requiresLegal=yes";

        t.steps = List.of(s1, s2, s3);
        templateRepo.save(t);

        var service = new ApprovalService(userRepo, templateRepo, reqRepo);

        // Scenario 1: Submit and fully approve (legal skipped)
        var meta1 = new HashMap<String, String>();
        meta1.put("amount", "1500");
        meta1.put("requiresLegal", "no");

        var req1 = service.createRequest("tenantA", "u-requester", "EXPENSE", meta1);
        System.out.println("Created request id=" + req1.id);
        service.submitRequest(req1.id, "tmpl-expense-basic");
        System.out.println("After submit, status=" + service.act("u-mahesh", req1.id, Action.APPROVE, "Manager OK").status);
        System.out.println("After Finance approve, status=" + service.act("u-suresh", req1.id, Action.APPROVE, "Finance OK").status);

        // Scenario 2: Rejection at step 2
        var meta2 = new HashMap<String, String>();
        meta2.put("amount", "5000");
        meta2.put("requiresLegal", "yes");

        var req2 = service.createRequest("tenantA", "u-requester", "EXPENSE", meta2);
        System.out.println("Created request id=" + req2.id);
        service.submitRequest(req2.id, "tmpl-expense-basic");
        System.out.println("After submit, status=" + service.act("u-mahesh", req2.id, Action.APPROVE, "Manager OK").status);
        System.out.println("After Finance reject, status=" + service.act("u-suresh", req2.id, Action.REJECT, "Insufficient docs").status);

        // Scenario 3: Tenant Isolation
        var tasksAditya = service.tasksForApprover("u-aditya");
        System.out.println("Aditya tasks(tenantB) should be 0: " + tasksAditya.size());

        // Scenario 4: Preflight missing approver chheck
        // Create a template with a role that has no users in tenantA
        var tpl2 = new WorkflowTemplate();
        t.id = "tmpl-missing";
        t.version = 1;
        t.name = "Missing Approver Demo";

        var sx = new StepDef();
        sx.id = "sx";
        sx.name = "Auditor Check";
        sx.role = Role.AUDITOR;
        tpl2.steps = List.of(sx);
        templateRepo.save(tpl2);

        var req4 = service.createRequest("tenantA", "u-requester", "EXPENSE", Map.of());

        try {
            service.submitRequest(req4.id, "tmpl-missing");
            System.out.println("ERROR: should have failed preflight for missing approver");
        }
        catch (Exception exception) {
            System.out.println("Expected preflight failure: " + exception.getMessage());
        }

        System.out.println("Demo complete");
    }
}