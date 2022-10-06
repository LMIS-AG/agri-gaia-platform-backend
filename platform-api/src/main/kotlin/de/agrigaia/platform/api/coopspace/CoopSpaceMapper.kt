package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.EntityToDtoMapper
import de.agrigaia.platform.api.config.MapperConfiguration
import de.agrigaia.platform.model.coopspace.CoopSpace
import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = MapperConfiguration::class)
interface CoopSpaceMapper : EntityToDtoMapper<CoopSpace, CoopSpaceDto> {

    override fun map(entity: CoopSpace): CoopSpaceDto

    @InheritInverseConfiguration
    @Mapping(target = "members", ignore = true) // TODO adjust later when frontend is adjusted
    override fun map(dto: CoopSpaceDto): CoopSpace

}