package dev.hendrikhoemberg.witchfirerandomizer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that logs incoming non-static HTTP requests with method, URI, query parameters,
 * status code, response time, and client IP.
 *
 * Static assets (/images/**, /css/**, /favicon.ico, /robots.txt) are skipped to prevent log bloat.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        return path.startsWith("/css/")
                || path.startsWith("/images/")
                || path.equals("/favicon.ico")
                || path.equals("/robots.txt");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String fullPath = (query != null && !query.isBlank()) ? uri + "?" + query : uri;
            String remoteIp = request.getRemoteAddr();

            String logMsg = String.format("[Request] %s %s %d (%dms) [%s]",
                    method, fullPath, status, duration, remoteIp != null ? remoteIp : "unknown");

            if (status >= 500) {
                logger.error(logMsg);
            } else if (status >= 400) {
                logger.warn(logMsg);
            } else {
                logger.info(logMsg);
            }
        }
    }
}
