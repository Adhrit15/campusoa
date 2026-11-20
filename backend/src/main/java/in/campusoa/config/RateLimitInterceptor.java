package in.campusoa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.campusoa.common.GlobalExceptionHandler;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token-bucket rate limiting for write requests (POST/PUT/PATCH/DELETE on /api/**).
 * <p>
 * Each user (or IP, if signed out) gets a bucket holding N tokens, refilled at N per minute.
 * Each write spends one token; an empty bucket means HTTP 429 with a Retry-After header. This
 * stops spam bots and accidental loops from flooding the database.
 * <p>
 * Buckets live in memory, which is fine for a single backend instance. With several instances you'd
 * store them in Redis (bucket4j supports that): see "Exercises" in REPORT.md.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int perMinute;
    private final ObjectMapper json;

    public RateLimitInterceptor(AppProperties props, ObjectMapper json) {
        this.perMinute = props.rateLimit().writesPerMinute();
        this.json = json;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (isRead(req.getMethod())) return true;

        if (buckets.size() > MAX_TRACKED_KEYS) buckets.clear();   // crude memory bound
        Bucket bucket = buckets.computeIfAbsent(keyFor(req), k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            res.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }
        long waitSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setHeader("Retry-After", String.valueOf(waitSeconds));
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(res.getOutputStream(), GlobalExceptionHandler.body(HttpStatus.TOO_MANY_REQUESTS,
                "RATE_LIMITED", "Too many requests: try again in " + waitSeconds + "s", req.getRequestURI()));
        return false;
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build();
    }

    private static String keyFor(HttpServletRequest req) {
        // getUserPrincipal() is our AppUserPrincipal's name = user id. Fall back to IP
        // (getRemoteAddr honours X-Forwarded-For because of server.forward-headers-strategy).
        return req.getUserPrincipal() != null ? "u:" + req.getUserPrincipal().getName() : "ip:" + req.getRemoteAddr();
    }

    private static boolean isRead(String method) {
        return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
    }
}
