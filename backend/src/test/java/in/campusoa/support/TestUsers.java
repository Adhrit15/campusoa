package in.campusoa.support;

import in.campusoa.auth.AppUserPrincipal;
import in.campusoa.user.AppUser;
import in.campusoa.user.AppUserRepository;
import in.campusoa.user.Role;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

/**
 * Creates real users in the test database and "signs them in" for MockMvc requests, skipping
 * the Google round-trip. Usage: {@code mvc.perform(get("/api/me").with(users.as(student)))}.
 */
@Component
public class TestUsers {

    private final AppUserRepository repo;

    public TestUsers(AppUserRepository repo) {
        this.repo = repo;
    }

    public AppUser create(Role role) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        AppUser u = new AppUser("user-" + id + "@student.example.edu", "sub-" + id, "Test " + id);
        u.setRole(role);
        return repo.save(u);
    }

    public AppUser student() { return create(Role.STUDENT); }

    public AppUser moderator() { return create(Role.MODERATOR); }

    public AppUser admin() { return create(Role.ADMIN); }

    public RequestPostProcessor as(AppUser u) {
        return SecurityMockMvcRequestPostProcessors.authentication(token(u));
    }

    public static UsernamePasswordAuthenticationToken token(AppUser u) {
        AppUserPrincipal p = principal(u.getId(), u.getEmail(), u.getRole());
        return new UsernamePasswordAuthenticationToken(p, null, p.getAuthorities());
    }

    public static AppUserPrincipal principal(long id, String email, Role role) {
        OidcIdToken idToken = OidcIdToken.withTokenValue("test-token")
                .subject("sub-" + id).claim("email", email)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
        return new AppUserPrincipal(id, email, role, new DefaultOidcUser(java.util.List.of(), idToken));
    }
}
