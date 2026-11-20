package in.campusoa.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicUrlFilterTest {

    @Test
    void rewritesRenderHostToPublicVercelUrl() throws Exception {
        var props = new AppProperties(List.of(), List.of(), "https://campusoa.vercel.app", "https://campusoa.vercel.app",
                null, null, null, null);
        var req = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        req.setScheme("http");
        req.setServerName("campusoa-api.onrender.com");
        req.setServerPort(10000);
        var chain = new MockFilterChain();

        new PublicUrlFilter(props).doFilter(req, new MockHttpServletResponse(), chain);

        HttpServletRequest seen = (HttpServletRequest) chain.getRequest();
        assertThat(seen.getRequestURL()).hasToString("https://campusoa.vercel.app/login/oauth2/code/google");
        assertThat(seen.isSecure()).isTrue();
        assertThat(seen.getServerPort()).isEqualTo(443);
    }
}
