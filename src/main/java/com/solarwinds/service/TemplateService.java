package com.solarwinds.service;

import com.solarwinds.domain.StepDef;
import com.solarwinds.domain.WorkflowTemplate;
import com.solarwinds.repo.TemplateRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Minimal helper to publish a new version of a workflow template.
 * - Copies latest version (if present), increments version, applies mutator, and saves.
 * - Keeps logic intentionally small for demo/POC.
 */
public class TemplateService {
    private final TemplateRepository tplRepo;

    public TemplateService(TemplateRepository tplRepo) {
        this.tplRepo = tplRepo;
    }

    /**
     * Publish a new version of the template with id = templateId.
     * The provided mutator can add/remove/modify steps before saving.
     *
     * Returns the new saved WorkflowTemplate.
     */
    public WorkflowTemplate publishNewVersion(String templateId, UnaryOperator<WorkflowTemplate> mutator) {
        var latestOpt = tplRepo.findLatestById(templateId);
        int nextVersion = latestOpt.map(t -> t.version + 1).orElse(1);

        WorkflowTemplate newTpl = new WorkflowTemplate();
        newTpl.id = templateId;
        newTpl.version = nextVersion;
        newTpl.name = latestOpt.map(t -> t.name + " (v" + nextVersion + ")").orElse("Template " + templateId);

        // deep copy steps from latest (if present) to avoid mutating previous versions
        if (latestOpt.isPresent()) {
            var latest = latestOpt.get();
            var copied = new ArrayList<StepDef>();
            if (latest.steps != null) copied.addAll(latest.steps);
            newTpl.steps = copied;
        } else {
            newTpl.steps = new ArrayList<>();
        }

        // allow caller to modify the template before saving
        newTpl = mutator.apply(newTpl);

        // save new version
        tplRepo.save(newTpl);
        return newTpl;
    }
}
