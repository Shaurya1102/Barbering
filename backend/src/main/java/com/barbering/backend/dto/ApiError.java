package com.Barbering.backend.dto;

/**
 * Consistent JSON error shape for all API failures: {"error": "..."} with an
 * optional "details" array for field-level validation messages.
 */
public record ApiError(String error, java.util.List<String> details) {

    public ApiError(String error) {
        this(error, null);
    }
}
