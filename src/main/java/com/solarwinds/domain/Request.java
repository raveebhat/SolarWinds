package com.solarwinds.domain;

import java.time.Instant;
import java.util.Map;

public class Request {
    public final String id;
    public final String tenantId;
    public final String requesterId;
    public final String category;
    public final Map<String, String> metadata; // amount, invoiceId etc
    public RequestStatus status;
    public WorkflowInstance workflowInstance;
    public final Instant createdAt;

    public Request(String id, String tenantId, String requesterId, String category, Map<String, String> metadata) {
        this.id = id;
        this.tenantId = tenantId;
        this.requesterId = requesterId;
        this.category = category;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }
}
