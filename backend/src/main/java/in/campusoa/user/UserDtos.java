package in.campusoa.user;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTOs (Data Transfer Objects) for users. Records are immutable, so a DTO can't be half-filled.
 * <p>
 * There are deliberately TWO output shapes:
 * <ul>
 *   <li>{@link MeDto}: what you see about yourself, including private CGPA/backlogs.</li>
 *   <li>{@link AuthorDto}: what OTHER people see about you. It has no private fields at all,
 *       so it's impossible to leak them by mistake.</li>
 * </ul>
 */
public final class UserDtos {

    private UserDtos() {}

    public record MeDto(
            long id,
            String email,
            String displayName,
            String avatarUrl,
            Role role,
            String branchCode,
            Short gradYear,
            boolean leaderboardOptIn,
            BigDecimal cgpa,
            Short activeBacklogs,
            boolean profileComplete) {}

    public record AuthorDto(long id, String displayName, String avatarUrl, String branchCode, Short gradYear) {

        public static AuthorDto of(AppUser u) {
            return new AuthorDto(u.getId(), u.getDisplayName(), u.getAvatarUrl(), u.getBranchCode(), u.getGradYear());
        }
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String displayName,
            @Size(max = 16) String branchCode,
            @Min(2020) @Max(2040) Short gradYear,
            @DecimalMin("0.00") @DecimalMax("10.00") @Digits(integer = 2, fraction = 2) BigDecimal cgpa,
            @Min(0) @Max(60) Short activeBacklogs,
            boolean leaderboardOptIn) {}

    public record BranchDto(String code, String name) {
        public static BranchDto of(Branch b) {
            return new BranchDto(b.getCode(), b.getName());
        }
    }

    /** Admin view: role and status, but still no private academic data. */
    public record AdminUserDto(long id, String email, String displayName, Role role, String branchCode,
                               Short gradYear, boolean banned, java.time.Instant createdAt,
                               java.time.Instant lastLoginAt) {
        public static AdminUserDto of(AppUser u) {
            return new AdminUserDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getBranchCode(),
                    u.getGradYear(), u.isBanned(), u.getCreatedAt(), u.getLastLoginAt());
        }
    }
}
