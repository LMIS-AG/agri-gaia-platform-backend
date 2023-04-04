package de.agrigaia.platform.model.coopspace

import de.agrigaia.platform.model.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany

@Entity
class CoopSpace(
    var name: String? = null,
    var company: String? = null,
    var mandant: String? = null,
    @JoinColumn(name = "member_id")
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    var members: List<Member> = ArrayList()
) : BaseEntity()
