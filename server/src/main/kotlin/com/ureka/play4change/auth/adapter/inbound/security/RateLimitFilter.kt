package com.ureka.play4change.auth.adapter.inbound.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.ureka.play4change.auth.adapter.inbound.web.MessageResponse
import io.micrometer.core.instrument.MeterRegistry
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
    private val meterRegistry: MeterRegistry,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val path = request.requestURI
        val clientIp = IpExtractor.extractClientIp(request)
        if (!rateLimitService.tryConsume(clientIp, path)) {
            val retryAfter = rateLimitService.retryAfterSeconds(clientIp, path)
            meterRegistry.counter("rate_limit_exceeded_total", "path_prefix", pathPrefix(path)).increment()
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

    private fun pathPrefix(path: String): String = when {
        path.startsWith("/auth/") -> "auth"
        path.startsWith("/topics/") -> "topics"
        path.startsWith("/reviews/") -> "reviews"
        path.startsWith("/enrollments/") -> "enrollments"
        else -> "other"
    }
}
