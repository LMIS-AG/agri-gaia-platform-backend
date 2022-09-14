package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseDto

data class CoopSpaceDto(
    var id: Long?,
    var name: String?,
    var company: String?,
    var members: List<MemberDto>?
) : BaseDto()