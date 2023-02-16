package de.agrigaia.platform.integration.minio

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "agrigaia-platform.minio")
open class MinioProperties(
    var url: String? = null,
    var technicalUserAccessKey: String? = null,
    var technicalUserSecretKey: String? = null
)
