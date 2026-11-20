package in.campusoa.scraper;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** One execution of the scraper, kept for the admin panel and for conditional GETs (ETag). */
@Entity
@Table(name = "scrape_run")
public class ScrapeRun {

    public enum Status { RUNNING, SUCCESS, NOT_MODIFIED, SKIPPED_ROBOTS, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.RUNNING;

    @Column(name = "http_status")
    private Short httpStatus;

    private String etag;

    @Column(name = "last_modified")
    private String lastModified;

    @Column(name = "items_seen", nullable = false)
    private int itemsSeen;

    @Column(name = "items_new", nullable = false)
    private int itemsNew;

    private String error;

    protected ScrapeRun() {}

    public ScrapeRun(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public void finish(Status status, Integer httpStatus, Instant at) {
        this.status = status;
        this.httpStatus = httpStatus == null ? null : httpStatus.shortValue();
        this.finishedAt = at;
    }

    public Long getId() { return id; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Status getStatus() { return status; }
    public Short getHttpStatus() { return httpStatus; }
    public String getEtag() { return etag; }
    public String getLastModified() { return lastModified; }
    public int getItemsSeen() { return itemsSeen; }
    public int getItemsNew() { return itemsNew; }
    public String getError() { return error; }

    public void setCacheValidators(String etag, String lastModified) {
        this.etag = etag;
        this.lastModified = lastModified;
    }

    public void setCounts(int seen, int created) {
        this.itemsSeen = seen;
        this.itemsNew = created;
    }

    public void setError(String error) {
        this.error = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
    }
}
