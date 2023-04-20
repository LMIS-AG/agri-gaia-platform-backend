package de.agrigaia.platform.business.errors

enum class ErrorType {
    // Default 4xx
    NOT_FOUND,
    BAD_REQUEST, // 400

    UNKNOWN,
    RESOURCE_ID_MISMATCH,

    // Specific errors
    EXAMPLE,
    BUCKET_NOT_EMPTY,
}