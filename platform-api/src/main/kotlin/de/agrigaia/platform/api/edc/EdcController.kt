package de.agrigaia.platform.api.edc

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.business.edc.EdcBusinessService
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.edc.EdcIntegrationService
import de.agrigaia.platform.model.edc.Asset
import de.agrigaia.platform.model.edc.PolicyDto
import de.agrigaia.platform.model.edc.PolicyType
import de.agrigaia.platform.persistence.repository.AssetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/edc")
class EdcController @Autowired constructor(
    private val edcBusinessService: EdcBusinessService,
    private val edcIntegrationService: EdcIntegrationService,
    private val assetRepository: AssetRepository,
) : HasLogger, BaseController() {

    /**
     * Get an assetjson from the user's MinIO bucket.
     *
     * @param assetName name of the policy
     * @return TODO
     */
    @GetMapping("assetjsons/{assetName}")
    fun getAssetjson(@PathVariable assetName: String) {
        TODO("Not yet implemented")
    }

    /**
     * Add an assetjson to the user's MinioBucket.
     *
     * @param assetName name of the asset
     * @return TODO
     */
    @PostMapping("assetjsons/{assetName}")
    fun addAssetjson(@PathVariable assetName: String, @RequestBody assetJsonDto: AssetJsonDto) {
        val assetPropName: String =
            assetJsonDto.assetPropName ?: throw BusinessException("No asset name in AssetJsonDto.", ErrorType.NOT_FOUND)
        val assetPropId: String =
            assetJsonDto.assetPropId ?: throw BusinessException("No asset id in AssetJsonDto.", ErrorType.NOT_FOUND)
        val jwtTokenValue = getJwtToken().tokenValue
        val bucketName = getBucketName()

        val assetJson = edcBusinessService.createAssetJson(
            assetPropName,
            assetPropId,
            bucketName,
            assetName,
            assetJsonDto.assetPropDescription,
            assetJsonDto.assetPropContentType,
            assetJsonDto.assetPropVersion,
            assetJsonDto.agrovocKeywords,
            assetJsonDto.latitude,
            assetJsonDto.longitude,
            assetJsonDto.dateRange,
            assetJsonDto.openApiDescription,
            assetJsonDto.dataAddressKeyName
        )

        edcIntegrationService.addAssetjson(jwtTokenValue, bucketName, assetName, assetJson)
    }


    /**
     * Delete an assetjson from the user's MinIO bucket.
     *
     * @param assetName name of the policy
     * @return TODO
     */
    @DeleteMapping("assetjsons/{assetName}")
    fun deleteAssetjson(@PathVariable assetName: String) {
        TODO("Not yet implemented")
    }


    /**
     * Return a list of all policies from the user's Minio bucket.
     *
     * @return list of policies in MinIO user's bucket
     */
    @GetMapping("policies")
    fun getAllPolicies(): ResponseEntity<List<PolicyDto>> {
        val jwt = getJwtToken().tokenValue
        val bucketName = getBucketName()
        val allPolicies = edcIntegrationService.getAllPolicies(jwt, bucketName)
        return ResponseEntity.ok(allPolicies)
    }


    /**
     * Get a policy from the user's MinIO bucket.
     *
     * @param policyName name of the policy
     * @return TODO
     */
    @GetMapping("policies/{policyName}")
    fun getPolicy(@PathVariable policyName: String): ResponseEntity<String> {
        val jwt = getJwtToken().tokenValue
        val bucketName = getBucketName()
        val policy = edcIntegrationService.getPolicyJson(jwt, bucketName, policyName)
        return ResponseEntity.ok(policy)
    }

    /**
     * Add a policy to the user's MinioBucket.
     * @policyDto request body, policyDto
     */
    @PostMapping("policies")
    fun addPolicy(@RequestBody policyDto: PolicyDto) {
        val policyName: String = policyDto.name ?: throw BusinessException("name was null", ErrorType.BAD_REQUEST)
        val policyType: PolicyType =
            policyDto.policyType ?: throw BusinessException("policyType was null", ErrorType.BAD_REQUEST)
        val policyJson: String = edcBusinessService.createPolicyJson(policyDto)
        val jwtTokenValue = getJwtToken().tokenValue
        val bucketName = getBucketName()

        edcIntegrationService.addPolicy(jwtTokenValue, bucketName, policyName, policyJson, policyType)
    }


    /**
     * Delete a policy from the user's MinIO bucket.
     *
     * @param policyName name of the policy
     * @return 200, even if file did not exist (MinIO is dumb)
     */
    @DeleteMapping("policies/{policyName}")
    fun deletePolicy(@PathVariable policyName: String) {
        val jwtTokenValue = getJwtToken().tokenValue
        val bucketName = getBucketName()
        edcIntegrationService.deletePolicy(jwtTokenValue, bucketName, policyName)
    }


    /**
     * Return a list of all contractdefinitions from the user's Minio bucket.
     *
     * @return list of contractdefinitions in MinIO user's bucket
     */
    @GetMapping("contractdefinitions")
    fun getAllContractDefinitions(): ResponseEntity<List<String>> {
        TODO("Not yet implemented")
    }


    /**
     * Get a contractdefinition from the user's MinIO bucket.
     *
     * @param contractDefinitionName name of the policy
     * @return TODO
     */
    @GetMapping("contractdefinitions/{contractDefinitionName}")
    fun getContractDefinitions(@PathVariable contractDefinitionName: String) {
        TODO("Not yet implemented")
    }

    /**
     * Add a contractdefinition to the user's MinioBucket.
     *
     * @param contractDefinitionName name of the policy
     * @return TODO
     */
    @PostMapping("contractdefinitions/{contractDefinitionName}")
    fun addContractDefinitions(@PathVariable contractDefinitionName: String, @RequestBody policyJson: String) {
        TODO("Not yet implemented")
    }


    /**
     * Delete a contractdefinition from the user's MinIO bucket.
     *
     * @param contractDefinitionName name of the policy
     * @return TODO
     */
    @DeleteMapping("contractdefinitions/{contractDefinitionName}")
    fun deleteContractDefinitions(@PathVariable contractDefinitionName: String) {
        TODO("Not yet implemented")
    }

    @DeleteMapping("unpublish/{bucketName}/{assetName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unpublishAsset(@PathVariable bucketName: String, @PathVariable assetName: String) {

        val asset: Asset = assetRepository.findByBucketAndName(bucketName, assetName)
            ?: throw BusinessException("Asset was null", ErrorType.BAD_REQUEST)

        val assetId = asset.assetId
            ?: throw BusinessException("assetId was null", ErrorType.BAD_REQUEST)
        val accessPolicyId = asset.accessPolicyId
            ?: throw BusinessException("accessPolicyId was null", ErrorType.BAD_REQUEST)
        val contractPolicyId = asset.contractPolicyId
            ?: throw BusinessException("contractPolicyId was null", ErrorType.BAD_REQUEST)
        val contractId = asset.contractId
            ?: throw BusinessException("contractId was null", ErrorType.BAD_REQUEST)

        assetRepository.delete(asset)
        this.edcIntegrationService.unpublishAsset(assetId, accessPolicyId, contractPolicyId, contractId)
    }

    @PostMapping("publish/{bucketName}/{assetName}/{accessPolicyName}/{contractPolicyName}")
    @ResponseStatus(HttpStatus.CREATED)
    fun publishAsset(
        @PathVariable bucketName: String,
        @PathVariable assetName: String,
        @PathVariable accessPolicyName: String,
        @PathVariable contractPolicyName: String,
        @RequestBody assetJsonDto: AssetJsonDto,
    ) {
        val assetPropName: String =
            assetJsonDto.assetPropName ?: throw BusinessException("No asset name in AssetJsonDto.", ErrorType.NOT_FOUND)
        val assetPropId: String =
            assetJsonDto.assetPropId ?: throw BusinessException("No asset id in AssetJsonDto.", ErrorType.NOT_FOUND)

        val assetJson = edcBusinessService.createAssetJson(
            assetPropName,
            assetPropId,
            bucketName,
            assetName,
            assetJsonDto.assetPropDescription,
            assetJsonDto.assetPropContentType,
            assetJsonDto.assetPropVersion,
            assetJsonDto.agrovocKeywords,
            assetJsonDto.latitude,
            assetJsonDto.longitude,
            assetJsonDto.dateRange,
            assetJsonDto.openApiDescription,
            assetJsonDto.dataAddressKeyName,
        )

        val jwtToken = getJwtToken().tokenValue
        val accessPolicyJson: String =
            this.edcIntegrationService.getPolicyforAsset(jwtToken, bucketName, accessPolicyName, assetName)
        val contractPolicyJson: String =
            this.edcIntegrationService.getPolicyforAsset(jwtToken, bucketName, contractPolicyName, assetName)
        val accessPolicyUUID: String = this.edcBusinessService.extractIdfromPolicy(accessPolicyJson)
        val contractPolicyUUID: String = this.edcBusinessService.extractIdfromPolicy(contractPolicyJson)
        val contractUUID = UUID.randomUUID().toString()
        val contractDefinitionJson =
            edcBusinessService.createContractDefinitionJson(
                assetPropId,
                accessPolicyUUID,
                contractPolicyUUID,
                contractUUID
            )

        this.edcIntegrationService.publishAsset(assetJson, accessPolicyJson, contractPolicyJson, contractDefinitionJson)

        val publishedAsset = Asset()

        publishedAsset.bucket = bucketName
        publishedAsset.name = assetName
        publishedAsset.assetId = assetPropId
        publishedAsset.accessPolicyId = accessPolicyUUID
        publishedAsset.contractPolicyId = contractPolicyUUID
        publishedAsset.contractId = contractUUID

        assetRepository.save(publishedAsset)
    }

    private fun getJwtToken(): Jwt {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        return jwtAuthenticationToken.token
    }

    private fun getBucketName(): String {
        return getJwtToken().claims["preferred_username"].toString().ifEmpty {
            throw BusinessException(
                "Could not extract preferred_username from jwt.",
                ErrorType.BAD_REQUEST
            )
        }
    }

    /**
     * Delete a policy from a MinIO bucket.
     *
     * @param bucketName name of the MinIO bucket
     * @param policyName name of the policy
     * @return TODO
     */
    @DeleteMapping("policies/{bucketName}/{policyName}")
    fun deletePolicy(@PathVariable bucketName: String, @PathVariable policyName: String) {
        TODO("Not yet implemented")
    }


    /**
     * Get a policy from a MinIO bucket.
     *
     * @param bucketName name of the MinIO bucket
     * @param policyName name of the policy
     * @return TODO
     */
    @GetMapping("policies/{bucketName}/{policyName}")
    fun getPolicy(@PathVariable bucketName: String, @PathVariable policyName: String) {
        TODO("Not yet implemented")
    }

}
