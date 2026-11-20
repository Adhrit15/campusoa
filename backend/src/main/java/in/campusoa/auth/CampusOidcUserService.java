package in.campusoa.auth;

import in.campusoa.user.AppUser;
import in.campusoa.user.UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Hook into Spring's OAuth2 login. After Spring has exchanged Google's authorisation code for
 * tokens and verified the ID token's signature, it calls {@link #loadUser}. We:
 * <ol>
 *   <li>let the parent class build the standard {@link OidcUser} (claims: sub, email, name...)</li>
 *   <li>apply OUR rules (domain allow-list, banned users) and create/update the app_user row</li>
 *   <li>return an {@link AppUserPrincipal} carrying our user id and role</li>
 * </ol>
 * Throwing {@link OAuth2AuthenticationException} aborts the login; the failure handler then
 * redirects the browser to /login?error=CODE.
 */
@Component
public class CampusOidcUserService extends OidcUserService {

    private final UserService users;

    public CampusOidcUserService(UserService users) {
        this.users = users;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser google = super.loadUser(request);
        return toPrincipal(google);
    }

    /** Separated from loadUser so it can be tested without a real Google round-trip. */
    AppUserPrincipal toPrincipal(OidcUser google) {
        try {
            AppUser user = users.registerLogin(new UserService.GoogleProfile(
                    google.getSubject(),
                    google.getEmail(),
                    Boolean.TRUE.equals(google.getEmailVerified()),
                    google.getFullName(),
                    google.getPicture()));
            return new AppUserPrincipal(user.getId(), user.getEmail(), user.getRole(), google);
        } catch (LoginRejectedException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error(e.code(), e.getMessage(), null), e);
        }
    }
}
