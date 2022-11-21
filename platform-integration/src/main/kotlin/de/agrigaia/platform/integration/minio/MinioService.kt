package de.agrigaia.platform.integration.minio
import io.minio.MinioClient
import io.minio.messages.Bucket
import org.springframework.stereotype.Service


@Service
class MinioService constructor() {

    // TODO move endpoint etc. into configuration
    private var minioClient = MinioClient.builder()
        .endpoint("https://minio-test-api.platform.agri-gaia.com")
        .credentials("MPQTCR3JV86LR4HI6WRN", "uZcdLk1UtRfbB5YdbLgPqKsuUJDrTvVDKbAVVqXD")
        .build()

    fun listBuckets(): MutableList<Bucket>? {
        println(" listBuckets ");
        var buckets = minioClient.listBuckets();
        return buckets;
    }
}