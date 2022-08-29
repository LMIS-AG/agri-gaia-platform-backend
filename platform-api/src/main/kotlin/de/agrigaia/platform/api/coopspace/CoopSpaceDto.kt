package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseDto

data class CoopSpaceDto(
    var id: Long?,
    var lineOne: String?,
    var lineTwo: String?,
) : BaseDto()