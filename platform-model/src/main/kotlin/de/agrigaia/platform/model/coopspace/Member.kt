package de.agrigaia.platform.model.coopspace

import de.agrigaia.platform.model.BaseEntity
import javax.persistence.Entity

@Entity
class Member (
    var name: String? = null,
    var company: String? = null
) : BaseEntity()