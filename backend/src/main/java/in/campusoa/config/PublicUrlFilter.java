package in.campusoa.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;

/**
 * Makes every request look as if it arrived at APP_PUBLIC_URL (e.g. https://campusoa.vercel.app).
 * <p>
 * <b>Why:</b> browsers talk to Vercel, which proxies /api, /oauth2 and /login/oauth2 to Render. Without
 * this, Spring would rebuild URLs with Render's hostname. During Google sign-in, Spring Security
 * checks that the callback URL equals the redirect_uri it sent to Google; a hostname mismatch
 * would make every login fail with "invalid_redirect_uri_parameter". Since all real traffic comes
 * through the public URL, pinning scheme/host/port to it is both simple and correct.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PublicUrlFilter extends OncePerRequestFilter {

    private final String scheme;
    private final String host;
    private final int port;

    public PublicUrlFilter(AppProperties props) {
        URI uri = URI.create(props.publicUrl());
        this.scheme = uri.getScheme();
        this.host = uri.getHost();
        this.port = uri.getPort() != -1 ? uri.getPort() : ("https".equals(scheme) ? 443 : 80);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public String getScheme() { return scheme; }
            @Override public String getServerName() { return host; }
            @Override public int getServerPort() { return port; }
            @Override public boolean isSecure() { return "https".equals(scheme); }

            @Override
            public StringBuffer getRequestURL() {
                StringBuffer url = new StringBuffer(scheme).append("://").append(host);
                if (!((port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme)))) {
                    url.append(':').append(port);
                }
                return url.append(getRequestURI());
            }
        }, response);
    }
}
