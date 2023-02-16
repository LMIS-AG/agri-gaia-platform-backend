package de.agrigaia.platform.model.coopspace

data class AddMemberRequest(val coopSpaceId: Long, val member: List<Member> = ArrayList())
