package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseDto
import de.agrigaia.platform.model.coopspace.Member

data class DeleteMemberDto(
    val coopSpaceName: String?,
    val member: Member?,
) : BaseDto()
