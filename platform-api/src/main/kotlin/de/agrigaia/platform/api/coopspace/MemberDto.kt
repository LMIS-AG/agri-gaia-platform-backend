package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseDto

data class MemberDto (
    var id: Long?,
    var name: String?,
    var company: String?
) : BaseDto()