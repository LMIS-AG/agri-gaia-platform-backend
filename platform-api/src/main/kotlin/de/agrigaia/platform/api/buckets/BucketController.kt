package de.agrigaia.platform.api.buckets

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.coopspace.AssetDto
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.buckets.STSDto
import io.minio.messages.Bucket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/buckets")
class BucketController @Autowired constructor(
        private val minioService: MinioService
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

    @GetMapping("{bucket}/assets")
    fun getBucketAssets(@PathVariable bucket: String): ResponseEntity<List<AssetDto>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        return try {
            val assetsForBucket = this.minioService.getPublishableAssetsForBucket(jwt, bucket)
                    .map { it.get() }
                    .map { AssetDto(it.etag(), it.objectName().replace("assets/", ""), it.lastModified().toString(), it.lastModified().toString(),
                        it.size().toString(), "label", bucket) }
            ResponseEntity.ok(assetsForBucket)
        } catch (e: Exception) {
            ResponseEntity.noContent().build()
        }
    }

    @PostMapping("upload/{bucket}")
    @ResponseStatus(HttpStatus.OK)
    fun uploadAsset(@PathVariable bucket: String, @RequestBody files: Array<MultipartFile>) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        this.minioService.uploadAssets(jwt, bucket, files)
    }

    @DeleteMapping("delete/{bucket}/{name}")
    @ResponseStatus(HttpStatus.OK)
    fun deleteAsset(@PathVariable bucket: String, @PathVariable name: String) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

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
