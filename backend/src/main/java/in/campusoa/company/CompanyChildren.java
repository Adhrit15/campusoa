package in.campusoa.company;

import in.campusoa.drive.StageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Small entities that belong to a company. Java allows only one PUBLIC top-level class per file,
 * so these are package-private: only the company package touches them directly; other packages
 * see DTOs.
 */
final class CompanyChildren {
    private CompanyChildren() {}
}

/** Another name a company goes by in circular titles ("JPMC" for JPMorgan Chase). */
@Entity
@Table(name = "company_alias")
class CompanyAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private String alias;

    protected CompanyAlias() {}

    CompanyAlias(Long companyId, String alias) {
        this.companyId = companyId;
        this.alias = alias;
    }

    Long getCompanyId() { return companyId; }
    String getAlias() { return alias; }
}

/** Full-time offers for one company, academic year and role. ctcMinLpa == null means "up to ctcMaxLpa". */
@Entity
@Table(name = "placement_stat")
class PlacementStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "role_title", nullable = false)
    private String roleTitle = "Full-time";

    @Column(name = "ctc_min_lpa")
    private BigDecimal ctcMinLpa;

    @Column(name = "ctc_max_lpa", nullable = false)
    private BigDecimal ctcMaxLpa;

    private Integer offers;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "source_note")
    private String sourceNote;

    protected PlacementStat() {}

    PlacementStat(Long companyId) {
        this.companyId = companyId;
    }

    Long getId() { return id; }
    Long getCompanyId() { return companyId; }
    String getAcademicYear() { return academicYear; }
    String getRoleTitle() { return roleTitle; }
    BigDecimal getCtcMinLpa() { return ctcMinLpa; }
    BigDecimal getCtcMaxLpa() { return ctcMaxLpa; }
    Integer getOffers() { return offers; }
    String getSourceUrl() { return sourceUrl; }
    String getSourceNote() { return sourceNote; }

    void update(String academicYear, String roleTitle, BigDecimal min, BigDecimal max, Integer offers,
                String sourceUrl, String sourceNote) {
        this.academicYear = academicYear;
        this.roleTitle = roleTitle == null || roleTitle.isBlank() ? "Full-time" : roleTitle.trim();
        this.ctcMinLpa = min;
        this.ctcMaxLpa = max;
        this.offers = offers;
        this.sourceUrl = sourceUrl;
        this.sourceNote = sourceNote;
    }
}

@Entity
@Table(name = "internship_stat")
class InternshipStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "stipend_inr_month", nullable = false)
    private Integer stipendInrMonth;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "source_note")
    private String sourceNote;

    protected InternshipStat() {}

    InternshipStat(Long companyId, String academicYear) {
        this.companyId = companyId;
        this.academicYear = academicYear;
    }

    Long getId() { return id; }
    String getAcademicYear() { return academicYear; }
    Integer getStipendInrMonth() { return stipendInrMonth; }
    String getSourceUrl() { return sourceUrl; }
    String getSourceNote() { return sourceNote; }

    void update(Integer stipend, String sourceUrl, String sourceNote) {
        this.stipendInrMonth = stipend;
        this.sourceUrl = sourceUrl;
        this.sourceNote = sourceNote;
    }
}

/** One step of a company's usual selection process (position 1 = first). */
@Entity
@Table(name = "selection_stage")
class SelectionStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private short position;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false)
    private StageType stageType;

    private String description;

    protected SelectionStage() {}

    SelectionStage(Long companyId, short position, StageType stageType, String description) {
        this.companyId = companyId;
        this.position = position;
        this.stageType = stageType;
        this.description = description;
    }

    short getPosition() { return position; }
    StageType getStageType() { return stageType; }
    String getDescription() { return description; }
}
