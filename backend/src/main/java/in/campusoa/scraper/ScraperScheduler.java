package in.campusoa.scraper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Only exists when SCRAPER_ENABLED=true (production). Two triggers:
 * <ul>
 *   <li>cron every 6 hours</li>
 *   <li>on startup, because Render's free tier sleeps after 15 idle minutes, so a cron tick
 *       may have been missed while asleep. {@code dueFor} prevents double runs.</li>
 * </ul>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scraper.enabled", havingValue = "true")
public class ScraperScheduler {

    private final NoticeBoardScraper scraper;

    public ScraperScheduler(NoticeBoardScraper scraper) {
        this.scraper = scraper;
    }

    @Scheduled(cron = "${app.scraper.cron}", zone = "${app.timezone}")
    public void scheduled() {
        scraper.run(NoticeBoardScraper.Trigger.SCHEDULE);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        // Run in the background so startup (and Render's health check) isn't delayed.
        Thread.ofVirtual().name("scraper-startup").start(() -> scraper.run(NoticeBoardScraper.Trigger.STARTUP));
    }
}
