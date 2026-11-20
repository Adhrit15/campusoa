package in.campusoa.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Private academic details used ONLY for the eligibility checker and only ever shown to the
 * owner. Shares its primary key with app_user (one-to-one).
 */
@Entity
@Table(name = "private_profile")
public class PrivateProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    private BigDecimal cgpa;

    @Column(name = "active_backlogs")
    private Short activeBacklogs;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected PrivateProfile() {}

    public PrivateProfile(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() { return userId; }
    public BigDecimal getCgpa() { return cgpa; }
    public Short getActiveBacklogs() { return activeBacklogs; }

    public void update(BigDecimal cgpa, Short activeBacklogs) {
        this.cgpa = cgpa;
        this.activeBacklogs = activeBacklogs;
        this.updatedAt = Instant.now();
    }
}
