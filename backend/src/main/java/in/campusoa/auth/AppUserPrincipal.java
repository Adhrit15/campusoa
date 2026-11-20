package in.campusoa.auth;

import in.campusoa.user.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * "Who is making this request", as stored in the session after Google sign-in.
 * <p>
 * It wraps Google's {@link OidcUser} (so Spring's OAuth2 machinery is happy) and adds OUR user id
 * and role. Controllers receive it with {@code @AuthenticationPrincipal AppUserPrincipal me}.
 * <p>
 * It must be {@link Serializable} because Spring Session JDBC stores the session (including this
 * object) as bytes in the SPRING_SESSION_ATTRIBUTES table.
 * <p>
 * {@link #getName()} returns our user id. Spring Session indexes sessions by this name, which is
 * how {@link SessionRevoker} finds and deletes every session of a banned user.
 */
public class AppUserPrincipal implements OidcUser, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final String email;
    private final Role role;
    private final OidcUser google;

    public AppUserPrincipal(long userId, String email, Role role, OidcUser google) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.google = google;
    }

    public long userId() { return userId; }

    public String email() { return email; }

    public Role role() { return role; }

    public boolean isModerator() { return role.atLeast(Role.MODERATOR); }

    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public String getName() { return String.valueOf(userId); }

    /** Every role at or below ours, so hasRole('MODERATOR') is true for admins too. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return java.util.Arrays.stream(Role.values())
                .filter(role::atLeast)
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .toList();
    }

    @Override public Map<String, Object> getClaims() { return google.getClaims(); }
    @Override public OidcUserInfo getUserInfo() { return google.getUserInfo(); }
    @Override public OidcIdToken getIdToken() { return google.getIdToken(); }
    @Override public Map<String, Object> getAttributes() { return google.getAttributes(); }

    @Override
    public String toString() {   // never log tokens
        return "AppUserPrincipal[userId=" + userId + ", role=" + role + "]";
    }
}
