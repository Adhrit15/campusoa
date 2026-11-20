package in.campusoa.auth;

import in.campusoa.config.AppProperties;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Decides who may sign in and who is an admin, from configuration (ALLOWED_EMAIL_DOMAINS,
 * ADMIN_EMAILS). Pure logic with no database, so it's trivial to unit test.
 * <p>
 * Fails closed: an empty allow-list means nobody can sign in, rather than everybody.
 */
@Component
public class EmailDomainPolicy {

    private final AppProperties props;

    public EmailDomainPolicy(AppProperties props) {
        this.props = props;
    }

    public boolean isAllowed(String email) {
        String domain = domainOf(email);
        if (domain == null) return false;
        // Exact domain match only: "evil-cbit.ac.in" or "cbit.ac.in.evil.com" must NOT pass.
        return props.allowedEmailDomains().contains(domain);
    }

    public boolean isBootstrapAdmin(String email) {
        return email != null && props.adminEmails().contains(email.toLowerCase(Locale.ROOT));
    }

    static String domainOf(String email) {
        if (email == null) return null;
        int at = email.lastIndexOf('@');
        if (at <= 0 || at == email.length() - 1) return null;
        return email.substring(at + 1).toLowerCase(Locale.ROOT);
    }
}
