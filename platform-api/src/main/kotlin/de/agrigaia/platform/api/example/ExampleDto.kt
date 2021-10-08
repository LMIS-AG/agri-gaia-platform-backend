package de.agrigaia.platform.api.example

import de.agrigaia.platform.api.BaseDto

data class ExampleDto(
    var id: Long?,
    var lineOne: String?,
    var lineTwo: String?,
) : BaseDto()