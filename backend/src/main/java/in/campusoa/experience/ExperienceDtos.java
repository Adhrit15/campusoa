package in.campusoa.experience;

import in.campusoa.common.ContentStatus;
import in.campusoa.question.QuestionDtos.QuestionDto;
import in.campusoa.question.QuestionDtos.QuestionRequest;
import in.campusoa.user.UserDtos.AuthorDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ExperienceDtos {

    private ExperienceDtos() {}

    public record CompanyRef(long id, String slug, String name) {}

    /**
     * {@code author} is null when the post is anonymous (unless you're its author or a moderator)
     * or when the author deleted their account. {@code anonymous} tells the UI which one.
     */
    public record ExperienceSummaryDto(long id, CompanyRef company, short year, String roleTitle, short difficulty,
                                       InterviewExperience.Outcome outcome, boolean anonymous, AuthorDto author,
                                       String excerpt, long votes, long comments, boolean votedByMe,
                                       Instant createdAt) {}

    public record RoundDto(int position, String name, String description) {}

    public record ExperienceDetailDto(long id, CompanyRef company, short year, String roleTitle, short difficulty,
                                      InterviewExperience.Outcome outcome, boolean anonymous, AuthorDto author,
                                      String bodyMd, List<RoundDto> rounds, List<QuestionDto> questions,
                                      long votes, boolean votedByMe, boolean mine, ContentStatus status,
                                      Instant createdAt, Instant updatedAt) {}

    public record CommentDto(long id, AuthorDto author, String body, boolean mine, Instant createdAt) {}

    // ----- requests -----

    public record RoundRequest(@NotBlank @Size(max = 100) String name, @Size(max = 5000) String description) {}

    public record ExperienceRequest(
            @NotNull @Positive Long companyId,
            @NotNull @Min(2015) @Max(2040) Short year,
            @NotBlank @Size(max = 120) String roleTitle,
            @NotNull @Min(1) @Max(5) Short difficulty,
            @NotNull InterviewExperience.Outcome outcome,
            @NotBlank @Size(min = 50, max = 20000, message = "must be 50–20000 characters") String bodyMd,
            @Size(max = 10) List<@Valid RoundRequest> rounds,
            @Size(max = 15) List<@Valid QuestionRequest> questions,
            boolean anonymous,
            @AssertTrue(message = "you must accept the posting guidelines") boolean acceptGuidelines) {}

    public record CommentRequest(@NotBlank @Size(max = 2000) String body) {}
}
