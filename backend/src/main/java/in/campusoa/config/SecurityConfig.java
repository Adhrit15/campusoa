package in.campusoa.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.campusoa.auth.CampusOidcUserService;
import in.campusoa.auth.SpaCsrf;
import in.campusoa.common.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

/**
 * The whole security model in one place. Every request passes through this filter chain before
 * reaching a controller.
 *
 * <pre>
 * request -> CORS -> CSRF check -> session lookup (SPRING_SESSION) -> OAuth2 login filters
 *         -> authorisation rules below -> controller (+ @PreAuthorize on admin/mod controllers)
 * </pre>
 */
@Configuration
@EnableMethodSecurity   // turns on @PreAuthorize: a second, per-method layer of role checks
public class SecurityConfig {

    /** GET endpoints anyone (even signed out) may read. */
    private static final String[] PUBLIC_READS = {
            "/api/drives/**", "/api/companies/**", "/api/experiences/**", "/api/questions/**",
            "/api/branches", "/api/topics", "/api/insights/**"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CampusOidcUserService oidcUserService,
                                            AppProperties props, ObjectMapper json) throws Exception {
        http
            .cors(cors -> {})   // uses the CorsConfigurationSource bean below
            .csrf(csrf -> csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrf.TokenRequestHandler()))
            .addFilterAfter(new SpaCsrf.CookieFilter(), BasicAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/health/**", "/api/docs/**", "/error").permitAll()
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/mod/**").hasRole("MODERATOR")   // admins have ROLE_MODERATOR too
                    .requestMatchers(HttpMethod.GET, PUBLIC_READS).permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll())   // anything we didn't think of is closed by default

            .oauth2Login(oauth -> oauth
                    .userInfoEndpoint(ui -> ui.oidcUserService(oidcUserService))
                    .successHandler((req, res, authn) -> res.sendRedirect(props.publicUrl() + "/"))
                    .failureHandler((req, res, ex) -> {
                        String code = ex instanceof OAuth2AuthenticationException o
                                ? o.getError().getErrorCode() : "login_failed";
                        res.sendRedirect(UriComponentsBuilder.fromUriString(props.publicUrl())
                                .path("/login").queryParam("error", code).toUriString());
                    }))

            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")   // POST only, and needs the CSRF header
                    .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))

            // An API should answer 401/403 JSON, not redirect to a login page.
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) ->
                            writeError(res, json, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                                    "Sign in to do that", req.getRequestURI()))
                    .accessDeniedHandler((req, res, e) ->
                            writeError(res, json, HttpStatus.FORBIDDEN, "FORBIDDEN",
                                    "You don't have permission to do that (or the CSRF token is missing)",
                                    req.getRequestURI())))

            .headers(h -> h
                    .contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'; "
                                    + "frame-ancestors 'none'; object-src 'none'"))
                    .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));

        return http.build();
    }

    /**
     * CORS: in production the browser talks to the backend through Vercel's same-origin proxy,
     * so CORS isn't even exercised. We still lock it to the one frontend origin as defence in depth.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(props.frontendOrigin()));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cfg.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    private static void writeError(HttpServletResponse res, ObjectMapper json, HttpStatus status, String code,
                                   String message, String path) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        json.writeValue(res.getOutputStream(), GlobalExceptionHandler.body(status, code, message, path));
    }
}
