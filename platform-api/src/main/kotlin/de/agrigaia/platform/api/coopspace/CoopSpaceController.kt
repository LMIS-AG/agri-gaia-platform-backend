package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.toEntity
import de.agrigaia.platform.business.services.coopspace.CoopSpaceService
import de.agrigaia.platform.model.coopspace.Member
import de.agrigaia.platform.model.coopspace.CoopSpace
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/coopspaces")
class CoopSpaceController @Autowired constructor(
    private val coopSpaceService: CoopSpaceService,
    private val mapper: CoopSpaceMapper,
) : BaseController() {

    @GetMapping
    fun getCoopSpaces(): ResponseEntity<List<CoopSpaceDto>> {
        return ResponseEntity.ok(listOf(CoopSpaceDto(123, "exampleOne", "exampleTwo", mutableListOf())))
        // TODO implement real business logic
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.mapper)

        // TODO remove when not needed anymore
        var member1: Member = Member("jende", "lmis")
        var member2: Member = Member("alopez", "bosch")
        coopSpace.members = mutableListOf(member1, member2)

        this.coopSpaceService.log(coopSpace)
        // TODO implement real business logic
    }

}

