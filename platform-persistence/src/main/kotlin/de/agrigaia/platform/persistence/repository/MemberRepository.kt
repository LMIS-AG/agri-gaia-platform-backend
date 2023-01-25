package de.agrigaia.platform.persistence.repository

import de.agrigaia.platform.model.coopspace.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long>
