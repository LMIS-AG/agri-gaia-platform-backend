package de.agrigaia.platform.model.coopspace

import de.agrigaia.platform.model.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Entity
class Member (
    var name: String? = null,
    var company: String? = null,
    var email: String? = null,
    @Enumerated(EnumType.STRING)
    var role: CoopSpaceRole? = null,
    var username: String? = null
) : BaseEntity()

