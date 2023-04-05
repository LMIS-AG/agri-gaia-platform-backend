package de.agrigaia.platform.model.coopspace

// TODO: These have to be nullable.
data class DeleteMemberRequest(val coopSpaceName: String, val member: Member)
