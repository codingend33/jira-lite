package com.jiralite.backend.filter;

import java.io.IOException; // java.io.IOException is the exception thrown by the IOException class
import java.util.UUID; // UUID is a class that represents a universally unique identifier (UUID)
import org.slf4j.MDC; // MDC is a class that represents a mapped diagnostic context
import org.springframework.stereotype.Component; // Component is a stereotype annotation that indicates that the class is a Spring component
import org.springframework.web.filter.OncePerRequestFilter; // OncePerRequestFilter is a filter that is executed once per request

import jakarta.servlet.FilterChain; // FilterChain is a class that represents a filter chain
import jakarta.servlet.ServletException; // ServletException is a class that represents a servlet exception
import jakarta.servlet.http.HttpServletRequest; // HttpServletRequest is a class that represents a HTTP request
import jakarta.servlet.http.HttpServletResponse; // HttpServletResponse is a class that represents a HTTP response

/**
 * Filter to propagate traceId across requests.
 * Reads X-Trace-Id header or generates UUID if missing.
 * Sets response header and puts traceId into MDC.
 */
@Component // Component is a stereotype annotation that indicates that the class is a
           // Spring component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id"; // TRACE_ID_HEADER is a constant that represents the
                                                                // header name for the trace ID
    private static final String MDC_KEY = "traceId"; // MDC_KEY is a constant that represents the key for the trace ID
                                                     // in the MDC

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER); // get the trace ID from the request header
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString(); // generate a new trace ID if the trace ID is not found in the
                                                    // request header
        }

        MDC.put(MDC_KEY, traceId); // put the trace ID into the MDC. all logs in the same request will have the
                                   // same trace ID
        response.setHeader(TRACE_ID_HEADER, traceId); // set the trace ID in the response header. this is used to
                                                      // identify the request in the logs

        try {
            filterChain.doFilter(request, response); // filter the request and response. this is where the actual
                                                     // request processing happens
        } finally {
            MDC.remove(MDC_KEY); // remove the trace ID from the MDC. this is important to avoid memory leaks. if
                                 // the trace ID is not removed, it will stay in the MDC for the entire request.
        }
    }
}
