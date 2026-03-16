package com.ureka.play4change.infra.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    @Profile("demo", "default")
    fun demoSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }          // stateless API, no CSRF needed
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests {
                it.anyRequest().permitAll() // all endpoints open for demo
            }
        return http.build()
    }

    @Bean
    @Profile("prod")
    fun prodSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Health and metrics — open (Prometheus scrapes these)
                    .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                    // Admin endpoints — require ADMIN role
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    // Everything else — require valid JWT
                    .anyRequest().authenticated()
            }
            // JWT validation — Spring reads Authorization: Bearer <token>
            // and validates signature + expiry automatically
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    // userId = jwt.subject claim (set in JwtAuthConverter if needed)
                }
            }
        return http.build()
    }
}