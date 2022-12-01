package de.agrigaia.platform.model.coopspace

import de.agrigaia.platform.model.BaseEntity
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.OneToMany

@Entity
class CoopSpace(
    var name: String? = null,
    var company: String? = null,
    var mandant: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    var members: List<Member> = ArrayList()
) : BaseEntity()
