package com.solarwinds.repo;

import com.solarwinds.domain.WorkflowTemplate;

import java.util.*;

/**
 * In-memory versioned template repository.
 *
 * Note: not thread-safe (fine for demo). In production use DB with unique (templateId, version)
 * and atomic increments to assign versions.
 */
public class InMemoryTemplateRepository implements TemplateRepository {

    private final Map<String, TreeMap<Integer, WorkflowTemplate>> store = new HashMap<>();

    @Override
    public void save(WorkflowTemplate template) {
        if (template == null || template.id == null) throw new IllegalArgumentException("template or id null");
        store.computeIfAbsent(template.id, k -> new TreeMap<>()).put(template.version, template);
    }

    @Override
    public Optional<WorkflowTemplate> findByIdAndVersion(String id, int version) {
        if (id == null) return Optional.empty();
        var map = store.get(id);
        if (map == null) return Optional.empty();
        return Optional.ofNullable(map.get(version));
    }

    @Override
    public Optional<WorkflowTemplate> findLatestById(String id) {
        if (id == null) return Optional.empty();
        var map = store.get(id);
        if (map == null || map.isEmpty()) return Optional.empty();
        return Optional.of(map.lastEntry().getValue());
    }

    @Override
    public List<WorkflowTemplate> findAllVersions(String id) {
        var map = store.get(id);
        if (map == null || map.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(map.values());
    }
}
