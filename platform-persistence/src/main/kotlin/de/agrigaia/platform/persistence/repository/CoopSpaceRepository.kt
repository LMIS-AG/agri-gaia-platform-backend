package de.agrigaia.platform.persistence.repository

import de.agrigaia.platform.model.coopspace.CoopSpace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CoopSpaceRepository : JpaRepository<CoopSpace, Long>