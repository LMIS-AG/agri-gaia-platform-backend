package de.agrigaia.platform.integration.minio

import de.agrigaia.platform.model.buckets.STSDto
import io.minio.*
import io.minio.credentials.Jwt
import io.minio.credentials.WebIdentityProvider
import io.minio.messages.*
import org.apache.logging.log4j.LogManager.getLogger
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import org.jsoup.Jsoup
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono


@Service
class MinioService(
    private val minioProperties: MinioProperties,
) {
    fun listBuckets(jwt: String): MutableList<Bucket> {
        val minioClient = getMinioClient(jwt)

        return minioClient.listBuckets()
    }

    fun bucketExists(name: String): Boolean {
        val minioClient = this.getMinioClientForTechnicalUser()

        return minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build())
    }

    fun getAssetsForCoopspace(jwt: String, company: String, bucketName: String, folder: String): List<Result<Item>> {
        val minioClient = this.getMinioClient(jwt)

        val bucketArgs = ListObjectsArgs.builder()
            .bucket("prj-$company-$bucketName")
            .recursive(true)
            .prefix(folder)
            .build()

        return minioClient.listObjects(bucketArgs).toList()
    }

    fun getPublishableAssetsForBucket(jwt: String, bucketName: String, folder: String): List<Result<Item>> {
        val minioClient = this.getMinioClient(jwt)

        val bucketArgs = ListObjectsArgs.builder()
            .bucket(bucketName)
            .recursive(true)
            .prefix("/$folder")
            .build()

        return minioClient.listObjects(bucketArgs).toList()
    }

    fun getFileContent(jwt: String, bucketName: String, fileName: String): String {
        val minioClient = this.getMinioClient(jwt)

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

    fun uploadAssets(jwt: String, bucketName: String, currentRoot: String, files: Array<MultipartFile>) {
        val minioClient = this.getMinioClient(jwt)

        val snowballObjects: List<SnowballObject> = files.map { file ->
            val objectName = "/Dockerfile"
            SnowballObject(
                objectName,
                ByteArrayInputStream(file.bytes),
                file.size,
                null,
            )
        }

        minioClient.uploadSnowballObjects(
            UploadSnowballObjectsArgs.builder().bucket(bucketName).objects(snowballObjects).build()
        )
    }

    fun deleteAsset(jwt: String, bucket: String, fileName: String) {
        val minioClient = this.getMinioClient(jwt)

        minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).`object`(fileName).build())
    }

    // TODO Please fix this, it's so bad
    private fun fixString(assetJson: String) =
        "{" + assetJson.replace("\" ", " ").replace("\",", ",").replace("\"\n", "\n").replace("\"\"", "\"")


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

    private fun getMinioClientForTechnicalUser(): MinioClient = MinioClient.builder()
        .endpoint(this.minioProperties.url)
        .credentials(this.minioProperties.technicalUserAccessKey, this.minioProperties.technicalUserSecretKey)
        .build()

    fun makeSTSRequest(jwt: String): STSDto {
        val body = LinkedMultiValueMap<String, String>()
        body.add("WebIdentityToken", jwt)
        body.add("Action", "AssumeRoleWithWebIdentity")
        body.add("Version", "2011-06-15")
        body.add("DurationSeconds", "21600")

        val webClient = WebClient.builder().baseUrl(this.minioProperties.url).build()

        val request = webClient.post()
            .uri("/")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData(body))

        val response: String = request
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, ::handleClientError)
            .onStatus(HttpStatus::is5xxServerError, ::handleServerError)
            .bodyToMono(String::class.java)
            .block() ?: throw Exception("Response from Minio was null.")

        val parsedResponse = Jsoup.parse(response)
        val accessKey = parsedResponse.getElementsByTag("accesskeyid")[0].childNode(0).toString().removePrefix("\n")
        val secretKey = parsedResponse.getElementsByTag("secretaccesskey")[0].childNode(0).toString().removePrefix("\n")
        val sessionToken = parsedResponse.getElementsByTag("sessiontoken")[0].childNode(0).toString().removePrefix("\n")

        // Create an STSResponse object and return it
        return STSDto(accessKey, secretKey, sessionToken)
    }

    private fun handleClientError(response: ClientResponse): Mono<out Throwable> {
        return response.bodyToMono(String::class.java)
            .doOnNext { getLogger().error("${response.statusCode()}: $it") }
            .then(response.createException())
    }

    private fun handleServerError(response: ClientResponse): Mono<Throwable> {
        return response.bodyToMono(String::class.java).flatMap {
            Mono.error(Exception("Minio server error: $it"))
        }
    }
}
