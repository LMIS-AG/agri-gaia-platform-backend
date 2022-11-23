package de.agrigaia.platform.integration.minio
import io.minio.MinioClient
import io.minio.messages.Bucket
import org.springframework.stereotype.Service


@Service
class MinioService constructor() {

    // TODO move endpoint etc. into configuration
    private var minioClient = MinioClient.builder()
        .endpoint("https://minio-test-api.platform.agri-gaia.com")
        .credentials("7SBIOO6XCSUYV81732ED", "eR3hBk8JNX87m1ieRog4zB+5GcXi1Ixmxd5pc72+")
        //.credentials("api-test-user", "test1234")
        .build()

    fun listBuckets(jwt: String): MutableList<Bucket>? {
        println(" listBuckets ");
        var buckets = minioClient.listBuckets();
        return buckets;
    }
}