package com.solarwinds.domain;

import java.util.List;

public class WorkFlowInstance {
    public final String templateId;
    public final int templateVersion;
    public final List<StepInstance> steps;
    public int currentIndex = 0; // index of step in progress/pending

    public WorkFlowInstance(String templateId, int templateVersion, List<StepInstance> steps) {
        this.templateId = templateId;
        this.templateVersion = templateVersion;
        this.steps = steps;
    }
}
