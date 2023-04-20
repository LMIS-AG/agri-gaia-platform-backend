package de.agrigaia.platform.model.coopspace

data class ChangeMemberRoleRequest(
    val coopSpaceName: String?,
    val originalRole: String?,
    val member: Member?,
)
