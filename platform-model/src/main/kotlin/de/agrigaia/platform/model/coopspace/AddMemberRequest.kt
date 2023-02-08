package de.agrigaia.platform.model.coopspace

data class AddMemberRequest(val coopSpaceId: Long, val coopSpaceName: String, val member: Member)
