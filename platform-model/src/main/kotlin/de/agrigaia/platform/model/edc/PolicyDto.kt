package de.agrigaia.platform.model.edc

// TODO: This should go into api, but I cba.
data class PolicyDto(
    val name: String,
    val policyType: PolicyType,
)

enum class PolicyType {
    CONTRACT,
    ACCESS,
}