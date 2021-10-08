package de.agrigaia.platform.api.config

import de.agrigaia.platform.common.ApplicationProperties
import de.agrigaia.platform.common.HasLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WebSecurityConfig @Autowired constructor(private val applicationProperties: ApplicationProperties) :
    WebSecurityConfigurerAdapter(), WebMvcConfigurer, HasLogger {

    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
            .cors()
            .and()

            .authorizeRequests().anyRequest().permitAll()
            .and()

            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
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
}