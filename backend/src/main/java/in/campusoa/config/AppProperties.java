package in.campusoa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * Type-safe view of the {@code app.*} block in application.yml.
 * Spring Boot turns comma-separated env vars (e.g. {@code ALLOWED_EMAIL_DOMAINS=a.edu,b.edu}) into lists.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        List<String> allowedEmailDomains,
        List<String> adminEmails,
        String publicUrl,
        String frontendOrigin,
        ZoneId timezone,
        RateLimit rateLimit,
        Moderation moderation,
        Scraper scraper) {

    public AppProperties {
        allowedEmailDomains = normalise(allowedEmailDomains);
        adminEmails = normalise(adminEmails);
        if (timezone == null) timezone = ZoneId.of("Asia/Kolkata");
        if (rateLimit == null) rateLimit = new RateLimit(30);
        if (moderation == null) moderation = new Moderation(3);
    }

    private static List<String> normalise(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .filter(v -> !v.isEmpty())
                .toList();
    }

    public record RateLimit(int writesPerMinute) {}

    public record Moderation(int autoHideReportThreshold) {}

    public record Scraper(boolean enabled, String url, String robotsUrl, String userAgent,
                          String cron, Duration minInterval, Duration timeout) {}
}
