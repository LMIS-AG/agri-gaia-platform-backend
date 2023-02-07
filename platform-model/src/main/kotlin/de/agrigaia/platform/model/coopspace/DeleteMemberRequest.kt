package de.agrigaia.platform.model.coopspace

data class DeleteMemberRequest(val memberId: Long, val username: String, val role: String, val coopSpaceName: String, val companyName: String)
