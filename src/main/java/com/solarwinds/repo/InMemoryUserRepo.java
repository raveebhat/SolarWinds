package com.solarwinds.repo;

import com.solarwinds.domain.Role;
import com.solarwinds.domain.User;

import java.util.*;

public class InMemoryUserRepo implements UserRepository{

    private final Map<String, User> users = new HashMap<>();

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id));
    }


    @Override
    public List<User> findByTenantAndRole(String tenantId, Role role) {
        var out = new ArrayList<User>();
        for (var u : users.values()) {
            if (u.tenantId().equals(tenantId) && u.roles().contains(role)) {
                out.add(u);
            }
        }
        return out;
    }

    @Override
    public void save(User user) {
        users.put(user.id(), user);
    }
}
