package in.campusoa.scraper;

import in.campusoa.company.CompanyService;
import in.campusoa.config.AppProperties;
import in.campusoa.drive.DriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Imports placement circulars from the public CDC notice board. Politeness rules:
 * <ol>
 *   <li><b>Identify:</b> a descriptive User-Agent (SCRAPER_USER_AGENT).</li>
 *   <li><b>Obey robots.txt:</b> fetched at most once a day; if our path is disallowed we stop.</li>
 *   <li><b>Be rare:</b> scheduled every 6 hours, and never more than once per {@code min-interval}
 *       (manual runs: once per 10 minutes).</li>
 *   <li><b>Use caching:</b> If-None-Match / If-Modified-Since. A "304 Not Modified" costs the
 *       server almost nothing, and we stop there.</li>
 *   <li><b>One request:</b> a single listing page; we never download the PDFs.</li>
 * </ol>
 */
@Service
public class NoticeBoardScraper {

    private static final Logger log = LoggerFactory.getLogger(NoticeBoardScraper.class);
    private static final Duration ROBOTS_TTL = Duration.ofHours(24);
    private static final Duration MANUAL_MIN_GAP = Duration.ofMinutes(10);
    private static final String BOT_TOKEN = "CampusOABot";

    private final HttpFetcher http;
    private final ScrapeRunRepository runs;
    private final DriveService drives;
    private final CompanyService companies;
    private final AppProperties.Scraper cfg;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private RobotsTxt robots;
    private Instant robotsFetchedAt;

    public NoticeBoardScraper(HttpFetcher http, ScrapeRunRepository runs, DriveService drives,
                              CompanyService companies, AppProperties props, Clock clock) {
        this.http = http;
        this.runs = runs;
        this.drives = drives;
        this.companies = companies;
        this.cfg = props.scraper();
        this.clock = clock;
    }

    public enum Trigger { SCHEDULE, STARTUP, MANUAL }

    /** @return the run record, or null if skipped because of the interval rules or a run in progress. */
    public ScrapeRun run(Trigger trigger) {
        if (!running.compareAndSet(false, true)) {
            log.info("Scrape skipped: already running");
            return null;
        }
        try {
            if (!dueFor(trigger)) return null;
            return doRun();
        } finally {
            running.set(false);
        }
    }

    boolean dueFor(Trigger trigger) {
        Instant now = Instant.now(clock);
        if (trigger == Trigger.MANUAL) {
            return runs.findFirstByOrderByStartedAtDesc()
                    .map(r -> r.getStartedAt().isBefore(now.minus(MANUAL_MIN_GAP))).orElse(true);
        }
        // Scheduled/startup: skip if the page was fetched successfully within min-interval.
        // (5 minutes of slack so a cron run isn't skipped because the last one took 2 seconds.)
        return lastFetch().map(r -> r.getStartedAt().isBefore(now.minus(cfg.minInterval()).plusSeconds(300)))
                .orElse(true);
    }

    private java.util.Optional<ScrapeRun> lastFetch() {
        return runs.findFirstByStatusInOrderByStartedAtDesc(List.of(ScrapeRun.Status.SUCCESS, ScrapeRun.Status.NOT_MODIFIED));
    }

    private ScrapeRun doRun() {
        ScrapeRun run = runs.save(new ScrapeRun(Instant.now(clock)));
        try {
            if (!robotsAllow()) {
                run.finish(ScrapeRun.Status.SKIPPED_ROBOTS, null, Instant.now(clock));
                log.warn("robots.txt disallows {}; not scraping", cfg.url());
                return runs.save(run);
            }

            Map<String, String> headers = new HashMap<>();
            lastFetch().ifPresent(prev -> {
                if (prev.getEtag() != null) headers.put("If-None-Match", prev.getEtag());
                if (prev.getLastModified() != null) headers.put("If-Modified-Since", prev.getLastModified());
            });

            HttpFetcher.Response res = http.get(cfg.url(), headers);
            if (res.status() == 304) {
                run.setCacheValidators(headersOr(headers, "If-None-Match"), headersOr(headers, "If-Modified-Since"));
                run.finish(ScrapeRun.Status.NOT_MODIFIED, 304, Instant.now(clock));
                return runs.save(run);
            }
            if (res.status() != 200) {
                run.setError("Unexpected HTTP " + res.status());
                run.finish(ScrapeRun.Status.FAILED, res.status(), Instant.now(clock));
                return runs.save(run);
            }

            List<NoticeBoardParser.Notice> notices = NoticeBoardParser.parse(res.body(), cfg.url());
            Map<String, Long> names = companies.namesAndAliasesLongestFirst();
            LocalDate today = LocalDate.now(clock);
            int created = 0;
            for (var n : notices) {
                var scraped = new DriveService.ScrapedDrive(
                        n.title(),
                        TitleAnalyzer.date(n.title()).orElse(today),
                        n.pdfUrl(),
                        TitleAnalyzer.matchCompany(n.title(), names).orElse(null),
                        TitleAnalyzer.guessCompany(n.title()),
                        TitleAnalyzer.stages(n.title()));
                if (drives.upsertScraped(scraped)) created++;
            }
            run.setCounts(notices.size(), created);
            run.setCacheValidators(res.etag(), res.lastModified());
            run.finish(ScrapeRun.Status.SUCCESS, 200, Instant.now(clock));
            log.info("Scrape done: {} circulars seen, {} new", notices.size(), created);
            return runs.save(run);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fail(run, e);
        } catch (Exception e) {
            return fail(run, e);
        }
    }

    private ScrapeRun fail(ScrapeRun run, Exception e) {
        log.warn("Scrape failed: {}", e.toString());
        run.setError(e.getClass().getSimpleName() + ": " + e.getMessage());
        run.finish(ScrapeRun.Status.FAILED, null, Instant.now(clock));
        return runs.save(run);
    }

    private boolean robotsAllow() throws Exception {
        Instant now = Instant.now(clock);
        if (robots == null || robotsFetchedAt.isBefore(now.minus(ROBOTS_TTL))) {
            HttpFetcher.Response r = http.get(cfg.robotsUrl(), Map.of());
            if (r.status() == 200) robots = RobotsTxt.parse(r.body());
            else if (r.status() >= 400 && r.status() < 500) robots = RobotsTxt.allowAll();   // RFC 9309: 4xx = no rules
            else throw new IllegalStateException("robots.txt returned HTTP " + r.status());  // 5xx: assume disallowed
            robotsFetchedAt = now;
        }
        return robots.isAllowed(BOT_TOKEN, URI.create(cfg.url()).getPath());
    }

    private static String headersOr(Map<String, String> headers, String key) {
        return headers.get(key);
    }
}
