package in.campusoa.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A signed-in person. Deliberately contains NO private academic data: CGPA and backlogs live in
 * {@link PrivateProfile}, so serialising or logging this entity can't leak them.
 * (Entities are never returned from controllers anyway: we always map to DTOs.)
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "google_sub", nullable = false, unique = true)
    private String googleSub;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "grad_year")
    private Short gradYear;

    @Column(name = "leaderboard_opt_in", nullable = false)
    private boolean leaderboardOptIn;

    @Column(nullable = false)
    private boolean banned;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected AppUser() {}

    public AppUser(String email, String googleSub, String displayName) {
        this.email = email;
        this.googleSub = googleSub;
        this.displayName = displayName;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGoogleSub() { return googleSub; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    public Short getGradYear() { return gradYear; }
    public void setGradYear(Short gradYear) { this.gradYear = gradYear; }
    public boolean isLeaderboardOptIn() { return leaderboardOptIn; }
    public void setLeaderboardOptIn(boolean leaderboardOptIn) { this.leaderboardOptIn = leaderboardOptIn; }
    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
