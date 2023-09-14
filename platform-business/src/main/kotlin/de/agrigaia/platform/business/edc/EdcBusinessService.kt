package de.agrigaia.platform.business.edc

import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.fuseki.FusekiConnectorService
import de.agrigaia.platform.model.edc.ConstraintDto
import de.agrigaia.platform.model.edc.PolicyDto
import org.springframework.stereotype.Service

/**
 * Service for EDC related actions not communicating with EDC directly.
 */
@Service
class EdcBusinessService(
    private val fusekiConnectorService: FusekiConnectorService,
) : HasLogger {

    fun createPolicyJson(
        policyDto: PolicyDto
    ): String {
        val permissions: List<ConstraintDto> = policyDto.permissions ?: throw BusinessException(
            "PolicyDto permissions must not be null.",
            ErrorType.BAD_REQUEST,
        )
        val permissionJsons: String = if (permissions.isEmpty()) {
            ""
        } else {
            permissions.joinToString(",\n") { constraintDtoToJson(it) }
        }
        return """
            {
                "uid": "use-eu",
                "id": "${policyDto.name}-<UUID>",
                "policy": {
                    "permissions": [
                        {
                            "edctype": "dataspaceconnector:permission",
                            "uid": null,
                            "target": "<TARGET>",
                            "action": {
                                "type": "USE",
                                "includedIn": null,
                                "constraint": null
                            },
                            "assignee": null,
                            "assigner": null,
                            "constraints": [
                            $permissionJsons
                            ],
                            "duties": []
                        }
                    ],
                    "prohibitions": [],
                    "obligations": [],
                    "extensibleProperties": {},
                    "inheritsFrom": null,
                    "assigner": null,
                    "assignee": null,
                    "target": "",
                    "type": {
                        "type": "set"
                    }
                }
            }
        """.trimIndent()
    }

    private fun constraintDtoToJson(constraintDto: ConstraintDto): String {
        val leftExpression: String = constraintDto.leftExpression ?: throw BusinessException(
            "ConstraintDto leftExpression must not be null.",
            ErrorType.BAD_REQUEST,
        )
        val operator: String = constraintDto.operator ?: throw BusinessException(
            "ConstraintDto operator must not be null.",
            ErrorType.BAD_REQUEST,
        )
        val rightExpression: String = constraintDto.rightExpression ?: throw BusinessException(
            "ConstraintDto rightExpression must not be null.",
            ErrorType.BAD_REQUEST,
        )
        return """
            {
              "edctype": "AtomicConstraint",
              "leftExpression": {
                "edctype": "dataspaceconnector:literalexpression",
                "value": "$leftExpression"
              },
              "rightExpression": {
                "edctype": "dataspaceconnector:literalexpression",
                "value": "$rightExpression"
              },
              "operator": "$operator"
            }
        """.trimIndent()
    }

    fun createAssetJson(
        assetPropName: String,
        assetPropId: String,
        bucketName: String,
        assetName: String,
        assetPropDescription: String?,
        assetPropContentType: String?,
        assetPropVersion: String?,
        agrovocKeywords: List<String>?,
        latitude: String?,
        longitude: String?,
        dateRange: String?,
        openApiDescription: String?,
        dataAddressKeyName: String?,
    ): String {
        val spatial = if (latitude.isNullOrEmpty() || longitude.isNullOrEmpty()) {
            "null"
        } else {
            "\"${this.fusekiConnectorService.getUriFromCoordinates(latitude, longitude)}\""
        }
        return """
            {
              "asset": {
                "properties": {
                  "asset:prop:name": "$assetPropName",
                  "asset:prop:byteSize": null,
                  "asset:prop:description": "${assetPropDescription ?: ""}",
                  "asset:prop:contenttype": "${assetPropContentType ?: ""}",
                  "asset:prop:version": "${assetPropVersion ?: ""}",
                  "asset:prop:id": "$assetPropId",
                  "theme": ${agrovocKeywords?.map { w -> "\"${this.fusekiConnectorService.getConceptUriFromKeyword(w)}\"" }},
                  "spatial": ${spatial},
                  "temporal": "${dateRange ?: ""}"${if (openApiDescription.isNullOrEmpty()) "" else ",\n                  \"openApiDescription\": \"$openApiDescription\""}
                },
                "id": "$assetPropId"
              },
              "dataAddress": {
                "properties": {
                  "type": "AmazonS3",
                  "region": "us-east-1",
                  "bucketName": "$bucketName",
                  "assetName": "$assetName",
                  "keyName": "${dataAddressKeyName ?: ""}"
                }
              }
            }"""
    }

    fun createContractDefinitionJson(assetId: String, accessPolicyUUID: String, contractPolicyUUID: String, catalogUUID: String): String =
        """
        {
          "accessPolicyId": "$accessPolicyUUID",
          "contractPolicyId": "$contractPolicyUUID",
          "id": "$catalogUUID",
          "criteria": [
            {
              "operandLeft": "asset:prop:id",
              "operator": "=",
              "operandRight": "$assetId"
            }
          ]
        }
        """

    /**
     * Extract UUID from policy JSON.
     */
    fun extractIdfromPolicy(policyJson: String): String {
        val idLine: String = policyJson.lines().firstOrNull { it.contains("\"id\": ") }
            ?: throw BusinessException("Could not extract id from policy.", ErrorType.UNKNOWN)
        return idLine.substringBeforeLast("\"").substringAfterLast("\"")
    }
}
