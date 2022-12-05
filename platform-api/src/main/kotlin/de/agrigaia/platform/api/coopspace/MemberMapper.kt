package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.EntityToDtoMapper
import de.agrigaia.platform.api.config.MapperConfiguration
import de.agrigaia.platform.model.coopspace.Member
import org.mapstruct.InheritConfiguration
import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = MapperConfiguration::class)
interface MemberMapper : EntityToDtoMapper<Member, MemberDto>{
    @Mapping(target = "id", ignore = true)
    override fun map(dto: MemberDto): Member

    @InheritInverseConfiguration
    override fun map(entity: Member): MemberDto
}
