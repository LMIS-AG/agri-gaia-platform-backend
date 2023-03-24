package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseDto

data class AssetDto(
    val id: String,
    val name: String,
    val date: String,
    val uploadDate: String,
    val size: String,
    val labeling: String,
    val bucket: String,
    val isPublished: Boolean
) : BaseDto()
