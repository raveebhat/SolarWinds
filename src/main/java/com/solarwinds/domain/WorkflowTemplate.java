package com.solarwinds.domain;

import java.util.ArrayList;
import java.util.List;

public class WorkflowTemplate {
    public String id;
    public int version;
    public String name;
    public List<StepDef> steps = new ArrayList<>();

    public WorkflowTemplate() {}
}
