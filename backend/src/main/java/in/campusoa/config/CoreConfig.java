package in.campusoa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Small, app-wide beans. */
@Configuration
public class CoreConfig {

    /**
     * Inject a {@link Clock} instead of calling {@code LocalDate.now()} directly, so tests can pin
     * "today" (streaks, weekly leaderboards). Uses the configured zone (IST).
     */
    @Bean
    Clock clock(AppProperties props) {
        return Clock.system(props.timezone());
    }

    /** Metadata shown at the top of Swagger UI (/api/docs). */
    @Bean
    OpenAPI campusOaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CampusOA API")
                .version("v1")
                .description("""
                        Unofficial, student-run placement-prep platform. Not affiliated with or endorsed by CBIT.
                        Authentication is a session cookie set by Google sign-in (/oauth2/authorization/google).
                        Write requests need the X-XSRF-TOKEN header (copy of the XSRF-TOKEN cookie).""")
                .license(new License().name("MIT")));
    }
}
