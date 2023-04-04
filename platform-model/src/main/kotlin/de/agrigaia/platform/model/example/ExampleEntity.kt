package de.agrigaia.platform.model.example

import de.agrigaia.platform.model.BaseEntity
import jakarta.persistence.Entity

@Entity
class ExampleEntity(
    var lineOne: String? = null,
    var lineTwo: String? = null
) : BaseEntity()