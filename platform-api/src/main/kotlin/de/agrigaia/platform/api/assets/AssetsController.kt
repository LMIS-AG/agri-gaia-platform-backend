package de.agrigaia.platform.api.assets


import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.integration.assets.AssetsService
import de.agrigaia.platform.integration.minio.MinioService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/assets")
class AssetsController @Autowired constructor(
    private val assetsService: AssetsService,
    private val minioService: MinioService
) : BaseController() {

    @PostMapping("{bucket}/{name}")
    @ResponseStatus(HttpStatus.CREATED)
    fun publishAsset(@PathVariable bucket: String, @PathVariable name: String) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val assetJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/asset.json")
        val policyJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/policy.json")
        val catalogJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/catalog.json")

        this.assetsService.publishAsset(assetJson, policyJson, catalogJson)
    }

    //TODO maybe refactor duplicated code

    @DeleteMapping("{bucket}/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unpublishAsset(@PathVariable bucket: String, @PathVariable name: String) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val assetJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/asset.json")
        val policyJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/policy.json")
        val catalogJson = this.minioService.getFileContent(jwt, bucket, "config/${name}/catalog.json")

        val assetMap = ObjectMapper().readValue<MutableMap<String, MutableMap<String, Any>>>(assetJson)
        val policyMap = ObjectMapper().readValue<MutableMap<String, Any>>(policyJson)
        val catalogMap = ObjectMapper().readValue<MutableMap<String, Any>>(catalogJson)

        val assetId = assetMap.get("asset")!!.get("id") as String
        val policyId = policyMap.get("id") as String
        val contractId = catalogMap.get("id") as String

        this.assetsService.unpublishAsset(assetId, policyId, contractId)
    }
}
