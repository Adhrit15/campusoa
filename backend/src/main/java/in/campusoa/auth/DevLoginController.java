package in.campusoa.auth;

import in.campusoa.user.AppUser;
import in.campusoa.user.AppUserRepository;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * LOCAL DEVELOPMENT ONLY. {@code @Profile("dev")} means this class doesn't even exist in
 * production (the "prod" profile), so /api/dev/** returns 404 there. It lets you sign in as one
 * of the demo users from R__demo_data.sql without setting up Google OAuth, and it's what the
 * Playwright smoke test uses.
 */
@RestController
@Profile("dev")
@Hidden
public class DevLoginController {

    private static final String DEMO_DOMAIN = "@demo.campusoa.local";

    private final AppUserRepository users;
    private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public DevLoginController(AppUserRepository users) {
        this.users = users;
    }

    public record DemoUser(long id, String displayName, String role) {}

    @GetMapping("/api/dev/users")
    public List<DemoUser> demoUsers() {
        return users.findAll().stream()
                .filter(u -> u.getEmail().endsWith(DEMO_DOMAIN))
                .map(u -> new DemoUser(u.getId(), u.getDisplayName(), u.getRole().name()))
                .toList();
    }

    /** Signs the browser in as a demo user (demo accounts only), exactly as if Google had said yes. */
    @PostMapping("/api/dev/login/{userId}")
    public ResponseEntity<Void> login(@PathVariable long userId, HttpServletRequest req, HttpServletResponse res) {
        AppUser u = users.findById(userId).filter(x -> x.getEmail().endsWith(DEMO_DOMAIN)).orElse(null);
        if (u == null) return ResponseEntity.notFound().build();

        OidcIdToken token = OidcIdToken.withTokenValue("dev").subject(u.getGoogleSub()).claim("email", u.getEmail())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(86400)).build();
        AppUserPrincipal principal = new AppUserPrincipal(u.getId(), u.getEmail(), u.getRole(),
                new DefaultOidcUser(List.of(), token));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        req.changeSessionId();   // fresh session id on login: defeats session-fixation attacks
        contextRepository.saveContext(ctx, req, res);
        return ResponseEntity.noContent().build();
    }
}
