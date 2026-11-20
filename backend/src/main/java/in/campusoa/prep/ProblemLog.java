package in.campusoa.prep;

import in.campusoa.question.Question;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/** A DSA problem the student solved. Plain CRUD, so Spring Data JPA does all the work. */
@Entity
@Table(name = "problem_log")
public class ProblemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Question.Difficulty difficulty;

    @Column(name = "solved_on", nullable = false)
    private LocalDate solvedOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ProblemLog() {}

    public ProblemLog(Long userId, String title, String url, Long topicId, Question.Difficulty difficulty,
                      LocalDate solvedOn) {
        this.userId = userId;
        this.title = title;
        this.url = url;
        this.topicId = topicId;
        this.difficulty = difficulty;
        this.solvedOn = solvedOn;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public Long getTopicId() { return topicId; }
    public Question.Difficulty getDifficulty() { return difficulty; }
    public LocalDate getSolvedOn() { return solvedOn; }
}
