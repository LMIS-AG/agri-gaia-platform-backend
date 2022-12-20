package de.agrigaia.platform.api.config

import de.agrigaia.platform.common.ApplicationProperties
import de.agrigaia.platform.common.HasLogger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.util.*

@Configuration
open class WebSecurityConfig @Autowired constructor(private val applicationProperties: ApplicationProperties) : WebMvcConfigurer, HasLogger {

private val log = getLogger()
    @Bean
    open fun filterChain(http: HttpSecurity): SecurityFilterChain {

        this.log.debug("Start filterChain")

        http.csrf().disable()
            .cors()
            .and()

            .authorizeRequests()
            .anyRequest().authenticated()
            .and()

            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            .and()
            .oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(this::extractAuthorities)

        this.log.debug("End filterChain")

        return http.build()
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedMethods(
                HttpMethod.GET.name,
                HttpMethod.POST.name,
                HttpMethod.DELETE.name,
                HttpMethod.PUT.name
            )
            .allowedOrigins(*this.applicationProperties.allowedOrigins)
            .exposedHeaders(HttpHeaders.LOCATION)
    }

    /**
     * Extracts the groups from the JWT. Hierarchical subgroups are currently seperated by an underscore
     */
    open fun extractAuthorities(jwt: Jwt): AbstractAuthenticationToken {

        this.log.debug("Trying to extract authorities")

        /*val groups: List<String> = jwt.getClaim("groups")
        val authorities = groups
            .map { role: String -> "GROUP_" + role.removeRange(0,1).replace("/", "_").uppercase(Locale.getDefault()) }
            .map { role: String? -> SimpleGrantedAuthority(role) }
            .toList()*/
        this.log.debug("DONE extracting authorities; authorities = hardcoded null")
        val authorities = null // TODO adjust when keycloak roles and groups are configured

        //this.log.debug("DONE extracting authorities")

        return JwtAuthenticationToken(jwt, authorities)
    }
}