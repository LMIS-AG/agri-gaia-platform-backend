package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.EntityToDtoMapper
import de.agrigaia.platform.api.config.MapperConfiguration
import de.agrigaia.platform.model.coopspace.Member
import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper

@Mapper(config = MapperConfiguration::class)
interface MemberMapper : EntityToDtoMapper<Member, MemberDto> {

    override fun map(entity: Member): MemberDto

    @InheritInverseConfiguration
    override fun map(dto: MemberDto): Member

}