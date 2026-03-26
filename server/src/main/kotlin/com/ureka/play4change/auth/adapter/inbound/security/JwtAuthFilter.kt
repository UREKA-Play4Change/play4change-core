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

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            try {
                val userId = tokenService.parseUserIdFromAccessToken(token)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER"))
                    )
            } catch (ex: Exception) {
                SecurityContextHolder.clearContext()
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token")
                return
            }
        }
        filterChain.doFilter(request, response)
    }
}
