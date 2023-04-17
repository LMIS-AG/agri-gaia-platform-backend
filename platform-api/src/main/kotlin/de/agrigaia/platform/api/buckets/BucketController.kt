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
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.Charset
import java.util.*

@RestController
@RequestMapping("/buckets")
class BucketController @Autowired constructor(
        private val minioService: MinioService,
        private val assetRepository: AssetRepository,
) : BaseController() {

    /*
    Returns all non-coopspace buckets the user has access to.
     */
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

    /*
    Returns assets in a given MinIO bucket. Currently returns empty list if bucket empty and 204 if user has no access.
     */
    @GetMapping("{bucket}/{base64encodedFolderName}")
    fun getBucketAssets(@PathVariable bucket: String, @PathVariable base64encodedFolderName: String): ResponseEntity<List<AssetDto>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val folder = String(Base64.getDecoder().decode(base64encodedFolderName))

        // TODO: `minioService.getPublishableAssetsForBucket()` returns a non-nullable list.
        //  This should check if the `assetsForBucket` list is empty and, if so, return 204.
        //  If user has no access, return 403.
        try {
            val assetsForBucket = this.minioService.getPublishableAssetsForBucket(jwt, bucket, folder)
                .map { it.get() }
                .map { asset ->
                    AssetDto(
                        asset.objectName().replace("assets/", ""),
                        asset.lastModified().toString(),
                        asset.lastModified().toString(),
                        asset.size().toString(),
                        "label",
                        bucket,
                        isPublished(bucket, asset.objectName().replace("assets/", ""))
                    )
                }
            return ResponseEntity.ok(assetsForBucket)
        } catch (e: Exception) {
            return ResponseEntity.noContent().build()
        }
    }

    // TODO
    private fun isPublished(bucket: String, name: String): Boolean {
        return assetRepository.findByBucketAndName(bucket, name) != null
    }

    // TODO `this.minioService.uploadAssets()` may fail if no access to requested bucket.
    @PostMapping("upload/{bucket}/{base64encodedFolderName}")
    @ResponseStatus(HttpStatus.OK)
    fun uploadAsset(@PathVariable bucket: String, @PathVariable base64encodedFolderName: String, @RequestBody files: Array<MultipartFile>) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val folder = if (base64encodedFolderName == "default") "/" else String(
            (Base64.getDecoder().decode(base64encodedFolderName)), Charset.forName("ISO-8859-1"))

        this.minioService.uploadAssets(jwt, bucket, folder, files)
    }

    @PostMapping("downloadAsset/{bucket}/{base64EncodedFileName}")
    @ResponseStatus(HttpStatus.OK)
    fun downloadAsset(@PathVariable bucket: String, @PathVariable base64EncodedFileName: String, response: HttpServletResponse) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val decodedBytes = Base64.getDecoder().decode(base64EncodedFileName)
        val name = String(decodedBytes, Charset.forName("ISO-8859-1"))

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

    // TODO `this.minioService.deleteAsset()` may fail if no access to requested bucket.
    @DeleteMapping("delete/{bucket}/{base64EncodedFileName}")
    @ResponseStatus(HttpStatus.OK)
    fun deleteAsset(@PathVariable bucket: String, @PathVariable base64EncodedFileName: String) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val decodedBytes = Base64.getDecoder().decode(base64EncodedFileName)
        val name = String(decodedBytes, Charset.forName("ISO-8859-1"))
        this.minioService.deleteAsset(jwt, bucket, name)
    }

    // TODO
    @GetMapping("/sts")
    fun getKeysAndToken(): ResponseEntity<STSDto> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        val stsDto = this.minioService.makeSTSRequest(jwt)

        return ResponseEntity.ok(stsDto)
    }

}
