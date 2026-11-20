package in.campusoa.drive;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Eligibility rules for one drive, entered by an admin. A drive WITHOUT a row here has
 * "criteria unknown". {@code eligibleBranches} maps the drive_eligible_branch junction table as
 * a simple set of branch codes; empty means "open to all branches".
 */
@Entity
@Table(name = "drive_criteria")
public class DriveCriteria {

    @Id
    @Column(name = "drive_id")
    private Long driveId;

    @Column(name = "min_cgpa")
    private BigDecimal minCgpa;

    @Column(name = "max_backlogs")
    private Short maxBacklogs;

    @Column(name = "grad_year")
    private Short gradYear;

    private String notes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "drive_eligible_branch", joinColumns = @JoinColumn(name = "drive_id"))
    @Column(name = "branch_code")
    private Set<String> eligibleBranches = new HashSet<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected DriveCriteria() {}

    public DriveCriteria(Long driveId) {
        this.driveId = driveId;
    }

    public Long getDriveId() { return driveId; }
    public BigDecimal getMinCgpa() { return minCgpa; }
    public Short getMaxBacklogs() { return maxBacklogs; }
    public Short getGradYear() { return gradYear; }
    public String getNotes() { return notes; }
    public Set<String> getEligibleBranches() { return eligibleBranches; }

    public void update(BigDecimal minCgpa, Short maxBacklogs, Short gradYear, Set<String> branches, String notes) {
        this.minCgpa = minCgpa;
        this.maxBacklogs = maxBacklogs;
        this.gradYear = gradYear;
        this.notes = notes;
        this.eligibleBranches.clear();
        if (branches != null) this.eligibleBranches.addAll(branches);
        this.updatedAt = Instant.now();
    }

    /** Snapshot used by the pure eligibility logic. */
    public Eligibility.Rules toRules() {
        return new Eligibility.Rules(minCgpa, maxBacklogs, gradYear, Set.copyOf(eligibleBranches));
    }
}
