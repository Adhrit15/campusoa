package in.campusoa.user;

/**
 * Roles are ordered by power. Spring Security sees them as authorities "ROLE_STUDENT" etc.
 * Rules use hasRole("ADMIN"); for "moderator or above", see {@link #atLeast(Role)}.
 */
public enum Role {
    STUDENT, MODERATOR, ADMIN;

    public boolean atLeast(Role other) {
        return this.ordinal() >= other.ordinal();
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
