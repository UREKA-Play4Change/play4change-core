package com.ureka.play4change.auth.adapter.inbound.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.ureka.play4change.auth.adapter.inbound.web.MessageResponse
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RateLimitFilter(
    private val rateLimitService: RateLimitService,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val path = request.requestURI
        if (!path.startsWith("/auth/")) {
            chain.doFilter(request, response)
            return
        }

        val clientIp = IpExtractor.extractClientIp(request)
        if (!rateLimitService.tryConsume(clientIp, path)) {
            val retryAfter = rateLimitService.retryAfterSeconds(clientIp, path)
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.setIntHeader("Retry-After", retryAfter.toInt())
            response.writer.write(
                objectMapper.writeValueAsString(
                    MessageResponse("Too many requests. Please wait before trying again.")
                )
            )
            return
        }

        chain.doFilter(request, response)
    }
}
