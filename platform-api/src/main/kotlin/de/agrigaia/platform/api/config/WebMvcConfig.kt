package de.agrigaia.platform.api.config

import de.agrigaia.platform.common.ApplicationProperties
import de.agrigaia.platform.common.HasLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableMethodSecurity
open class WebMvcConfig @Autowired constructor(private val applicationProperties: ApplicationProperties) :
    WebMvcConfigurer, HasLogger {

    /**
    Called once on application startup.
     */
    @Bean
    open fun filterChain(http: HttpSecurity): SecurityFilterChain {

        this.getLogger().debug("Start filterChain")

        http.csrf().disable()
            .cors()
            .and()

            .authorizeHttpRequests()
            .anyRequest().authenticated()
            .and()

            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)

            .and()
            .oauth2ResourceServer()
            .jwt()
            .jwtAuthenticationConverter(this::extractAuthorities)

        this.getLogger().debug("End filterChain")

        return http.build()
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedMethods(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.PUT.name()
            )
            .allowedOrigins(*this.applicationProperties.allowedOrigins)
            .exposedHeaders(HttpHeaders.LOCATION)
    }

    /**
     * Extracts the KC groups from the JWT.
     * Called with every incoming http request.
     */
    open fun extractAuthorities(jwt: Jwt): AbstractAuthenticationToken {
        val userGroups = jwt.getClaim<List<String>>("usergroup")

        val companyAuthorities  = userGroups
            .map { it.split("/")[1] }
            .map { role -> SimpleGrantedAuthority("company-$role") }
            .distinct()
        val coopSpaceAuthorities = userGroups
            .filter { it.contains("Projects") }
            .map { it.substringAfterLast("/") }
            .map { role -> SimpleGrantedAuthority("coopspace-$role") }
        val authorities = companyAuthorities + coopSpaceAuthorities

        return JwtAuthenticationToken(jwt, authorities)
    }
}