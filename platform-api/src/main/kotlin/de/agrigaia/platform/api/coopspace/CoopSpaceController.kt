package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.toEntity
import de.agrigaia.platform.business.coopspace.CoopSpaceService
import de.agrigaia.platform.business.keycloak.KeycloakService
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.coopspace.DeleteMemberRequest
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
    private val minioService: MinioService,
    private val keycloakService: KeycloakService,
    private val memberMapper: MemberMapper,
) : HasLogger, BaseController() {

    @GetMapping
    fun getCoopSpaces(): ResponseEntity<List<CoopSpaceDto>> {

        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue

        // Filter the list of coop spaces, only returning those for which the user has access to the associated bucket
        val coopSpacesWithUserAccess = this.coopSpaceService.filterCoopSpacesByBucketAccess(
            coopSpaces = this.coopSpaceService.findAll(),  // All coopSpaces.
            buckets = this.minioService.listBuckets(jwt),  // Buckets with user access.
        )

        // Map the coop spaces to DTOs and return the result
        val coopSpaceDtos = this.coopSpaceMapper.mapToDtos(coopSpacesWithUserAccess)
        return ResponseEntity.ok(coopSpaceDtos)
    }

    @GetMapping("/companies")
    fun getValidCompanyNames(): ResponseEntity<List<String>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val userGroups = jwtAuthenticationToken.token.claims["usergroup"] as List<String>
        val companyNames = userGroups.map { it.split("/")[1] }
        return ResponseEntity.ok(companyNames.distinct())
    }

    @GetMapping("{id}")
    fun getCoopSpace(@PathVariable id: Long): ResponseEntity<CoopSpaceDto> {
        return ResponseEntity.ok(this.coopSpaceMapper.map(this.coopSpaceService.findCoopSpace(id)))
    }

    @GetMapping("/members")
    fun getKeycloakUsers(): ResponseEntity<List<MemberDto>> {
        // TODO: Kommentar von EBE.
        // Arbeitsstand / Versuch Keycloak anzusprechen
        // this.keycloakService.getUserResource("0e68593d-6604-4e7a-aa53-15b1af988c2d")

        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val members = this.keycloakService.getKeycloakUsers().filter {
            it.username != jwtAuthenticationToken.token.claims["preferred_username"]
        }

        val memberDtos = this.memberMapper.mapToDtos(members)
        return ResponseEntity.ok(memberDtos)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val creator =
            this.keycloakService.findKeycloakUserByMail(jwtAuthenticationToken.token.claims["email"] as String)
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        this.coopSpaceService.createCoopSpace(coopSpace, creator)
    }

    @PostMapping("delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        this.coopSpaceService.deleteCoopSpace(jwt, coopSpace)
    }

    @PostMapping("/deleteMember")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeUserFromCoopSpace(@RequestBody deleteMemberRequest: DeleteMemberRequest) {
        this.coopSpaceService.removeUserFromCoopSpace(
            deleteMemberRequest.username,
            deleteMemberRequest.role,
            deleteMemberRequest.coopSpaceName,
            deleteMemberRequest.companyName
        )
    }

    @GetMapping("{id}/assets")
    fun getAssetsForCoopSpace(@PathVariable id: Long): ResponseEntity<Any> {
        val coopSpace = this.coopSpaceService.findCoopSpace(id)
        val company = coopSpace.company?.lowercase()
        val bucketName = coopSpace.name!!
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        return try {
            val assetsForBucket =
                this.minioService.getAssetsForCoopspace(jwt, company!!, bucketName).map { it.get() }.map {
                    AssetDto(
                        it.etag(),
                        it.objectName(),
                        it.lastModified().toString(),
                        it.lastModified().toString(),
                        "${it.size()}MB",
                        "label",
                        bucketName
                    )
                }
            ResponseEntity.ok(assetsForBucket)
        } catch (e: Exception) {
            ResponseEntity.noContent().build()
        }
    }
}

