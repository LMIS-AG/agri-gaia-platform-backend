package de.agrigaia.platform.api.buckets

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.coopspace.AssetDto
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.buckets.STSDto
import de.agrigaia.platform.persistence.repository.AssetRepository
import io.minio.messages.Bucket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/buckets")
class BucketController @Autowired constructor(
        private val minioService: MinioService,
        private val assetRepository: AssetRepository,
) : BaseController() {

    @GetMapping
    fun getBuckets(): ResponseEntity<List<BucketDto>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val buckets = minioService.listBuckets(jwt)

        val bucketDtos: MutableList<BucketDto> = buckets
            .filter { !it.name().startsWith("prj-") }
            .map { bucket: Bucket -> BucketDto(bucket.name()) } as MutableList<BucketDto>

        return ResponseEntity.ok(bucketDtos)
    }

    @GetMapping("{bucket}/{base64encodedFolderName}")
    fun getBucketAssets(@PathVariable bucket: String, @PathVariable base64encodedFolderName: String): ResponseEntity<List<AssetDto>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val folder = String(Base64.getDecoder().decode(base64encodedFolderName))

        return try {
            val assetsForBucket = this.minioService.getPublishableAssetsForBucket(jwt, bucket, folder)
                .map { it.get() }
                .map { asset ->
                    AssetDto(asset.objectName().replace("assets/", ""), asset.lastModified().toString(), asset.lastModified().toString(),
                        asset.size().toString(), "label", bucket, isPublished(bucket, asset.objectName().replace("assets/", "")))
                }
            ResponseEntity.ok(assetsForBucket)
        } catch (e: Exception) {
            ResponseEntity.noContent().build()
        }
    }

    private fun isPublished(bucket: String, name: String): Boolean {
        return assetRepository.findByBucketAndName(bucket, name) != null
    }

    @PostMapping("upload/{bucket}/{base64encodedFolderName}")
    @ResponseStatus(HttpStatus.OK)
    fun uploadAsset(@PathVariable bucket: String, @PathVariable base64encodedFolderName: String, @RequestBody files: Array<MultipartFile>) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        var folder = "/"
        if (base64encodedFolderName != "default") {
            folder = String(Base64.getDecoder().decode(base64encodedFolderName))
        }

        this.minioService.uploadAssets(jwt, bucket, folder, files)
    }

    @PostMapping("downloadAsset/{bucket}/{base64EncodedFileName}")
    @ResponseStatus(HttpStatus.OK)
    fun downloadAsset(@PathVariable bucket: String, @PathVariable base64EncodedFileName: String, response: HttpServletResponse) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val name = String(Base64.getDecoder().decode(base64EncodedFileName))

        this.minioService.downloadAsset(jwt, bucket, name, response)
    }

    @PostMapping("downloadFolder/{bucket}/{base64EncodedFolderName}")
    @ResponseStatus(HttpStatus.OK)
    fun downloadFolder(@PathVariable bucket: String, @PathVariable base64EncodedFolderName: String, response: HttpServletResponse) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val folderName = String(Base64.getDecoder().decode(base64EncodedFolderName))

        this.minioService.downloadFolder(jwt, bucket, folderName, response)
    }

    @DeleteMapping("delete/{bucket}/{base64EncodedFileName}")
    @ResponseStatus(HttpStatus.OK)
    fun deleteAsset(@PathVariable bucket: String, @PathVariable base64EncodedFileName: String) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val name = String(Base64.getDecoder().decode(base64EncodedFileName))

        this.minioService.deleteAsset(jwt, bucket, name)
    }

    @GetMapping("/sts")
    fun getKeysAndToken(): ResponseEntity<STSDto> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val stsDto = this.minioService.makeSTSRequest(jwt)

        return ResponseEntity.ok(stsDto)
    }

}
