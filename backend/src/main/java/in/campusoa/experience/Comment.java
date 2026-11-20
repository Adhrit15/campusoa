package in.campusoa.experience;

import in.campusoa.common.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experience_id", nullable = false)
    private Long experienceId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Comment() {}

    public Comment(Long experienceId, Long authorId, String body) {
        this.experienceId = experienceId;
        this.authorId = authorId;
        this.body = body;
    }

    public Long getId() { return id; }
    public Long getExperienceId() { return experienceId; }
    public Long getAuthorId() { return authorId; }
    public String getBody() { return body; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
