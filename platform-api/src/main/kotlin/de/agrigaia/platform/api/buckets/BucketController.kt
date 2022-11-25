package de.agrigaia.platform.api.buckets

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.integration.minio.MinioService
import io.minio.messages.Bucket
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/buckets")
class BucketController@Autowired constructor(
    private val minioService: MinioService,
) : BaseController() {

    @GetMapping
    fun getBuckets(): ResponseEntity<List<BucketDto>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val buckets = minioService.listBuckets(jwt)

        println(" getBuckets ");
        print(buckets)

        // TODO filter buckets?

        var bucketDtos:MutableList<BucketDto> = mutableListOf();
        if (buckets != null) {
            bucketDtos = buckets.map { bucket: Bucket -> BucketDto(bucket.name()) } as MutableList<BucketDto>
        }

        return ResponseEntity.ok(bucketDtos)
    }

}