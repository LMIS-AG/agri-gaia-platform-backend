package de.agrigaia.platform.model.coopspace

// TODO: These have to be nullable.
data class AddMemberRequest(val coopSpaceId: Long, val member: List<Member> = ArrayList())
