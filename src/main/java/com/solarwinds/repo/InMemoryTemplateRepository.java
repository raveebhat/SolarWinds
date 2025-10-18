package com.solarwinds.repo;

import com.solarwinds.domain.WorkflowTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

public class InMemoryTemplateRepository implements TemplateRepository {

    private final Map<String, TreeMap<Integer, WorkflowTemplate>> store = new HashMap<>();

    @Override
    public void save(WorkflowTemplate template) {
        store.computeIfAbsent(template.id, k -> new TreeMap<>()).put(template.version, template);
    }

    @Override
    public Optional<WorkflowTemplate> findByIdAndVersion(String id, int version) {
        return Optional.ofNullable(store.getOrDefault(id, new TreeMap<>()).get(version));
    }

    @Override
    public Optional<WorkflowTemplate> findLatestById(String id) {
        var map = store.get(id);
        if (map == null || map.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(map.lastEntry().getValue());
    }
}
