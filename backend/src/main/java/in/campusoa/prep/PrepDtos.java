package in.campusoa.prep;

import in.campusoa.question.Question;
import in.campusoa.user.UserDtos.AuthorDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class PrepDtos {

    private PrepDtos() {}

    public record RoadmapItemDto(long id, int position, String title, String description, String resourceUrl,
                                 boolean done) {}

    public record RoadmapSectionDto(String code, String name, List<RoadmapItemDto> items, int done, int total) {}

    public record ProblemDto(long id, String title, String url, String topicCode, String topicName,
                             Question.Difficulty difficulty, LocalDate solvedOn) {}

    public record ProblemRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 500) @Pattern(regexp = "^https?://.+", message = "must be a link") String url,
            @NotBlank String topicCode,
            @NotNull Question.Difficulty difficulty,
            @NotNull @PastOrPresent LocalDate solvedOn) {}

    public record WeekCount(LocalDate weekStart, long solved) {}

    public record TopicCount(String code, String name, long solved) {}

    public record DashboardDto(long totalSolved, long solvedThisWeek, long solvedThisMonth, int currentStreak,
                               int longestStreak, List<WeekCount> perWeek, List<TopicCount> topicCoverage,
                               long easy, long medium, long hard, int roadmapDone, int roadmapTotal) {}

    public enum Period { WEEK, MONTH, ALL }

    public enum Scope { OVERALL, COHORT }

    public record LeaderboardEntry(long rank, long denseRank, AuthorDto user, long solved, boolean me) {}

    public record MyRank(long rank, long solved) {}

    public record LeaderboardDto(Period period, Scope scope, LocalDate from, LocalDate to, String cohortBranch,
                                 Short cohortYear, List<LeaderboardEntry> entries, long totalRanked, MyRank me,
                                 boolean optedIn) {}
}
