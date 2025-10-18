package com.solarwinds.repo;

import com.solarwinds.domain.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository {
    void save(Request request);
    Optional<Request> findById(String id);
    List<Request> findByTenant(String tenantId);
}
