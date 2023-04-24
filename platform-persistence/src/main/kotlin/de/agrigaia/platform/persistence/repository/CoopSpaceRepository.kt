package de.agrigaia.platform.persistence.repository

import de.agrigaia.platform.model.coopspace.CoopSpace
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CoopSpaceRepository : JpaRepository<CoopSpace, Long> {
    // Return `Optional<CoopSpace>` instead of `CoopSpace?` so method works similarly to built in ones like
    // `findById()`, which also returns an `Optional<T>`.
    fun findByName(name: String): Optional<CoopSpace>
}
