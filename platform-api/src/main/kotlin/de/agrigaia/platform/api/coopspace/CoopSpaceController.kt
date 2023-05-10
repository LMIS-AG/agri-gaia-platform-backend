package de.agrigaia.platform.api.coopspace

import de.agrigaia.platform.api.BaseController
import de.agrigaia.platform.api.toEntity
import de.agrigaia.platform.business.coopspace.CoopSpaceService
import de.agrigaia.platform.business.errors.BusinessException
import de.agrigaia.platform.business.errors.ErrorType
import de.agrigaia.platform.business.keycloak.KeycloakService
import de.agrigaia.platform.common.HasLogger
import de.agrigaia.platform.integration.minio.MinioService
import de.agrigaia.platform.model.coopspace.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import java.util.*


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
            .map { it.removePrefix("company-") }.distinct()
        return ResponseEntity.ok(validCompanyNames)
    }

    // Bisschen haesslich, ich weiss.
    @PreAuthorize("hasAnyAuthority('coopspace-'+#coopSpaceName+'-Guest', 'coopspace-'+#coopSpaceName+'-User', 'coopspace-'+#coopSpaceName+'-Admin')")
    @GetMapping("{coopSpaceName}")
    open fun getCoopSpaceByName(@PathVariable coopSpaceName: String): ResponseEntity<CoopSpaceDto> {
        val coopSpace: CoopSpace = this.coopSpaceService.findCoopSpaceByName(coopSpaceName)
        val coopSpaceDto: CoopSpaceDto = this.coopSpaceMapper.map(coopSpace)
        return ResponseEntity.ok(coopSpaceDto)
    }

    @PreAuthorize("hasAnyAuthority('coopspace-'+#coopSpaceName+'-Guest', 'coopspace-'+#coopSpaceName+'-User', 'coopspace-'+#coopSpaceName+'-Admin')")
    @GetMapping("{coopSpaceName}/members")
    open fun getMembersOfCoopSpace(@PathVariable coopSpaceName: String): ResponseEntity<List<MemberDto>> {
        val coopSpace: CoopSpace = this.coopSpaceService.findCoopSpaceByName(coopSpaceName)
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

    @PreAuthorize("hasAuthority('company-'+#coopSpaceDto.company)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    open fun createCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto): ResponseEntity<CoopSpaceDto> {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val creator =
            this.keycloakService.findKeycloakUserByMail(jwtAuthenticationToken.token.getClaimAsString("email"))
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        val createdCoopSpace: CoopSpace = this.coopSpaceService.createCoopSpace(coopSpace, creator)
        val createdCoopSpaceDto: CoopSpaceDto = this.coopSpaceMapper.map(createdCoopSpace)
        return ResponseEntity.ok(createdCoopSpaceDto)
    }

    @PreAuthorize("hasAuthority('coopspace-'+#addMembersDto.coopSpaceName+'-Admin')")
    @PostMapping("/addMembers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    open fun addMembersToCoopSpace(@RequestBody addMembersDto: AddMembersDto) {
        val coopSpaceName =
            addMembersDto.coopSpaceName ?: throw BusinessException("coopSpaceName is null", ErrorType.BAD_REQUEST)
        val memberList =
            addMembersDto.memberList ?: throw BusinessException("memberList is null", ErrorType.BAD_REQUEST)
        // This maps the CoopSpace to a CoopSpaceDto and back to a CoopSpace, i.e. inelegantly creates a copy.
        val coopSpace: CoopSpace = this.coopSpaceMapper.map(this.coopSpaceService.findCoopSpaceByName(coopSpaceName))
            .toEntity(this.coopSpaceMapper)

        // add user to the CoopSpace by adding it both to the respective subgroup in Keycloak and the database
        this.coopSpaceService.addUsersToKeycloakGroup(memberList, coopSpaceName)
        this.coopSpaceService.addUsersToDatabase(memberList, coopSpace)
    }

    /**
    Change member role in coopspace by removing/adding to keycloak groups and updating the database.
     */
    @PreAuthorize("hasAuthority('coopspace-'+#changeMemberRoleDto.coopSpaceName+'-Admin')")
    @PostMapping("/changeMemberRole")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    open fun changeMemberRole(@RequestBody changeMemberRoleDto: ChangeMemberRoleDto) {
        val username =
            changeMemberRoleDto.username ?: throw BusinessException("No username given", ErrorType.BAD_REQUEST)
        val id = changeMemberRoleDto.id ?: throw BusinessException("No member.id given", ErrorType.BAD_REQUEST)
        val oldRole =
            changeMemberRoleDto.oldRole ?: throw BusinessException("No originalRole given", ErrorType.BAD_REQUEST)
        val newRole = changeMemberRoleDto.newRole ?: throw BusinessException("No role given", ErrorType.BAD_REQUEST)
        val coopSpaceName = changeMemberRoleDto.coopSpaceName ?: throw BusinessException(
            "No coopSpaceName given",
            ErrorType.BAD_REQUEST
        )
        val company =
            changeMemberRoleDto.company ?: throw BusinessException("No member.company given", ErrorType.BAD_REQUEST)
        val coopSpace: CoopSpace = this.coopSpaceService.findCoopSpaceByName(coopSpaceName)

        this.coopSpaceService.removeUserFromKeycloakGroup(username, oldRole, company, coopSpaceName)
        this.coopSpaceService.addUserToKeycloakGroup(username, newRole, company, coopSpaceName)
        this.coopSpaceService.changeUserRoleInDatabase(username, newRole, id, coopSpace)

    }

    @PreAuthorize("hasAnyAuthority('coopspace-'+#coopSpaceName+'-Guest', 'coopspace-'+#coopSpaceName+'-User', 'coopspace-'+#coopSpaceName+'-Admin')")
    @GetMapping("{coopSpaceName}/{base64encodedFolderName}")
    open fun getAssetsForCoopSpace(
        @PathVariable coopSpaceName: String,
        @PathVariable base64encodedFolderName: String
    ): ResponseEntity<Any> {
        val coopSpace = this.coopSpaceService.findCoopSpaceByName(coopSpaceName)
        val company = coopSpace.company?.lowercase() ?: throw BusinessException("Company was null", ErrorType.NOT_FOUND)
        val bucketName = coopSpace.name ?: throw BusinessException("BucketName was null", ErrorType.NOT_FOUND)
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val folder = if (base64encodedFolderName == "default") "/" else
            String(Base64.getDecoder().decode(base64encodedFolderName))
        return try {
            val assetsForBucket =
                this.minioService.getAssetsForCoopspace(jwt, company, bucketName, folder).map { it.get() }.map {
                    AssetDto(
                        it.objectName(),
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

    @GetMapping("existsbyname/{name}")
    open fun checkIfCoopSpaceAlreadyExistsByName(@PathVariable name: String): ResponseEntity<Boolean> {
        return ResponseEntity.ok(this.minioService.bucketExists(name))
    }

    // TODO: This should be a @DeleteMapping and only require an id.
    @PreAuthorize("hasAuthority('coopspace-' + #coopSpaceDto.name + '-Admin')")
    @PostMapping("delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    open fun deleteCoopSpace(@RequestBody coopSpaceDto: CoopSpaceDto) {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as JwtAuthenticationToken
        val jwt = jwtAuthenticationToken.token.tokenValue
        val coopSpace: CoopSpace = coopSpaceDto.toEntity(this.coopSpaceMapper)
        this.coopSpaceService.deleteCoopSpace(jwt, coopSpace)
    }

    // TODO: This should be a @DeleteMapping.
    /* Remove user from the CoopSpace by removing it both from the subgroup in Keycloak and the database. */
    @PreAuthorize("hasAuthority('coopspace-' + #deleteMemberDto.coopSpaceName + '-Admin')")
    @PostMapping("/deleteMember")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    open fun removeUserFromCoopSpace(@RequestBody deleteMemberDto: DeleteMemberDto) {
        val username: String =
            deleteMemberDto.member?.username ?: throw BusinessException("Username was null.", ErrorType.NOT_FOUND)
        val role: String = deleteMemberDto.member?.role.toString()
        val company: String =
            deleteMemberDto.member?.company ?: throw BusinessException("Company was null.", ErrorType.NOT_FOUND)
        val coopSpaceName: String =
            deleteMemberDto.coopSpaceName ?: throw BusinessException("CoopSpaceName was null", ErrorType.NOT_FOUND)
        val id: Long = deleteMemberDto.member?.id ?: throw BusinessException("ID was null", ErrorType.NOT_FOUND)

        this.coopSpaceService.removeUserFromKeycloakGroup(username, role, company, coopSpaceName)
        this.coopSpaceService.removeUserFromDatabase(id)
    }
}

