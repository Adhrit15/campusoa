package in.campusoa.prep;

import in.campusoa.common.ApiException;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.prep.PrepDtos.DashboardDto;
import in.campusoa.prep.PrepDtos.LeaderboardDto;
import in.campusoa.prep.PrepDtos.Period;
import in.campusoa.prep.PrepDtos.ProblemDto;
import in.campusoa.prep.PrepDtos.ProblemRequest;
import in.campusoa.prep.PrepDtos.RoadmapSectionDto;
import in.campusoa.prep.PrepDtos.Scope;
import in.campusoa.question.Topic;
import in.campusoa.question.TopicRepository;
import in.campusoa.user.AppUser;
import in.campusoa.user.AppUserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PrepService {

    private final PrepQueries queries;
    private final LeaderboardQueries leaderboard;
    private final ProblemLogRepository problems;
    private final TopicRepository topics;
    private final AppUserRepository users;
    private final Clock clock;

    public PrepService(PrepQueries queries, LeaderboardQueries leaderboard, ProblemLogRepository problems,
                       TopicRepository topics, AppUserRepository users, Clock clock) {
        this.queries = queries;
        this.leaderboard = leaderboard;
        this.problems = problems;
        this.topics = topics;
        this.users = users;
        this.clock = clock;
    }

    // ---------- roadmap ----------

    @Transactional(readOnly = true)
    public List<RoadmapSectionDto> roadmap(long userId) {
        return queries.roadmap(userId);
    }

    @Transactional
    public void tick(long userId, long itemId, boolean done) {
        if (!queries.roadmapItemExists(itemId)) throw ApiException.notFound("Roadmap item");
        queries.tick(userId, itemId, done);
    }

    // ---------- problem log ----------

    @Transactional(readOnly = true)
    public PageResponse<ProblemDto> problems(long userId, PageParams page) {
        var p = problems.findByUserIdOrderBySolvedOnDescIdDesc(userId, PageRequest.of(page.page(), page.size()));
        Map<Long, Topic> byId = topics.findAll().stream().collect(Collectors.toMap(Topic::getId, Function.identity()));
        return PageResponse.of(p, pl -> toDto(pl, byId.get(pl.getTopicId())));
    }

    @Transactional
    public ProblemDto addProblem(long userId, ProblemRequest req) {
        Topic topic = topics.findByCodeIn(List.of(req.topicCode())).stream().findFirst()
                .orElseThrow(() -> ApiException.badRequest("UNKNOWN_TOPIC", "Unknown topic " + req.topicCode()));
        String url = req.url().trim();
        if (problems.existsByUserIdAndUrl(userId, url)) {
            throw ApiException.conflict("DUPLICATE_PROBLEM", "You've already logged this problem");
        }
        ProblemLog saved = problems.save(new ProblemLog(userId, req.title().trim(), url, topic.getId(),
                req.difficulty(), req.solvedOn()));
        return toDto(saved, topic);
    }

    @Transactional
    public void deleteProblem(long userId, long id) {
        // findByIdAndUserId: you can only ever see or delete YOUR rows (no IDOR bug)
        problems.delete(problems.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Problem")));
    }

    // ---------- dashboard ----------

    @Transactional(readOnly = true)
    public DashboardDto dashboard(long userId) {
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);
        Map<String, Long> t = queries.totals(userId, weekStart, monthStart);
        Streaks.Result streak = Streaks.compute(problems.solvedDaysDesc(userId), today);
        List<RoadmapSectionDto> roadmap = queries.roadmap(userId);
        return new DashboardDto(t.get("total"), t.get("week"), t.get("month"), streak.current(), streak.longest(),
                queries.perWeek(userId, today), queries.topicCoverage(userId), t.get("easy"), t.get("medium"),
                t.get("hard"), roadmap.stream().mapToInt(RoadmapSectionDto::done).sum(),
                roadmap.stream().mapToInt(RoadmapSectionDto::total).sum());
    }

    // ---------- leaderboard ----------

    @Transactional(readOnly = true)
    public LeaderboardDto leaderboard(long viewerId, Period period, Scope scope, PageParams page) {
        AppUser me = users.findById(viewerId).orElseThrow(() -> ApiException.notFound("User"));
        LocalDate today = LocalDate.now(clock);
        LocalDate from = switch (period) {
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> today.withDayOfMonth(1);
            case ALL -> LocalDate.of(2015, 1, 1);
        };
        LocalDate to = switch (period) {          // exclusive upper bound
            case WEEK -> from.plusWeeks(1);
            case MONTH -> from.plusMonths(1);
            case ALL -> today.plusDays(2);
        };
        boolean cohort = scope == Scope.COHORT;
        if (cohort && (me.getBranchCode() == null || me.getGradYear() == null)) {
            throw ApiException.badRequest("PROFILE_INCOMPLETE", "Set your branch and graduation year to see your cohort");
        }
        String branch = cohort ? me.getBranchCode() : null;
        Short year = cohort ? me.getGradYear() : null;
        return new LeaderboardDto(period, scope, from, to.minusDays(1), branch, year,
                leaderboard.page(from, to, cohort, branch, year, page, viewerId),
                leaderboard.count(from, to, cohort, branch, year),
                me.isLeaderboardOptIn() ? leaderboard.myRank(from, to, cohort, viewerId) : null,
                me.isLeaderboardOptIn());
    }

    private static ProblemDto toDto(ProblemLog p, Topic t) {
        return new ProblemDto(p.getId(), p.getTitle(), p.getUrl(), t == null ? null : t.getCode(),
                t == null ? null : t.getName(), p.getDifficulty(), p.getSolvedOn());
    }
}
