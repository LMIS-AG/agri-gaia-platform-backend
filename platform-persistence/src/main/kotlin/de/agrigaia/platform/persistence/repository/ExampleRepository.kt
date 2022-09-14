package de.agrigaia.platform.persistence.repository

import de.agrigaia.platform.model.example.ExampleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ExampleRepository : JpaRepository<ExampleEntity, Long?>