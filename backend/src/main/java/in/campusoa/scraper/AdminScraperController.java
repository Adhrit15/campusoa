package in.campusoa.scraper;

import in.campusoa.common.ApiException;
import in.campusoa.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/scraper")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: scraper")
public class AdminScraperController {

    private final ScrapeRunRepository runs;
    private final NoticeBoardScraper scraper;

    public AdminScraperController(ScrapeRunRepository runs, NoticeBoardScraper scraper) {
        this.runs = runs;
        this.scraper = scraper;
    }

    public record RunDto(long id, Instant startedAt, Instant finishedAt, ScrapeRun.Status status, Short httpStatus,
                         int itemsSeen, int itemsNew, String error) {
        static RunDto of(ScrapeRun r) {
            return new RunDto(r.getId(), r.getStartedAt(), r.getFinishedAt(), r.getStatus(), r.getHttpStatus(),
                    r.getItemsSeen(), r.getItemsNew(), r.getError());
        }
    }

    @GetMapping("/runs")
    @Operation(summary = "Scraper run history, newest first")
    public PageResponse<RunDto> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(runs.findAllByOrderByStartedAtDesc(PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), 50))), RunDto::of);
    }

    @PostMapping("/run")
    @Operation(summary = "Run the scraper now (at most once per 10 minutes)")
    public RunDto runNow() {
        ScrapeRun r = scraper.run(NoticeBoardScraper.Trigger.MANUAL);
        if (r == null) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "SCRAPE_TOO_SOON",
                    "The scraper ran less than 10 minutes ago (or is running now)");
        }
        return RunDto.of(r);
    }
}
