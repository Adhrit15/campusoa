package in.campusoa.question;

import in.campusoa.common.ContentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A question someone was asked, described in the poster's own words. Many-to-many with topics
 * through the question_topic junction table: JPA reads and writes that table for us.
 */
@Entity
@Table(name = "question")
public class Question {

    public enum Difficulty { EASY, MEDIUM, HARD }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experience_id")
    private Long experienceId;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "body_md", nullable = false)
    private String bodyMd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany
    @JoinTable(name = "question_topic",
            joinColumns = @JoinColumn(name = "question_id"),
            inverseJoinColumns = @JoinColumn(name = "topic_id"))
    private Set<Topic> topics = new HashSet<>();

    protected Question() {}

    public Question(Long authorId, Long companyId, Long experienceId, String bodyMd, Difficulty difficulty,
                    Set<Topic> topics) {
        this.authorId = authorId;
        this.companyId = companyId;
        this.experienceId = experienceId;
        this.bodyMd = bodyMd;
        this.difficulty = difficulty;
        this.topics.addAll(topics);
    }

    public Long getId() { return id; }
    public Long getExperienceId() { return experienceId; }
    public Long getCompanyId() { return companyId; }
    public Long getAuthorId() { return authorId; }
    public String getBodyMd() { return bodyMd; }
    public Difficulty getDifficulty() { return difficulty; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Set<Topic> getTopics() { return topics; }
}
