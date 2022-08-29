package de.agrigaia.platform.api.config

import de.agrigaia.platform.api.coopspace.CoopSpaceMapper
import de.agrigaia.platform.api.example.ExampleMapper
import org.mapstruct.InjectionStrategy
import org.mapstruct.MapperConfig
import org.mapstruct.ReportingPolicy

/**
 * Base configuration for all Mappers in the admin area.
 * All mappers should be specified in "uses" section.
 * This causes the mappers to "know" each other and enables them to use each other when mapping relations to other dtos.
 * (Note: This causes build warnings since each mapper has itself in its own "uses" list, but it does not seem to cause any problems.)
 */
@MapperConfig(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    uses = [ExampleMapper::class, CoopSpaceMapper::class])
interface MapperConfiguration 