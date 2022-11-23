package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.toEntity
import de.agrigaia.platform.integration.coopspace.CoopSpaceService
import de.agrigaia.platform.integration.keycloak.KeycloakService
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.coopspace.CoopSpaceRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/coopspaces")
class CoopSpaceController @Autowired constructor(
    private val coopSpaceService: CoopSpaceService,
    private val keycloakService: KeycloakService,
    private val mapper: CoopSpaceMapper,
    private val minioService: MinioService,
) : BaseController() {

    @GetMapping
    fun getCoopSpaces(): ResponseEntity<List<CoopSpaceDto>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val buckets = minioService.listBuckets(jwt)

        println(" getCoopSpaces ");
        print(buckets)

        return ResponseEntity.ok(listOf(CoopSpaceDto(123, "exampleOne", "exampleTwo", "mgrave", mutableListOf())))
        // TODO implement real business logic
    }

    @GetMapping("/members")
    fun getMembers(): ResponseEntity<List<MemberDto>> {
        // Arbeitsstand / Versuch Keycloak anzusprechen
        // this.keycloakService.getUserResource("0e68593d-6604-4e7a-aa53-15b1af988c2d"); TODO Move into service method in business layer

        return ResponseEntity.ok(listOf(
            MemberDto(1,"Alopez", "LMIS", "abcd@test.de",  CoopSpaceRole.VIEWER, "alopez"),
            MemberDto(2,"Jende", "LMIS", "efgh@test.de", CoopSpaceRole.EDITOR, "jende"),
            MemberDto(3,"Ebelli", "LMIS", "ijkl@test.de", CoopSpaceRole.EDITOR, "ebelli")
        ))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.mapper)
        this.coopSpaceService.createCoopSpace(coopSpace)
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.mapper)
        this.coopSpaceService.deleteCoopSpace(coopSpace)
    }

}

