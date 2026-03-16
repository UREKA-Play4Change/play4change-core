package com.ureka.play4change.infra.pipeline.filters

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
//  RequestIdFilter
//
//  Runs before everything else (Order = 1).
//  Generates a unique ID per request and puts it in the MDC (Mapped Diagnostic
//  Context). Every log line produced during that request automatically includes
//  the request ID — makes tracing a single request through all log output trivial.
//
//  Also adds the ID to the response header so clients can correlate their
//  logs with server logs (useful when debugging with your professor).
//
//  Example log output with this filter:
//  [req-id=a1b2c3] POST /api/v1/tasks/abc/submit 200 45ms
//  [req-id=a1b2c3] Assigned task=xyz to userId=user1
//  [req-id=a1b2c3] Validation passed points=20
// ─────────────────────────────────────────────────────────────────────────────
@Component
@Order(1)
class RequestIdFilter : OncePerRequestFilter() {

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val MDC_KEY = "req-id"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Use client-provided ID if present, otherwise generate one
        val requestId = request.getHeader(REQUEST_ID_HEADER)
            ?: UUID.randomUUID().toString().take(8)

        try {
            MDC.put(MDC_KEY, requestId)
            response.setHeader(REQUEST_ID_HEADER, requestId)
            filterChain.doFilter(request, response)
        } finally {
            // Always clean up MDC — thread pool reuse means stale values leak
            // into unrelated requests if not cleared
            MDC.remove(MDC_KEY)
        }
    }
}