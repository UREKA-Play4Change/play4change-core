package com.ureka.play4change.infra.pipeline.interceptors

import io.micrometer.core.instrument.MeterRegistry
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────────────────
//  LoggingInterceptor
//
//  Logs every request with method, path, status code, and duration.
//  Also records a Prometheus histogram for request duration — this is what
//  feeds your Grafana dashboards with API latency data.
//
//  Runs AFTER the RequestIdFilter so every log line here has the request ID
//  from MDC automatically (configured in logback.xml).
//
// ─────────────────────────────────────────────────────────────────────────────
@Component
class LoggingInterceptor(
    private val meterRegistry: MeterRegistry
) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(javaClass)

    // Store timer start in request attribute — survives through to afterCompletion
    private val TIMER_ATTR = "request.timer.start"

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        request.setAttribute(TIMER_ATTR, System.currentTimeMillis())
        log.debug("--> {} {}", request.method, request.requestURI)
        return true // true = continue processing, false = stop here
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startMs = request.getAttribute(TIMER_ATTR) as? Long ?: return
        val durationMs = System.currentTimeMillis() - startMs
        val status = response.status
        val method = request.method
        val path = request.requestURI

        // Normalise path for metrics — replace IDs with {id} placeholder
        // so Prometheus doesn't create a new metric per unique UUID
        val normalisedPath = normalisePath(path)

        if (ex != null) {
            log.error("<-- {} {} → {} [{}ms] ERROR: {}", method, path, status, durationMs, ex.message)
        } else {
            log.info("<-- {} {} → {} [{}ms]", method, path, status, durationMs)
        }

        // Prometheus histogram — feeds Grafana "API latency by endpoint" panel
        meterRegistry.timer(
            "http.server.requests",
            "method", method,
            "uri", normalisedPath,
            "status", status.toString()
        ).record(durationMs, TimeUnit.MILLISECONDS)
    }

    // Replace UUIDs and numeric IDs in paths so Prometheus cardinality stays low
    private fun normalisePath(path: String): String {
        return path
            .replace(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"), "{id}")
            .replace(Regex("/\\d+"), "/{id}")
    }
}