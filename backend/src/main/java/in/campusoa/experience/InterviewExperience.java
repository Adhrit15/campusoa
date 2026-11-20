package in.campusoa.experience;

import in.campusoa.common.ContentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A student's interview experience. Its rounds are a true part of it (an "aggregate" in DDD terms):
 * they're created, replaced and deleted together with the experience, so they're mapped as a
 * cascading {@code @OneToMany}. {@code orphanRemoval} deletes a round as soon as it is removed
 * from the list.
 * <p>
 * The {@code search_vector} column is computed by Postgres and deliberately NOT mapped here.
 */
@Entity
@Table(name = "interview_experience")
public class InterviewExperience {

    public enum Outcome { SELECTED, REJECTED, WAITING, WITHDREW }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "interview_year", nullable = false)
    private short interviewYear;

    @Column(name = "role_title", nullable = false)
    private String roleTitle;

    @Column(nullable = false)
    private short difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome;

    @Column(name = "body_md", nullable = false)
    private String bodyMd;

    @Column(nullable = false)
    private boolean anonymous;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status = ContentStatus.PUBLISHED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "experience_id", nullable = false)
    @OrderBy("position")
    private List<ExperienceRound> rounds = new ArrayList<>();

    protected InterviewExperience() {}

    public InterviewExperience(Long authorId) {
        this.authorId = authorId;
    }

    public void update(long companyId, short year, String roleTitle, short difficulty, Outcome outcome,
                       String bodyMd, boolean anonymous) {
        this.companyId = companyId;
        this.interviewYear = year;
        this.roleTitle = roleTitle;
        this.difficulty = difficulty;
        this.outcome = outcome;
        this.bodyMd = bodyMd;
        this.anonymous = anonymous;
        this.updatedAt = Instant.now();
    }

    public void replaceRounds(List<ExperienceRound> newRounds) {
        rounds.clear();
        rounds.addAll(newRounds);
    }

    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public Long getCompanyId() { return companyId; }
    public short getInterviewYear() { return interviewYear; }
    public String getRoleTitle() { return roleTitle; }
    public short getDifficulty() { return difficulty; }
    public Outcome getOutcome() { return outcome; }
    public String getBodyMd() { return bodyMd; }
    public boolean isAnonymous() { return anonymous; }
    public ContentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<ExperienceRound> getRounds() { return rounds; }
}
