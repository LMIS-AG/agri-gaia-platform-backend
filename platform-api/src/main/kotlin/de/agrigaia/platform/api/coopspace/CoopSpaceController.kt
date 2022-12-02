package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.toEntity
import de.agrigaia.platform.business.coopspace.CoopSpaceService
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.coopspace.CoopSpaceRole
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*


// TODO Parse JWT and look for roles to see if the user has the rights for the coopspaces and buckets (local db and minio)
@RestController
@RequestMapping("/coopspaces")
class CoopSpaceController @Autowired constructor(
    private val coopSpaceService: CoopSpaceService,
    private val coopSpaceMapper: CoopSpaceMapper,
    private val minioService: MinioService
) : BaseController() {

    @GetMapping
    fun getCoopSpaces(): ResponseEntity<List<CoopSpaceDto>> {
        val mapToDtos = this.coopSpaceMapper.mapToDtos(this.coopSpaceService.findAll())
        return ResponseEntity.ok(mapToDtos)
    }

    @GetMapping("{id}")
    fun getCoopSpace(@PathVariable id: Long): ResponseEntity<CoopSpaceDto> {
        return ResponseEntity.ok(this.coopSpaceMapper.map(this.coopSpaceService.findCoopSpace(id)))
    }

    @GetMapping("/members")
    fun getMembers(): ResponseEntity<List<MemberDto>> {
        // Arbeitsstand / Versuch Keycloak anzusprechen
        // this.keycloakService.getUserResource("0e68593d-6604-4e7a-aa53-15b1af988c2d")

        return ResponseEntity.ok(listOf(
            MemberDto(1,"Alejandro Lopez", "Bosch", "alejandro.lopez2@de.bosch.com",  CoopSpaceRole.VIEWER, "alopez"),
            MemberDto(2,"Julian Ende", "LMIS", "julian.ende@lmis.de", CoopSpaceRole.EDITOR, "jende"),
            MemberDto(3,"Enis Belli", "LMIS", "enis.belli@lmis.de", CoopSpaceRole.EDITOR, "ebelli"),
            MemberDto(4,"Katharina Beckwermert", "LMIS", "katharina.beckwermert@lmis.de", CoopSpaceRole.EDITOR, "kbeckwermert"),
            MemberDto(5,"Henning Wuebben", "LMIS", "henning.wuebben@lmis.de", CoopSpaceRole.EDITOR, "hwuebben"),
            MemberDto(6,"Christoph Manß", "DFKI", "christoph.manss@dfki.de", CoopSpaceRole.EDITOR, "cmanss")
        ))
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        this.coopSpaceService.createCoopSpace(coopSpace)
    }

    @PostMapping("delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        this.coopSpaceService.deleteCoopSpace(jwt, coopSpace)
    }

    @GetMapping("{id}/assets")
    fun getAssetsForCoopSpace(@PathVariable id: Long): ResponseEntity<Any> {
        val coopSpace = this.coopSpaceService.findCoopSpace(id)
        val company = coopSpace.company?.lowercase()
        val bucketName = coopSpace.name!!
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        return try {
            val assetsForBucket = this.minioService.getAssetsForCoopscpae(jwt, company!!, bucketName)
                .map { it.get() }
                .map { AssetDto(it.etag(), it.objectName(), it.lastModified().toString(), it.lastModified().toString(), "${it.size()}MB", "label", bucketName) }
            ResponseEntity.ok(assetsForBucket)
        } catch (e: Exception) {
            ResponseEntity.noContent().build()
        }
    }
}

