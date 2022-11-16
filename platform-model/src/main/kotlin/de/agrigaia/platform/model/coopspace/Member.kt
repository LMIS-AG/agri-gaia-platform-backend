package de.agrigaia.platform.model.coopspace

import de.agrigaia.platform.model.BaseEntity
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Entity
class Member (
    var name: String? = null,
    var company: String? = null,
    var email: String? = null,
    @Enumerated(EnumType.STRING)
    var role: CoopSpaceRole? = null,
    var username: String? = null
) : BaseEntity()

