package de.agrigaia.platform.api

import de.agrigaia.platform.model.BaseEntity

fun <TDto : BaseDto, TEntity : BaseEntity> TDto.toEntity(mapper: EntityToDtoMapper<TEntity, TDto>): TEntity =
    mapper.map(this)

fun <TDto : BaseDto, TEntity : BaseEntity> TEntity.toDto(mapper: EntityToDtoMapper<TEntity, TDto>): TDto =
    mapper.map(this)

fun <TDto : BaseDto, TEntity : BaseEntity> Collection<TDto>.toEntities(mapper: EntityToDtoMapper<TEntity, TDto>): List<TEntity> =
    this.map { it.toEntity(mapper) }

fun <TDto : BaseDto, TEntity : BaseEntity> Collection<TEntity>.toDtos(mapper: EntityToDtoMapper<TEntity, TDto>): List<TDto> =
    this.map { it.toDto(mapper) }