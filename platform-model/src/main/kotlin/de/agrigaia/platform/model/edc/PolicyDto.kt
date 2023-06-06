package de.agrigaia.platform.model.edc

import com.fasterxml.jackson.databind.JsonNode

// TODO: This should go into api, but I cba.
data class PolicyDto(
    val name: String?,
    val policyType: PolicyType?,
    val rawJson: JsonNode?,
)

enum class PolicyType {
    CONTRACT,
    ACCESS,
}