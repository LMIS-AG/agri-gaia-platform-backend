package de.agrigaia.platform.model.edc

data class PolicyDto(
    val name: String?,
    val policyType: PolicyType?,
    val permissions: List<ConstraintDto>?,
//    val rawJson: JsonNode?,
)

enum class PolicyType {
    CONTRACT,
    ACCESS,
}