package in.campusoa.auth;

import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

/**
 * Logs a user out everywhere by deleting their sessions from the SPRING_SESSION table.
 * Used when an admin bans a user or changes their role (the role is cached in the session, so
 * they must sign in again to pick up the new one), and when a user deletes their account.
 * <p>
 * This instant revocation is one of the main reasons we chose server sessions over JWTs: a JWT
 * stays valid until it expires, whatever happens in the database.
 */
@Component
public class SessionRevoker {

    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public SessionRevoker(FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.sessions = sessions;
    }

    public int revokeAll(long userId) {
        var found = sessions.findByPrincipalName(String.valueOf(userId));
        found.keySet().forEach(sessions::deleteById);
        return found.size();
    }
}
