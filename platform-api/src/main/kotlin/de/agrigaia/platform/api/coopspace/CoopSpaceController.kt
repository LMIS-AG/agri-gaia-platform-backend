package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.toEntity
import de.agrigaia.platform.business.coopspace.CoopSpaceService
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.business.keycloak.KeycloakService
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.AddMemberRequest
import de.agrigaia.platform.model.coopspace.ChangeMemberRoleRequest
import de.agrigaia.platform.model.coopspace.CoopSpace
import de.agrigaia.platform.model.coopspace.DeleteMemberRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PostAuthorize
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*


// TODO Parse JWT and look for roles to see if the user has the rights for the coopspaces and buckets (local db and minio)
@RestController
@RequestMapping("/coopspaces")
open class CoopSpaceController @Autowired constructor(
    private val coopSpaceService: CoopSpaceService,
    private val coopSpaceMapper: CoopSpaceMapper,
    private val minioService: MinioService,
    private val keycloakService: KeycloakService,
    private val memberMapper: MemberMapper,
) : HasLogger, BaseController() {

    @GetMapping
    open fun getCoopSpaces(authentication: Authentication): ResponseEntity<List<CoopSpaceDto>> {
        val coopSpaceAuthorities = authentication.authorities.filter { it.authority.startsWith("coopspace-") }
        val coopSpacesWithUserAccess = this.coopSpaceService.getCoopSpacesWithUserAccess(coopSpaceAuthorities)
        val coopSpaceDtos = this.coopSpaceMapper.mapToDtos(coopSpacesWithUserAccess)
        return ResponseEntity.ok(coopSpaceDtos)
    }

    @GetMapping("/companies")
    open fun getValidCompanyNames(authentication: Authentication): ResponseEntity<List<String>> {
        val validCompanyNames: List<String> = authentication.authorities
            .map { it.authority }
            .filter { it.contains("company-") }
            .map { it.removePrefix("company-")}.distinct()
        return ResponseEntity.ok(validCompanyNames)
    }

    // Bisschen haesslich, ich weiss.
    @PostAuthorize("hasAnyAuthority('coopspace-'+returnObject.body.name+'-Guest', 'coopspace-'+returnObject.body.name+'-User', 'coopspace-'+returnObject.body.name+'-Admin')")
    @GetMapping("{id}")
    open fun getCoopSpace(@PathVariable id: Long): ResponseEntity<CoopSpaceDto> {
        val coopSpace: CoopSpace = this.coopSpaceService.findCoopSpace(id)
        val coopSpaceDto: CoopSpaceDto = this.coopSpaceMapper.map(coopSpace)
        return ResponseEntity.ok(coopSpaceDto)
    }

    // TODO
    @GetMapping("{id}/members")
    open fun getMembersOfCoopSpace(@PathVariable id: Long): ResponseEntity<List<MemberDto>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val username = jwtAuthenticationToken.token.claims["preferred_username"] as String
        val coopSpace: CoopSpace = this.coopSpaceService.findCoopSpace(id)

        if (!this.coopSpaceService.hasAccessToCoopSpace(username, coopSpace)) {
            return ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
        return ResponseEntity.ok(this.memberMapper.mapToDtos(coopSpace.members))
    }

    @GetMapping("/members")
    open fun getKeycloakUsers(): ResponseEntity<List<MemberDto>> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val loggedInUserName: String = jwtAuthenticationToken.token.getClaimAsString("preferred_username")
        val members = this.keycloakService.getKeycloakUsers().filter { it.username != loggedInUserName }
        val memberDtos = this.memberMapper.mapToDtos(members)
        return ResponseEntity.ok(memberDtos)
    }

    // TODO
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    open fun createCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto): ResponseEntity<CoopSpaceDto> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val creator =
            this.keycloakService.findKeycloakUserByMail(jwtAuthenticationToken.token.claims["email"] as String)
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        val createdCoopSpace: CoopSpace = this.coopSpaceService.createCoopSpace(coopSpace, creator)
        val createdCoopSpaceDto = this.coopSpaceMapper.map(createdCoopSpace)
        return ResponseEntity.ok(createdCoopSpaceDto)
    }

    // TODO
    @PostMapping("delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    open fun deleteCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        this.coopSpaceService.deleteCoopSpace(jwt, coopSpace)
    }

    // TODO
    @PreAuthorize("hasAuthority(#deleteMemberRequest.coopSpaceName + '-Admin')")
    @PostMapping("/deleteMember")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    open fun removeUserFromCoopSpace(
        @RequestBody deleteMemberRequest: DeleteMemberRequest,
        authentication: Authentication
    ) {
        // remove user from the CoopSpace by removing it both from the respective subgroup in Keycloak and the database
        this.coopSpaceService.removeUserFromKeycloakGroup(
            deleteMemberRequest.member.username!!,
            deleteMemberRequest.member.role!!.toString(),
            deleteMemberRequest.member.company!!,
            deleteMemberRequest.coopSpaceName
        )
        this.coopSpaceService.removeUserFromDatabase(
            deleteMemberRequest.member.id
        )
    }

    // TODO
    @PostMapping("/addMember")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    open fun addUserToCoopSpace(@RequestBody addMemberRequest: AddMemberRequest) {
        val coopSpaceDto = this.coopSpaceMapper.map(this.coopSpaceService.findCoopSpace(addMemberRequest.coopSpaceId))
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        val coopSpaceName = coopSpace.name ?: throw BusinessException("CoopSpaceName is null", ErrorType.NOT_FOUND)

        // add user to the CoopSpace by adding it both to the respective subgroup in Keycloak and the database
        this.coopSpaceService.addUsersToKeycloakGroup(
            addMemberRequest.member,
            coopSpaceName
        )
        this.coopSpaceService.addUsersToDatabase(
            addMemberRequest.member,
            coopSpace
        )
    }

    // TODO
    @PostMapping("/changeMemberRole")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    open fun changeMemberRoleInCoopSpace(@RequestBody changeMemberRoleRequest: ChangeMemberRoleRequest) {
        val coopSpaceDto =
            this.coopSpaceMapper.map(this.coopSpaceService.findCoopSpace(changeMemberRoleRequest.coopSpaceId))
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        val coopSpaceName = coopSpace.name ?: throw BusinessException("CoopSpaceName is null", ErrorType.NOT_FOUND)

        // change role of the user by removing it from its respective Keycloak subgroup (e.g. "...-User"), adding it to another subgroup (e.g. "...-Admin") and
        // update his role in the database by updating the CoopSpace

        this.coopSpaceService.removeUserFromKeycloakGroup(
            changeMemberRoleRequest.member.username!!,
            changeMemberRoleRequest.originalRole,
            changeMemberRoleRequest.member.company!!,
            coopSpaceName,
        )
        this.coopSpaceService.addUserToKeycloakGroup(
            changeMemberRoleRequest.member,
            coopSpaceName
        )
        this.coopSpaceService.changeUserRoleInDatabase(
            changeMemberRoleRequest.member,
            coopSpace
        )

    }

    // TODO
    @GetMapping("{id}/assets")
    open fun getAssetsForCoopSpace(@PathVariable id: Long): ResponseEntity<Any> {
        val coopSpace = this.coopSpaceService.findCoopSpace(id)
        val company = coopSpace.company?.lowercase()
        val bucketName = coopSpace.name!!
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        return try {
            val assetsForBucket =
                this.minioService.getAssetsForCoopspace(jwt, company!!, bucketName).map { it.get() }.map {
                    AssetDto(
                        it.objectName().replace("assets/", ""),
                        it.lastModified().toString(),
                        it.lastModified().toString(),
                        it.size().toString(),
                        "label",
                        bucketName,
                        isPublished = false
                    )
                }
            ResponseEntity.ok(assetsForBucket)
        } catch (e: Exception) {
            ResponseEntity.noContent().build()
        }
    }

    // TODO
    @GetMapping("existsbyname/{name}")
    open fun checkIfCoopSpaceAlreadyExistsByName(@PathVariable name: String): ResponseEntity<Boolean> {
        return ResponseEntity.ok(this.minioService.bucketExists(name))
    }
}

