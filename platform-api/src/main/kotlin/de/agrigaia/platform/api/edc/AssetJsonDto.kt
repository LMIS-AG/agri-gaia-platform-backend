package de.agrigaia.platform.api.edc

import de.agrigaia.platform.api.BaseDto

data class AssetJsonDto(
    val assetPropName: String?,
    val assetPropByteSize: Long?,  // Approximately 8 million TB.
    val assetPropDescription: String?,
    val assetPropContentType: String?,
    val assetPropVersion: String?,
    val assetPropId: String?,
    val agrovocKeywords: List<String>?,
    val latitude: String?,
    val longitude: String?,
    val dateRange: String?,
    val openApiDescription: String?,
    val dataAddressType: String?,
    val dataAddressRegion: String?,
    val dataAddressBucketName: String?,
    val dataAddressAssetName: String?,
    val dataAddressKeyName: String?,
) : BaseDto()
