package in.campusoa.user;

import in.campusoa.auth.EmailDomainPolicy;
import in.campusoa.auth.LoginRejectedException;
import in.campusoa.auth.SessionRevoker;
import in.campusoa.common.ApiException;
import in.campusoa.user.UserDtos.MeDto;
import in.campusoa.user.UserDtos.UpdateProfileRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

/**
 * Business rules about users: who can sign in, profile updates, and account deletion.
 * {@code @Transactional} makes each public method one database transaction: either every write
 * in it is committed, or (on an exception) none are.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository users;
    private final PrivateProfileRepository profiles;
    private final BranchRepository branches;
    private final EmailDomainPolicy policy;
    private final SessionRevoker sessionRevoker;
    private final Clock clock;

    public UserService(AppUserRepository users, PrivateProfileRepository profiles, BranchRepository branches,
                       EmailDomainPolicy policy, SessionRevoker sessionRevoker, Clock clock) {
        this.users = users;
        this.profiles = profiles;
        this.branches = branches;
        this.policy = policy;
        this.sessionRevoker = sessionRevoker;
        this.clock = clock;
    }

    /** What Google told us about the person signing in. */
    public record GoogleProfile(String sub, String email, boolean emailVerified, String name, String pictureUrl) {}

    /**
     * Called on every successful Google sign-in. Creates the user the first time (sign-up),
     * refreshes name/photo afterwards, and enforces the allow-list and bans.
     */
    @Transactional
    public AppUser registerLogin(GoogleProfile g) {
        if (g.email() == null || !g.emailVerified()) {
            throw new LoginRejectedException("email_unverified", "Google account email is not verified");
        }
        String email = g.email().toLowerCase(Locale.ROOT);
        if (!policy.isAllowed(email)) {
            throw new LoginRejectedException("domain_not_allowed", "Only college email addresses can sign in");
        }

        AppUser user = users.findByGoogleSub(g.sub()).orElseGet(() -> {
            log.info("New user signed up (id assigned on insert)");
            return new AppUser(email, g.sub(), displayNameOrDefault(g.name(), email));
        });
        if (user.isBanned()) {
            throw new LoginRejectedException("account_banned", "This account has been suspended");
        }
        user.setEmail(email);
        if (g.pictureUrl() != null) user.setAvatarUrl(g.pictureUrl());
        if (policy.isBootstrapAdmin(email)) user.setRole(Role.ADMIN);   // promote only, never demote
        user.setLastLoginAt(Instant.now(clock));
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public MeDto me(long userId) {
        AppUser u = load(userId);
        PrivateProfile p = profiles.findById(userId).orElse(null);
        return toMe(u, p);
    }

    @Transactional
    public MeDto updateProfile(long userId, UpdateProfileRequest req) {
        AppUser u = load(userId);
        if (req.branchCode() != null && !branches.existsById(req.branchCode())) {
            throw ApiException.badRequest("UNKNOWN_BRANCH", "Unknown branch: " + req.branchCode());
        }
        u.setDisplayName(req.displayName().trim());
        u.setBranchCode(req.branchCode());
        u.setGradYear(req.gradYear());
        u.setLeaderboardOptIn(req.leaderboardOptIn());

        PrivateProfile p = profiles.findById(userId).orElseGet(() -> new PrivateProfile(userId));
        p.update(req.cgpa(), req.activeBacklogs());
        profiles.save(p);
        return toMe(u, p);
    }

    /**
     * Deletes the account. Thanks to the FK rules in V1__schema.sql the database does the rest:
     * private profile, roadmap progress, problem log and votes CASCADE (deleted); experiences,
     * questions and comments are kept but their author_id becomes NULL ("deleted user").
     */
    @Transactional
    public void deleteAccount(long userId) {
        AppUser u = load(userId);
        users.delete(u);
        users.flush();
        sessionRevoker.revokeAll(userId);
    }

    public AppUser load(long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
    }

    static MeDto toMe(AppUser u, PrivateProfile p) {
        boolean complete = u.getBranchCode() != null && u.getGradYear() != null;
        return new MeDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getAvatarUrl(), u.getRole(),
                u.getBranchCode(), u.getGradYear(), u.isLeaderboardOptIn(),
                p == null ? null : p.getCgpa(), p == null ? null : p.getActiveBacklogs(), complete);
    }

    private static String displayNameOrDefault(String name, String email) {
        if (name != null && !name.isBlank()) return name.length() > 100 ? name.substring(0, 100) : name;
        return email.substring(0, email.indexOf('@'));
    }
}
