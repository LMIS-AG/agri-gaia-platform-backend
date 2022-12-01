package de.agrigaia.platform.integration.minio

import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.Result
import io.minio.credentials.Jwt
import io.minio.credentials.WebIdentityProvider
import io.minio.messages.Bucket
import io.minio.messages.Item
import org.springframework.stereotype.Service


@Service
class MinioService (private val minioProperties: MinioProperties){
    fun listBuckets(jwt: String): MutableList<Bucket> {
        val minioClient = getMinioClient(jwt)

        val buckets = minioClient.listBuckets()
        return buckets;
    }

    fun getAssetsForCoopscpae(jwt: String, company: String, bucketName: String): List<Result<Item>> {
        val minioClient = this.getMinioClient(jwt)

        val bucketArgs = ListObjectsArgs.builder()
            .bucket("prj-$company-$bucketName")
            .build()

        return minioClient.listObjects(bucketArgs).toList()
    }
    fun getAssetsForBucket(jwt: String, bucketName: String): List<Result<Item>> {
        val minioClient = this.getMinioClient(jwt)

        val bucketArgs = ListObjectsArgs.builder()
            .bucket(bucketName)
            .build()

        return minioClient.listObjects(bucketArgs).toList()
    }

    private fun getMinioClient(jwt: String): MinioClient = MinioClient.builder()
        .credentialsProvider(
            WebIdentityProvider(
                { Jwt(jwt, 8600) },
                this.minioProperties.url!!,
                null, null, null, null, null
            )
        )
        .endpoint(this.minioProperties.url)
        .build()
}
