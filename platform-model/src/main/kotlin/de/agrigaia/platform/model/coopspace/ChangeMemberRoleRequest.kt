package de.agrigaia.platform.model.coopspace

data class ChangeMemberRoleRequest(val coopSpaceId: Long?, val originalRole: String?, val member: Member?)
