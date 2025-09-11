package com.cityfix.api.domain;

/**
 * Stored as lowercase strings to match the DB CHECK constraint
 * ('citizen','staff','contractor','admin').
 */
public enum Role {
    citizen, staff, contractor, admin;

    public String asAuthority() {
        return "ROLE_" + name().toUpperCase();
    }
}