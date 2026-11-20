package in.campusoa.scraper;

import in.campusoa.drive.StageType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class ScraperUnitTest {

    static String fixture() throws IOException {
        try (var in = ScraperUnitTest.class.getResourceAsStream("/scraper/notice-board.html")) {
            return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Nested
    class Parser {
        @Test
        void readsOnlyCircularPdfsUnderTheHeadingAndDedupes() throws IOException {
            var notices = NoticeBoardParser.parse(fixture(), "https://example.ac.in/placement_post/notice-board/");
            assertThat(notices).hasSize(5);   // 6 links, one duplicate; the non-PDF and later sections excluded
            assertThat(notices.get(0).title()).isEqualTo("JPMC Pre-Placement Talk Placement Circular on 20.11.2026");
            assertThat(notices).noneMatch(n -> n.title().contains("Graduation") || n.title().contains("Industry Linkage"));
            assertThat(notices.get(2).title()).contains("12 & 13.11.2026");   // HTML entities decoded
        }

        @Test
        void fallsBackWhenHeadingMissing() {
            String html = "<p><a href='https://x.edu/a.pdf'>Foo Placement Circular on 01.02.2026</a>"
                    + "<a href='https://x.edu/b.pdf'>Holiday list</a></p>";
            assertThat(NoticeBoardParser.parse(html, "https://x.edu/")).extracting(NoticeBoardParser.Notice::pdfUrl)
                    .containsExactly("https://x.edu/a.pdf");
        }
    }

    @Nested
    class Titles {
        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "Infosys Interviews Placement Circular on 11.11.2026|2026-11-11",
                "Orbit Rockets In-Person Interviews Placement Circular on 12 & 13.11.2026.|2026-11-12",
                "Acme Talk Placement Circular on 06, 07 & 08.01.2026|2026-01-06",
                "LTM Technical Interviews Placement Circular on 18.11.2026.|2026-11-18",
                "Old style circular 5-3-2025|2025-03-05",
                "Infosys Interviews Placement Circular on 01 & 03 October-2026|2026-10-01",
                "Fluxon Virtual Interviews Placement Circular on 27 & 28 July. 2026|2026-07-27",
                "A course from 20th-23rd April 2026|2026-04-20"})
        void parsesFirstDate(String title, LocalDate expected) {
            assertThat(TitleAnalyzer.date(title)).contains(expected);
        }

        @Test
        void noDateGivesEmpty() {
            assertThat(TitleAnalyzer.date("Placement circular (date TBA)")).isEmpty();
            assertThat(TitleAnalyzer.date("Bad date on 31.02.2026")).isEmpty();
        }

        @Test
        void detectsStages() {
            assertThat(TitleAnalyzer.stages("Acme Pre-Placement Talk, Online Assessment & Interviews Placement Circular"))
                    .containsExactlyInAnyOrder(StageType.PPT, StageType.OA, StageType.INTERVIEW);
            assertThat(TitleAnalyzer.stages("Foo Group Discussion and HR round")).contains(StageType.GD, StageType.HR);
            assertThat(TitleAnalyzer.stages("Bar Full Drive Placement Circular")).isEmpty();
        }

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
                "Orbit Rockets In-Person Interviews Placement Circular on 20.11.2026|Orbit Rockets",
                "VU- Dynamics Technical Assessment Test-1 Placement Circular|VU- Dynamics",
                "Metrics4 Analytics Pre-Placement Talk, Online Assessment|Metrics4 Analytics",
                "Johnson & Johnson Placement drive Placement Circular on 20.11.2026|Johnson & Johnson"})
        void guessesCompanyFromTitleHead(String title, String expected) {
            assertThat(TitleAnalyzer.guessCompany(title)).isEqualTo(expected);
        }

        @Test
        void matchesLongestKnownNameWithWordBoundaries() {
            Map<String, Long> names = new LinkedHashMap<>();
            names.put("Electronic Arts", 1L);
            names.put("JPMC", 2L);
            names.put("EA", 1L);
            names.put("LTM", 3L);
            assertThat(TitleAnalyzer.matchCompany("Electronic Arts Online Assessment", names)).contains(1L);
            assertThat(TitleAnalyzer.matchCompany("JPMC Pre-Placement Talk", names)).contains(2L);
            assertThat(TitleAnalyzer.matchCompany("LTM Technical Interviews", names)).contains(3L);
            assertThat(TitleAnalyzer.matchCompany("Rearrange Tech Interviews", names)).isEmpty();   // "EA" inside a word
        }
    }

    @Nested
    class Robots {
        private static final String TXT = """
                # comment
                User-agent: *
                Disallow: /wp-admin/
                Allow: /wp-admin/admin-ajax.php

                User-agent: BadBot
                User-agent: CampusOABot
                Disallow: /private/
                Allow: /private/ok$
                """;

        @Test
        void usesSpecificGroupForOurBot() {
            RobotsTxt r = RobotsTxt.parse(TXT);
            assertThat(r.isAllowed("CampusOABot", "/placement_post/notice-board/")).isTrue();
            assertThat(r.isAllowed("CampusOABot", "/private/x")).isFalse();
            assertThat(r.isAllowed("CampusOABot", "/private/ok")).isTrue();       // longer Allow wins
            assertThat(r.isAllowed("CampusOABot", "/wp-admin/")).isTrue();        // our group doesn't list it
        }

        @Test
        void fallsBackToStarGroup() {
            RobotsTxt r = RobotsTxt.parse(TXT);
            assertThat(r.isAllowed("OtherBot", "/wp-admin/options.php")).isFalse();
            assertThat(r.isAllowed("OtherBot", "/wp-admin/admin-ajax.php")).isTrue();
        }

        @Test
        void wildcardAndDisallowAll() {
            assertThat(RobotsTxt.parse("User-agent: *\nDisallow: /").isAllowed("CampusOABot", "/anything")).isFalse();
            assertThat(RobotsTxt.parse("User-agent: *\nDisallow: /*.pdf$").isAllowed("x", "/a/b.pdf")).isFalse();
            assertThat(RobotsTxt.parse("User-agent: *\nDisallow:").isAllowed("x", "/a")).isTrue();
        }
    }
}
