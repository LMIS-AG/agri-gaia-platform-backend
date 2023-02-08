package de.agrigaia.platform.model.coopspace

data class ModifyCoopSpaceRequest(val coopSpaceId: Long, val memberId: Long, val username: String, val role: String, val coopSpaceName: String, val companyName: String)
