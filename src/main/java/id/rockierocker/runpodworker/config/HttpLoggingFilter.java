package id.rockierocker.runpodworker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter yang mencatat log setiap HTTP request dan response yang masuk.
 * Menggunakan {@link ContentCachingRequestWrapper} dan {@link ContentCachingResponseWrapper}
 * agar body bisa dibaca lebih dari sekali tanpa merusak stream.
 */
@Slf4j
@Component
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final int MAX_BODY_LOG_SIZE = 10_000; // karakter

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper  wrappedRequest  = new ContentCachingRequestWrapper(request, MAX_BODY_LOG_SIZE);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(wrappedRequest);
            logResponse(wrappedRequest, wrappedResponse, duration);
            // wajib dipanggil agar response body tetap dikirim ke client
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);

        log.info("\n>>> INCOMING REQUEST" +
                "\n    Method  : {}" +
                "\n    URI     : {}" +
                "\n    Query   : {}" +
                "\n    Headers : Accept={}, Content-Type={}" +
                "\n    Body    : {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                request.getHeader("Accept"),
                request.getHeader("Content-Type"),
                truncate(body)
        );
    }

    private void logResponse(ContentCachingRequestWrapper request,
                              ContentCachingResponseWrapper response,
                              long duration) {
        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);

        log.info("\n<<< OUTGOING RESPONSE" +
                "\n    URI        : {}" +
                "\n    Status     : {}" +
                "\n    Execute-In : {} ms" +
                "\n    Body       : {}",
                request.getRequestURI(),
                response.getStatus(),
                duration,
                truncate(body)
        );
    }

    private String truncate(String body) {
        if (body == null || body.isBlank()) return "(empty)";
        return body.length() > MAX_BODY_LOG_SIZE
                ? body.substring(0, MAX_BODY_LOG_SIZE) + "... [TRUNCATED]"
                : body;
    }
}

