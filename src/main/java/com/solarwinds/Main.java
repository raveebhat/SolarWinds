package com.solarwinds;

import com.solarwinds.domain.*;
import com.solarwinds.repo.InMemoryRequestRepository;
import com.solarwinds.repo.InMemoryTemplateRepository;
import com.solarwinds.repo.InMemoryUserRepo;
import com.solarwinds.service.ApprovalService;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
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

        String json = ""; // Read from file

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

        var service = new ApprovalService(userRepo, templateRepo, reqRepo);

        var req = service.createRequest("tenantA", "u-requester", "EXPENSE", Map.of("requestLegal", "yes"));
        service.submitRequest(req.id, "tmpl-expense-basic");

        System.out.println("Req created/in-review? status =" + service.act("u-mahesh", req.id, Action.APPROVE, "ok").status);

    }
}