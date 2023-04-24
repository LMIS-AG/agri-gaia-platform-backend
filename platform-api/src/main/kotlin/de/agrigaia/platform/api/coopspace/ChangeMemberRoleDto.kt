package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseDto
import de.agrigaia.platform.model.coopspace.CoopSpaceRole

data class ChangeMemberRoleDto(
    val username: String?,
    val id: Long?,
    val oldRole: String?,
    val newRole: CoopSpaceRole?,
    val coopSpaceName: String?,
    val company: String?,
) : BaseDto()
