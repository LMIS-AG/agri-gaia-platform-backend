package de.agrigaia.platform.api.edc

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.business.edc.EdcBusinessService
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.edc.EdcIntegrationService
import de.agrigaia.platform.model.edc.Asset
import de.agrigaia.platform.model.edc.PolicyDto
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
     * Return a list of all assets from the user's Minio bucket.
     *
     * @return list of assets in MinIO user's bucket
     */
    @GetMapping("assets")
    fun getAllAssets(): ResponseEntity<List<String>> {
        TODO("Not yet implemented")
    }


    /**
     * Get an asset from the user's MinIO bucket.
     *
     * @param assetName name of the policy
     * @return TODO
     */
    @GetMapping("assets/{assetName}")
    fun getAsset(@PathVariable assetName: String) {
        TODO("Not yet implemented")
    }

    /**
     * Add an asset to the user's MinioBucket.
     *
     * @param assetName name of the asset
     * @return TODO
     */
    @PostMapping("assets/{assetName}")
    fun addAsset(@PathVariable assetName: String, @RequestBody assetJson: String) {
        TODO("Not yet implemented")
    }


    /**
     * Delete an asset from the user's MinIO bucket.
     *
     * @param assetName name of the policy
     * @return TODO
     */
    @DeleteMapping("assets/{assetName}")
    fun deleteAsset(@PathVariable assetName: String) {
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
     *
     * @param policyName name of the policy
     * @policyJson request body, json of the policy
     */
    @PostMapping("policies/{policyName}")
    fun addPolicy(@PathVariable policyName: String, @RequestBody policyJson: String) {
        val jwtTokenValue = getJwtToken().tokenValue
        val bucketName = getBucketName()
        edcIntegrationService.addPolicy(jwtTokenValue, bucketName, policyName, policyJson)
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
        val policyId = asset.policyId
            ?: throw BusinessException("policyId was null", ErrorType.BAD_REQUEST)
        val contractId = asset.contractId
            ?: throw BusinessException("contractId was null", ErrorType.BAD_REQUEST)

        assetRepository.delete(asset)
        this.edcIntegrationService.unpublishAsset(assetId, policyId, contractId)
    }

    @PostMapping("publish/{bucketName}/{assetName}/{policyName}")
    @ResponseStatus(HttpStatus.CREATED)
    fun publishAsset(
        @PathVariable bucketName: String,
        @PathVariable assetName: String,
        @PathVariable policyName: String,
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
            assetJsonDto.dataAddressKeyName
        )

        val jwtToken = getJwtToken().tokenValue
        val policyJson: String =
            this.edcIntegrationService.getPolicyforAsset(jwtToken, bucketName, policyName, assetName)
        val policyUUID: String = this.edcBusinessService.extractIdfromPolicy(policyJson)
        val contractUUID = UUID.randomUUID().toString()
        val contractDefinitionJson =
            edcBusinessService.createContractDefinitionJson(assetPropId, policyUUID, contractUUID)

        this.edcIntegrationService.publishAsset(assetJson, policyJson, contractDefinitionJson)

        val publishedAsset = Asset()

        publishedAsset.bucket = bucketName
        publishedAsset.name = assetName
        publishedAsset.assetId = assetPropId
        publishedAsset.policyId = policyUUID
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
     * Returns a list of policy names from a Minio bucket.
     *
     * @param bucketName name of the MinIO bucket
     * @return list of names of the policies in MinIO bucket `bucketName`
     */
    @GetMapping("policies/{bucketName}")
    fun getPolicyNames(@PathVariable bucketName: String): ResponseEntity<List<String>> {
        val jwt = getJwt()
        val policyNames: List<String> = this.edcIntegrationService.getAllPolicyNames(jwt, bucketName)
        return ResponseEntity.ok(policyNames)
    }


    /**
     * Save a policy to a MinioBucket.
     *
     * @param bucketName name of the MinIO bucket
     * @param policyName name of the policy
     * @return TODO
     */
    @PostMapping("policies/{bucketName}/{policyName}")
    fun addPolicy(@PathVariable bucketName: String, @PathVariable policyName: String, @RequestBody policyJson: String) {
        TODO("Not yet implemented")
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

    private fun getJwt(): String {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        return jwtAuthenticationToken.token.tokenValue
    }
}
