package in.campusoa.prep;

import in.campusoa.auth.AppUserPrincipal;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.prep.PrepDtos.DashboardDto;
import in.campusoa.prep.PrepDtos.LeaderboardDto;
import in.campusoa.prep.PrepDtos.ProblemDto;
import in.campusoa.prep.PrepDtos.ProblemRequest;
import in.campusoa.prep.PrepDtos.RoadmapSectionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Everything here needs sign-in (the "/api/**" rule) and only ever touches the caller's own data. */
@RestController
@Tag(name = "Prep tracker & leaderboard")
public class PrepController {

    private final PrepService service;

    public PrepController(PrepService service) {
        this.service = service;
    }

    public record TickRequest(boolean done) {}

    @GetMapping("/api/prep/roadmap")
    public List<RoadmapSectionDto> roadmap(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.roadmap(me.userId());
    }

    @PutMapping("/api/prep/roadmap/{itemId}")
    @Operation(summary = "Tick (done=true) or untick a roadmap item")
    public ResponseEntity<Void> tick(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long itemId,
                                     @RequestBody TickRequest body) {
        service.tick(me.userId(), itemId, body.done());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/prep/problems")
    public PageResponse<ProblemDto> problems(@AuthenticationPrincipal AppUserPrincipal me,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.problems(me.userId(), new PageParams(page, size));
    }

    @PostMapping("/api/prep/problems")
    public ResponseEntity<ProblemDto> addProblem(@AuthenticationPrincipal AppUserPrincipal me,
                                                 @Valid @RequestBody ProblemRequest body) {
        return ResponseEntity.status(201).body(service.addProblem(me.userId(), body));
    }

    @DeleteMapping("/api/prep/problems/{id}")
    public ResponseEntity<Void> deleteProblem(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        service.deleteProblem(me.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/prep/dashboard")
    @Operation(summary = "Streak, totals, problems per week (12 weeks), DSA topic coverage, roadmap progress")
    public DashboardDto dashboard(@AuthenticationPrincipal AppUserPrincipal me) {
        return service.dashboard(me.userId());
    }

    @GetMapping("/api/leaderboard")
    @Operation(summary = "Ranked opted-in users by problems solved (period=WEEK|MONTH|ALL, scope=OVERALL|COHORT)")
    public LeaderboardDto leaderboard(@AuthenticationPrincipal AppUserPrincipal me,
                                      @RequestParam(defaultValue = "WEEK") PrepDtos.Period period,
                                      @RequestParam(defaultValue = "OVERALL") PrepDtos.Scope scope,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return service.leaderboard(me.userId(), period, scope, new PageParams(page, size));
    }
}
