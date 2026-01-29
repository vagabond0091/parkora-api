package com.parkora.api.enums;

import lombok.Getter;

/**
 * User roles stored in the database. Spring Security authorities are "ROLE_" + name.
 */
@Getter
public enum UserRole {

    ADMIN("Administrator"),
    CUSTOMER("Customer"),
    MODERATOR("Moderator");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    /** Role name as stored in the database (for Role.name and findByName). */
    public String getAuthorityName() {
        return name();
    }
}
