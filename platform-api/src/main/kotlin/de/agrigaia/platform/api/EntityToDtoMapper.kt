package de.agrigaia.platform.api

import de.agrigaia.platform.model.BaseEntity

interface EntityToDtoMapper<TEntity : BaseEntity, TDto : BaseDto> {
    fun map(dto: TDto): TEntity
    fun map(entity: TEntity): TDto

    fun mapToEntities(entity: List<TDto>): List<TEntity>
    fun mapToDtos(entity: List<TEntity>): List<TDto>
}