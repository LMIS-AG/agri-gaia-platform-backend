package de.agrigaia.platform.business.edc

import de.agrigaia.platform.integration.fuseki.FusekiConnectorService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EdcServiceTest {

    @Test
    fun `Test createAssetJson`() {
        val fusekiConnectorService: FusekiConnectorService = mockk()
        val edcService = EdcService(fusekiConnectorService)
        every { fusekiConnectorService.getConceptUriFromKeyword(any()) } returns "someAgrovocUri"

        val assetPropName = "someName"
        val assetPropId = "someId"
        val bucketName = "someBucketName"
        val assetName = "someAssetName"
        val assetPropDescription = "someAssetPropDescription"
        val assetPropContentType = "someAssetPropContentType"
        val assetPropVersion = "someAssetPropVersion"
        val agrovocKeywords: List<String> = listOf("a", "b")
        val latitude = "someLatitude"
        val longitude = "someLongitude"
        val dateRange = "someDateRange"
        val dataAddressKeyName = "someDataAddressKeyName"

        val expected = """
        {
          "asset": {
            "properties": {
              "asset:prop:name": "someName",
              "asset:prop:byteSize": null,
              "asset:prop:description": "someAssetPropDescription",
              "asset:prop:contenttype": "someAssetPropContentType",
              "asset:prop:version": "someAssetPropVersion",
              "asset:prop:id": "someId",
              "theme": ["someAgrovocUri", "someAgrovocUri"],
              "spatial": "someGeoNamesUri",
              "temporal": "someDateRange"
            },
            "id": "someId"
          },
          "dataAddress": {
            "properties": {
              "type": "AmazonS3",
              "region": "us-east-1",
              "bucketName": "someBucketName",
              "assetName": "someAssetName",
              "keyName": "someDataAddressKeyName"
            }
          }
        }"""
        val actual = edcService.createAssetJson(
            assetPropName,
            assetPropId,
            bucketName,
            assetName,
            assetPropDescription,
            assetPropContentType,
            assetPropVersion,
            agrovocKeywords,
            latitude,
            longitude,
            dateRange,
            dataAddressKeyName,
        )
        assertEquals(expected, actual)
    }
}
