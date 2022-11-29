package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.EntityToDtoMapper
import de.agrigaia.platform.api.config.MapperConfiguration
import de.agrigaia.platform.model.coopspace.Member
import org.mapstruct.Mapper

@Mapper(config = MapperConfiguration::class)
interface MemberMapper : EntityToDtoMapper<Member, MemberDto>