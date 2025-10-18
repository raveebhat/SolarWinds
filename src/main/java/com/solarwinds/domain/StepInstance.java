package com.solarwinds.domain;

import java.time.Instant;

public class StepInstance {
    public final String stepId;
    public final String stepName;
    public final Role role;
    public StepStatus status;
    public String approverId;
    public String comment;
    public Instant actedAt;

    public StepInstance(String stepId, String stepName, Role role) {
        this.stepId = stepId;
        this.stepName = stepName;
        this.role = role;
    }

    public void mark(Action action, String approverId, String comment) {
        this.approverId = approverId;
        this.comment = comment;
        this.actedAt = Instant.now();
        this.status = action == Action.APPROVE ? StepStatus.APPROVED : StepStatus.REJECTED;
    }
}
