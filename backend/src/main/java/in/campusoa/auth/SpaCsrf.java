package in.campusoa.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * CSRF for a single-page app, following the Spring Security reference docs.
 * <p>
 * <b>The attack:</b> a malicious site makes your browser send {@code POST /api/experiences}. The
 * browser attaches your session cookie automatically, so without protection the request would
 * succeed as you.
 * <p>
 * <b>The defence (double-submit cookie):</b> the server sets a random token in a cookie named
 * {@code XSRF-TOKEN} (readable by JavaScript, unlike the session cookie). Our SPA copies it into
 * an {@code X-XSRF-TOKEN} header on every write. An attacker's page can make the browser SEND the
 * cookie but cannot READ it (same-origin policy), so it can't produce the matching header.
 */
public final class SpaCsrf {

    private SpaCsrf() {}

    /**
     * Uses BREACH-safe (XOR-masked) tokens when rendered into a page, but accepts the raw cookie
     * value when it comes back in a header, which is what an SPA sends.
     */
    public static final class TokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            xor.handle(request, response, csrfToken);
            csrfToken.get();   // force the token to load so the cookie is (re)written on every response
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String header = request.getHeader(csrfToken.getHeaderName());
            return StringUtils.hasText(header)
                    ? plain.resolveCsrfTokenValue(request, csrfToken)
                    : xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    /** Touches the deferred token on every request so the XSRF-TOKEN cookie always exists. */
    public static final class CookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) token.getToken();
            chain.doFilter(request, response);
        }
    }
}
