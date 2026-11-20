package in.campusoa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point. {@code @SpringBootApplication} turns on component scanning for every class under
 * {@code in.campusoa}, so each feature package (drive, company, prep, ...) is picked up automatically.
 * {@code @ConfigurationPropertiesScan} binds {@code app.*} settings to {@link in.campusoa.config.AppProperties}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CampusOaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusOaApplication.class, args);
    }
}
