package com.solarwinds.domain;

import java.util.Set;

public record User(String id, String name, String tenantId, Set<Role> roles) { }
