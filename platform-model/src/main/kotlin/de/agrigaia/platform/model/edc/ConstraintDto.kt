package de.agrigaia.platform.model.edc

data class ConstraintDto(
    val constraintType: ConstraintType?,
    val leftExpression: String?,
    val operator: String?,
    val rightExpression: String?,
)

enum class ConstraintType {
    PERMISSION,
    OBLIGATION,
    DUTY,
}
