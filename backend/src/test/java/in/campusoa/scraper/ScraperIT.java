package in.campusoa.scraper;

import in.campusoa.drive.DriveRepository;
import in.campusoa.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Full scraper run against a fake website and a real database. */
class ScraperIT extends IntegrationTest {

    private static final String ROBOTS = "https://www.cbit.ac.in/robots.txt";
    private static final String PAGE = "https://www.cbit.ac.in/placement_post/notice-board/";

    @MockitoBean HttpFetcher http;   // replaces the real network client in this test's Spring context
    @Autowired NoticeBoardScraper scraper;
    @Autowired DriveRepository drives;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM scrape_run");
        jdbc.update("DELETE FROM drive WHERE source = 'SCRAPED'");
    }

    @Test
    void importsCircularsThenUsesConditionalGet() throws Exception {
        when(http.get(eq(ROBOTS), anyMap())).thenReturn(new HttpFetcher.Response(200,
                "User-agent: *\nDisallow: /wp-admin/\n", null, null));
        when(http.get(eq(PAGE), eq(Map.of()))).thenReturn(new HttpFetcher.Response(200,
                ScraperUnitTest.fixture(), "\"abc\"", "Fri, 20 Nov 2026 06:00:15 GMT"));

        ScrapeRun first = scraper.run(NoticeBoardScraper.Trigger.MANUAL);
        assertThat(first.getStatus()).isEqualTo(ScrapeRun.Status.SUCCESS);
        assertThat(first.getItemsSeen()).isEqualTo(5);
        assertThat(first.getItemsNew()).isEqualTo(5);

        var ea = drives.findAll().stream().filter(d -> d.getTitle().startsWith("Electronic Arts")).findFirst().orElseThrow();
        assertThat(ea.getCompanyId()).isNotNull();          // matched the seeded company
        var orbit = drives.findAll().stream().filter(d -> d.getTitle().startsWith("Orbit")).findFirst().orElseThrow();
        assertThat(orbit.getCompanyId()).isNull();          // unknown company...
        assertThat(orbit.getCompanyGuess()).isEqualTo("Orbit Rockets");   // ...but a guess for the admin
        assertThat(orbit.getNoticeDate()).hasToString("2026-11-12");

        // Second run: we send the ETag back; the site answers 304 and nothing is re-imported.
        jdbc.update("UPDATE scrape_run SET started_at = started_at - interval '1 hour'");
        when(http.get(eq(PAGE), eq(Map.of("If-None-Match", "\"abc\"", "If-Modified-Since",
                "Fri, 20 Nov 2026 06:00:15 GMT")))).thenReturn(new HttpFetcher.Response(304, "", null, null));
        assertThat(scraper.run(NoticeBoardScraper.Trigger.MANUAL).getStatus()).isEqualTo(ScrapeRun.Status.NOT_MODIFIED);
        assertThat(drives.count()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void scheduledRunIsSkippedWithinSixHours() throws Exception {
        jdbc.update("INSERT INTO scrape_run (started_at, status) VALUES (now() - interval '1 hour', 'SUCCESS')");
        assertThat(scraper.run(NoticeBoardScraper.Trigger.SCHEDULE)).isNull();
        verify(http, never()).get(eq(PAGE), anyMap());
    }
}
