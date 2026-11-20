package in.campusoa.config;

import in.campusoa.support.IntegrationTest;
import in.campusoa.support.TestUsers;
import in.campusoa.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With a limit of 3 writes/minute, the 4th write gets 429 + Retry-After; reads are never limited. */
@TestPropertySource(properties = "app.rate-limit.writes-per-minute=3")
class RateLimitIT extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired TestUsers users;

    @Test
    void fourthWriteInAMinuteIsRejected() throws Exception {
        AppUser s = users.student();
        String body = "{\"displayName\":\"Rate\",\"leaderboardOptIn\":false}";
        for (int i = 0; i < 3; i++) {
            mvc.perform(put("/api/me/profile").with(users.as(s)).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        }
        mvc.perform(put("/api/me/profile").with(users.as(s)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
        for (int i = 0; i < 5; i++) mvc.perform(get("/api/me").with(users.as(s))).andExpect(status().isOk());

        // the limit is per user: someone else is unaffected
        mvc.perform(put("/api/me/profile").with(users.as(users.student())).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
    }
}
