package com.ureka.play4change.infrastructure.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.ureka.play4change.auth.adapter.inbound.security.JwtAuthFilter
import com.ureka.play4change.auth.adapter.inbound.web.MessageResponse
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val objectMapper: ObjectMapper,
    private val corsConfigurationSource: CorsConfigurationSource,
    environment: Environment
) {

    // Evaluated once at construction — active profiles are fixed after context refresh.
    private val isProd: Boolean = environment.activeProfiles.contains("prod")

    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { _, response, _ ->
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write(objectMapper.writeValueAsString(MessageResponse("Authentication required")))
        }

    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler =
        AccessDeniedHandler { _, response, _ ->
            response.status = HttpServletResponse.SC_FORBIDDEN
            response.contentType = "application/json"
            response.writer.write(objectMapper.writeValueAsString(MessageResponse("Insufficient role")))
        }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(
                    "/auth/**",
                    "/api/stats/public",
                    "/error",
                    "/actuator/health",
                    "/actuator/prometheus",
                ).permitAll()
                val swaggerPaths = arrayOf("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                if (!isProd) {
                    auth.requestMatchers(*swaggerPaths).permitAll()
                } else {
                    auth.requestMatchers(*swaggerPaths).hasRole("ADMIN")
                }
                auth.requestMatchers("/admin/**").hasRole("ADMIN")
                auth.anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint())
                it.accessDeniedHandler(accessDeniedHandler())
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
