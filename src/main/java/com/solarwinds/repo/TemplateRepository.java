package com.solarwinds.repo;

import com.solarwinds.domain.WorkflowTemplate;

import java.util.Optional;

public interface TemplateRepository {
    void save(WorkflowTemplate template);
    Optional<WorkflowTemplate> findByIdAndVersion(String id, int version);
    Optional<WorkflowTemplate> findLatestById(String id);
}
