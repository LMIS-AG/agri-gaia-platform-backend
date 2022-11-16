package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseDto
import de.agrigaia.platform.model.coopspace.CoopSpaceRole

data class MemberDto (
    var id: Long?,
    var name: String?,
    var company: String?,
    var email: String?,
    var role: CoopSpaceRole?,
    var username: String?
) : BaseDto()