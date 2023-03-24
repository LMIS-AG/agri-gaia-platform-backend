package de.agrigaia.platform.persistence.repository

import de.agrigaia.platform.model.edc.Asset
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AssetRepository : JpaRepository<Asset, Long> {
    fun findByBucketAndName(bucket: String, name: String): Asset?
}


