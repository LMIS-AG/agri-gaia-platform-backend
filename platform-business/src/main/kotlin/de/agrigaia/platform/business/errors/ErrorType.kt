package de.agrigaia.platform.business.errors

enum class ErrorType {
    // Default 4xx
    NOT_FOUND,

    UNKNOWN,
    RESOURCE_ID_MISMATCH,

    // Specific errors
    EXAMPLE,
}