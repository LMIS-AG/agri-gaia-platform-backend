package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.EntityToDtoMapper
import de.agrigaia.platform.api.config.MapperConfiguration
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.example.ExampleEntity
import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper

@Mapper(config = MapperConfiguration::class)
interface CoopSpaceMapper : EntityToDtoMapper<CoopSpace, CoopSpaceDto> {

    override fun map(entity: CoopSpace): CoopSpaceDto

    @InheritInverseConfiguration
    override fun map(dto: CoopSpaceDto): CoopSpace
}