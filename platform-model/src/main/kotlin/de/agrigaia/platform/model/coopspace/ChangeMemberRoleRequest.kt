package de.agrigaia.platform.model.coopspace

// TODO: These have to be nullable.
data class ChangeMemberRoleRequest(val coopSpaceId: Long, val originalRole: String, val member: Member)
