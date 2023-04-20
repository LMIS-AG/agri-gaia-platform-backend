package de.agrigaia.platform.model.coopspace

data class AddMembersRequest(
    val coopSpaceName: String?,
    val memberList: List<Member>? = ArrayList(),
)
