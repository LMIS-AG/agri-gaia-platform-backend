package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseDto
import de.agrigaia.platform.model.coopspace.Member

data class AddMembersDto(
    val coopSpaceName: String?,
    val memberList: List<Member>? = ArrayList(),
) : BaseDto()
