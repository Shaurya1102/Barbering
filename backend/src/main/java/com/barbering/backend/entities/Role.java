package com.Barbering.backend.entities;

/**
 * Application roles. CUSTOMER and BARBER are the current product roles;
 * ADMIN can be added here later without schema changes (the column is a
 * VARCHAR with a CHECK constraint that would then be extended by migration).
 */
public enum Role {
    CUSTOMER,
    BARBER
}
