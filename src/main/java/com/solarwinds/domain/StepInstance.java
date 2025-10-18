package com.solarwinds.domain;

import java.time.Instant;

public class StepInstance {
    public final String stepId;
    public final String stepName;
    public final Role role;
    public StepStatus status;      // will be initialized to PENDING below
    public String approverId;     // who acted
    public String comment;
    public Instant actedAt;

    // Constructor - ensure status defaults to PENDING
    public StepInstance(String stepId, String stepName, Role role) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.role = role;
        this.status = StepStatus.PENDING; // <-- important fix
    }

    // mark the step with APPROVE or REJECT action
    public void mark(Action action, String approverId, String comment) {
        this.approverId = approverId;
        this.comment = comment;
        this.actedAt = Instant.now();
        this.status = action == Action.APPROVE ? StepStatus.APPROVED : StepStatus.REJECTED;
    }

    // optional helper if you need to mark skipped at submission time
    public void markSkipped() {
        this.status = StepStatus.SKIPPED;
        this.actedAt = Instant.now();
    }
}
