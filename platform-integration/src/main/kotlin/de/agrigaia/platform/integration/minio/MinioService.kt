package de.agrigaia.platform.integration.minio
import io.minio.*
import io.minio.credentials.Jwt
import io.minio.credentials.WebIdentityProvider
import io.minio.messages.*
import org.springframework.stereotype.Service
import java.io.BufferedReader


@Service
class MinioService(private val minioProperties: MinioProperties) {
    fun listBuckets(jwt: String): MutableList<Bucket> {
        val minioClient = getMinioClientForTechnicalUser(jwt)

        return minioClient.listBuckets()
    }

    fun bucketExists(name: String): Boolean {
        val minioClient = this.getMinioClientForTechnicalUser()

        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build())
    }

    fun getAssetsForCoopspace(jwt: String, company: String, bucketName: String): List<Result<Item>> {
        val minioClient = this.getMinioClientForTechnicalUser(jwt)

        val bucketArgs = ListObjectsArgs.builder()
                .bucket("prj-$company-$bucketName")
                .build()

        return minioClient.listObjects(bucketArgs).toList()
    }

    fun getPublishableAssetsForBucket(jwt: String, bucketName: String): List<Result<Item>> {
        val minioClient = this.getMinioClientForTechnicalUser(jwt)

        val bucketArgs = ListObjectsArgs.builder()
                .bucket(bucketName)
                .recursive(true)
                .prefix("assets/")
                .build()

        return minioClient.listObjects(bucketArgs).toList()
    }

    fun getFileContent(jwt: String, bucketName: String, fileName: String): String {
        val minioClient = this.getMinioClientForTechnicalUser(jwt)

        val sqlExpression = "select * from S3Object"
        val iss = InputSerialization(null, false, null, null, FileHeaderInfo.USE, null, null, null)
        val os = OutputSerialization(null, null, null, QuoteFields.ASNEEDED, null)

        val getObjectArgs = SelectObjectContentArgs.builder()
                .bucket(bucketName)
                .`object`(fileName)
                .sqlExpression(sqlExpression)
                .inputSerialization(iss)
                .outputSerialization(os)
                .requestProgress(true)
                .build()

        val selectObjectContent = minioClient.selectObjectContent(getObjectArgs)
        val text = selectObjectContent.bufferedReader().use(BufferedReader::readText)

        return fixString(text)
    }

    // TODO Please fix this, it's so bad
    private fun fixString(assetJson: String) =
        "{" + assetJson.replace("\" ", " ").replace("\",", ",").replace("\"\n", "\n").replace("\"\"", "\"")


    private fun getMinioClientForTechnicalUser(jwt: String): MinioClient = MinioClient.builder()
            .credentialsProvider(
                    WebIdentityProvider(
                            { Jwt(jwt, 8600) },
                            this.minioProperties.url!!,
                            null, null, null, null, null
                    )
            )
            .endpoint(this.minioProperties.url)
            .build()


    private fun getMinioClientForTechnicalUser(): MinioClient = MinioClient.builder()
        .endpoint(this.minioProperties.url)
        .credentials(this.minioProperties.technicalUserAccessKey, this.minioProperties.technicalUserSecretKey)
        .build()
}
