package com.ureka.play4change.auth.adapter.inbound.security

import com.ureka.play4change.auth.application.TokenService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(private val tokenService: TokenService) : OncePerRequestFilter() {

    companion object {
        private val PUBLIC_PREFIXES = listOf(
            "/auth/",
            "/actuator/health",
            "/actuator/prometheus",
            "/swagger-ui/",
            "/swagger-ui.html",
            "/v3/api-docs/",
            "/error"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Public routes: skip token parsing entirely — no 401, auth context untouched
        val path = request.requestURI
        if (PUBLIC_PREFIXES.any { path == it || path.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            try {
                val (userId, role) = tokenService.parseAccessToken(token)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_$role"))
                    )
            } catch (ex: Exception) {
                // Token present on a protected route but invalid — reject immediately
                SecurityContextHolder.clearContext()
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token")
                return
            }
        }
        // No token on a protected route: let downstream Spring Security handle it (no 401 here)
        filterChain.doFilter(request, response)
    }
}
