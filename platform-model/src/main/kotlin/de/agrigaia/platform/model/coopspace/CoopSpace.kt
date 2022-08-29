package de.agrigaia.platform.model.coopspace

import de.agrigaia.platform.model.BaseEntity
import javax.persistence.Entity

@Entity
class CoopSpace (
    var lineOne: String? = null,
    var lineTwo: String? = null
) : BaseEntity()