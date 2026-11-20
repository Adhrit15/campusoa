package in.campusoa.question;

import in.campusoa.user.UserDtos.AuthorDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class QuestionDtos {

    private QuestionDtos() {}

    public record TopicDto(String code, String name, String parentCode) {}

    public record CompanyRef(long id, String slug, String name) {}

    public record QuestionDto(long id, String bodyMd, Question.Difficulty difficulty, List<TopicDto> topics,
                              CompanyRef company, Long experienceId, AuthorDto author, boolean anonymous,
                              long votes, boolean votedByMe, String status, Instant createdAt) {}

    /** Used both standalone (POST /api/questions) and inline when posting an experience. */
    public record QuestionRequest(
            @NotBlank @Size(min = 10, max = 5000) String bodyMd,
            @NotNull Question.Difficulty difficulty,
            @NotEmpty @Size(max = 5) List<@NotBlank String> topicCodes,
            Long companyId) {}
}
