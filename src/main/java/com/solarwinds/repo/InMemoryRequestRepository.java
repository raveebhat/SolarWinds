package com.solarwinds.repo;

import com.solarwinds.domain.Request;

import java.util.*;

public class InMemoryRequestRepository implements RequestRepository{

    private final Map<String, Request> map = new HashMap<>();

    @Override
    public void save(Request request) {
        map.put(request.id, request);
    }

    @Override
    public Optional<Request> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public List<Request> findByTenant(String tenantId) {
        var out = new ArrayList<Request>();
        for (var r : map.values()) {
            if (r.tenantId.equals(tenantId)) {
                out.add(r);
            }
        }
        return out;
    }
}
