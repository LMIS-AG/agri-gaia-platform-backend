package de.agrigaia.platform.integration.minio

import io.minio.MinioClient
import io.minio.credentials.Jwt
import io.minio.credentials.WebIdentityProvider
import io.minio.messages.Bucket
import org.springframework.stereotype.Service


@Service
class MinioService {
    fun listBuckets(jwt: String): MutableList<Bucket>? {
        val minioClient = MinioClient.builder()
            .credentialsProvider(
                WebIdentityProvider(
                    { Jwt(jwt, 8600) },
                    "https://minio-test-api.platform.agri-gaia.com",
                    null, null, null, null, null
                )
            )
            .endpoint("https://minio-test-api.platform.agri-gaia.com")
            .build()

        val buckets = minioClient.listBuckets()
        return buckets;
    }
}