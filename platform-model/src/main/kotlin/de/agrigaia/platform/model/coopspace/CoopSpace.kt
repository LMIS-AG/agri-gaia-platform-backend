package de.agrigaia.platform.model.coopspace

import de.agrigaia.platform.model.BaseEntity
import javax.persistence.Entity
import javax.persistence.OneToMany

@Entity
class CoopSpace(var name: String? = null, var company: String? = null, @OneToMany var members: List<Member> =  ArrayList()) : BaseEntity()