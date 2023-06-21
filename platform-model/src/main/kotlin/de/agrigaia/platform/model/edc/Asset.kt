package de.agrigaia.platform.model.edc

import de.agrigaia.platform.model.BaseEntity
import jakarta.persistence.Entity


@Entity
class Asset (
    var name: String? = null,
    var bucket: String? = null,
    var assetId: String? = null,
    var accessPolicyId: String? = null,
    var contractPolicyId: String? = null,
    var contractId: String? = null

) : BaseEntity()
