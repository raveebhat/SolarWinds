package com.solarwinds.repo;

import com.solarwinds.domain.WorkflowTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Repository API for workflow templates with versioning support.
 * Implementations should store templates keyed by (templateId, version).
 */
public interface TemplateRepository {
    void save(WorkflowTemplate template);
    Optional<WorkflowTemplate> findByIdAndVersion(String id, int version);
    Optional<WorkflowTemplate> findLatestById(String id);
    List<WorkflowTemplate> findAllVersions(String id);
}
