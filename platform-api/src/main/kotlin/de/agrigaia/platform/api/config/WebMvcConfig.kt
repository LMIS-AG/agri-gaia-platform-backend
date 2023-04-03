package de.agrigaia.platform.api.config

import com.nimbusds.jose.shaded.json.JSONArray
import de.agrigaia.platform.common.ApplicationProperties
import de.agrigaia.platform.common.HasLogger
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
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
//@EnableMethodSecurity
open class WebMvcConfig @Autowired constructor(private val applicationProperties: ApplicationProperties) :
    WebMvcConfigurer, HasLogger {

    private val log = getLogger()

    /*
    Called once on application startup.
     */
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
     * Called with every incoming http request.
     */
    open fun extractAuthorities(jwt: Jwt): AbstractAuthenticationToken {
        this.log.debug("Calling extractAuthorities()")
        val usergroups = jwt.getClaim<JSONArray>("usergroup")
        val authorities = usergroups
            .map { it.toString() }
            .filter { it.contains("Projects") }
            .map { it.substringAfterLast("/") }
            .map { role -> SimpleGrantedAuthority(role) }

        this.log.debug("Authorities:")
        for (authority in authorities) {
            this.log.debug(authority.toString())
        }

        return JwtAuthenticationToken(jwt, authorities)
    }
}