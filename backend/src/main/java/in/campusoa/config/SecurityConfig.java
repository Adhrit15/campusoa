package in.campusoa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Milestone 1 placeholder: only ops endpoints are open. Replaced by the real OAuth2 setup in M3. */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
                .requestMatchers("/actuator/health/**", "/api/docs/**", "/error").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }
}
