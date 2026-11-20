package in.campusoa.moderation;

import in.campusoa.common.ContentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ModerationDtos {

    private ModerationDtos() {}

    public enum Reason { SPAM, CONFIDENTIAL, PERSONAL_INFO, ABUSIVE, OFF_TOPIC, OTHER }

    /** What a moderator decides. DISMISS = report was wrong; RESTORE = un-hide content. */
    public enum Decision { DISMISS, HIDE, REMOVE, RESTORE }

    public record ReportRequest(@NotNull ContentTarget targetType, @NotNull @Positive Long targetId,
                                @NotNull Reason reason, @Size(max = 500) String details) {}

    public record ResolveRequest(@NotNull Decision decision, @Size(max = 500) String note) {}

    public record QueueItemDto(long reportId, ContentTarget targetType, long targetId, Long experienceId,
                               String preview, ContentStatus targetStatus, Reason reason, String details,
                               String reporterName, long openReportsForTarget, Instant createdAt) {}

    public record ActionDto(long id, String moderatorName, String action, ContentTarget targetType, Long targetId,
                            String note, Instant createdAt) {}
}
