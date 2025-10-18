package com.solarwinds.repo;

import com.solarwinds.domain.Role;
import com.solarwinds.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(String id);
    List<User> findByTenantAndRole(String tenantId, Role role);
    void save(User user);
}
