package in.campusoa.drive;

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

/**
 * One placement circular / recruitment event. Scraped drives are deduplicated on {@code pdfUrl}.
 * Several circulars for the same company (PPT, then OA, then interviews) can be MERGED into one
 * drive by an admin: the merged ones point at the survivor via {@code mergedIntoId} and are hidden.
 */
@Entity
@Table(name = "drive")
public class Drive {

    public enum Source { SCRAPED, MANUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_guess")
    private String companyGuess;

    @Column(nullable = false)
    private String title;

    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;

    @Column(name = "pdf_url", unique = true)
    private String pdfUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "role_title")
    private String roleTitle;

    @Column(name = "ctc_text")
    private String ctcText;

    @Column(name = "merged_into_id")
    private Long mergedIntoId;

    @Column(nullable = false)
    private boolean hidden;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Drive() {}

    public Drive(Source source, String title, LocalDate noticeDate) {
        this.source = source;
        this.title = title;
        this.noticeDate = noticeDate;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getCompanyGuess() { return companyGuess; }
    public void setCompanyGuess(String companyGuess) { this.companyGuess = companyGuess; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getNoticeDate() { return noticeDate; }
    public void setNoticeDate(LocalDate noticeDate) { this.noticeDate = noticeDate; }
    public String getPdfUrl() { return pdfUrl; }
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }
    public Source getSource() { return source; }
    public String getRoleTitle() { return roleTitle; }
    public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }
    public String getCtcText() { return ctcText; }
    public void setCtcText(String ctcText) { this.ctcText = ctcText; }
    public Long getMergedIntoId() { return mergedIntoId; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public void touch() { this.updatedAt = Instant.now(); }

    public void mergeInto(long targetId) {
        this.mergedIntoId = targetId;
        this.hidden = true;
        touch();
    }
}
