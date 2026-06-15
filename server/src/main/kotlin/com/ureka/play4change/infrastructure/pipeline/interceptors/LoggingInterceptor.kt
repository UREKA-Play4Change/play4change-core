package com.ureka.play4change.infrastructure.pipeline.interceptors

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class LoggingInterceptor : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        request.setAttribute(TIMER_ATTRIBUTE, System.currentTimeMillis())
        log.debug("--> {} {}", request.method, request.requestURI)
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startMs = request.getAttribute(TIMER_ATTRIBUTE) as? Long ?: return
        val durationMs = System.currentTimeMillis() - startMs
        val status = response.status
        val method = request.method
        val path = request.requestURI

        if (ex != null) {
            log.error("<-- {} {} → {} [{}ms] ERROR: {}", method, path, status, durationMs, ex.message)
        } else {
            log.info("<-- {} {} → {} [{}ms]", method, path, status, durationMs)
        }
        // HTTP metrics are recorded automatically by Spring Boot's WebMvcMetricsFilter
        // (http_server_requests_seconds_* family). Do NOT duplicate them here — having
        // two registrations with different tag-key sets causes a Prometheus label
        // inconsistency exception and corrupts the entire metric family.
    }

    private companion object {
        const val TIMER_ATTRIBUTE = "request.timer.start"
    }
}
