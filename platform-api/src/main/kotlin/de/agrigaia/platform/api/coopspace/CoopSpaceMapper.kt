package de.agrigaia.platform.api.example

import de.agrigaia.platform.api.EntityToDtoMapper
import de.agrigaia.platform.api.config.MapperConfiguration
import de.agrigaia.platform.model.example.ExampleEntity
import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper

@Mapper(config = MapperConfiguration::class)
interface ExampleMapper : EntityToDtoMapper<ExampleEntity, ExampleDto> {

    override fun map(entity: ExampleEntity): ExampleDto

    @InheritInverseConfiguration
    override fun map(dto: ExampleDto): ExampleEntity
}