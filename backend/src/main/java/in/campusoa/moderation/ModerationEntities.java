package in.campusoa.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

final class ModerationEntities {
    private ModerationEntities() {}
}

/** A user's report about one piece of content. Exactly one of the three target ids is set (DB CHECK). */
@Entity
@Table(name = "report")
class Report {

    enum Status { OPEN, RESOLVED, DISMISSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id")
    private Long reporterId;

    @Column(name = "experience_id")
    private Long experienceId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "comment_id")
    private Long commentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModerationDtos.Reason reason;

    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Report() {}

    Report(Long reporterId, ContentTarget target, long targetId, ModerationDtos.Reason reason, String details) {
        this.reporterId = reporterId;
        switch (target) {
            case EXPERIENCE -> this.experienceId = targetId;
            case QUESTION -> this.questionId = targetId;
            case COMMENT -> this.commentId = targetId;
        }
        this.reason = reason;
        this.details = details;
    }

    Long getId() { return id; }
    Status getStatus() { return status; }

    ContentTarget target() {
        if (experienceId != null) return ContentTarget.EXPERIENCE;
        if (questionId != null) return ContentTarget.QUESTION;
        return ContentTarget.COMMENT;
    }

    long targetId() {
        return experienceId != null ? experienceId : questionId != null ? questionId : commentId;
    }
}

/** Append-only audit log entry. */
@Entity
@Table(name = "moderation_action")
class ModerationAction {

    enum Action { AUTO_HIDE, HIDE, REMOVE, RESTORE, DISMISS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "moderator_id")
    private Long moderatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Column(name = "experience_id")
    private Long experienceId;

    @Column(name = "question_id")
    private Long questionId;

    @Column(name = "comment_id")
    private Long commentId;

    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ModerationAction() {}

    ModerationAction(Long moderatorId, Action action, ContentTarget target, long targetId, String note) {
        this.moderatorId = moderatorId;
        this.action = action;
        switch (target) {
            case EXPERIENCE -> this.experienceId = targetId;
            case QUESTION -> this.questionId = targetId;
            case COMMENT -> this.commentId = targetId;
        }
        this.note = note;
    }
}
