package in.campusoa.prep;

import in.campusoa.support.IntegrationTest;
import in.campusoa.support.TestUsers;
import in.campusoa.user.AppUser;
import in.campusoa.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Roadmap, problem log, dashboard and the window-function leaderboard against real Postgres. */
class PrepIT extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired TestUsers users;
    @Autowired AppUserRepository userRepo;
    @Autowired JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM problem_log");
    }

    private AppUser rankedUser(String name, String branch, int year, boolean optIn) {
        AppUser u = users.student();
        u.setDisplayName(name);
        u.setBranchCode(branch);
        u.setGradYear((short) year);
        u.setLeaderboardOptIn(optIn);
        return userRepo.save(u);
    }

    /** Insert n problems solved today for a user (straight SQL: fast and explicit). */
    private void solved(AppUser u, int n) {
        Long topic = jdbc.queryForObject("SELECT id FROM topic WHERE code = 'dsa-arrays'", Long.class);
        for (int i = 0; i < n; i++) {
            jdbc.update("INSERT INTO problem_log (user_id, title, url, topic_id, difficulty, solved_on) VALUES (?,?,?,?,?,?)",
                    u.getId(), "P" + i, "https://example.com/" + u.getId() + "/" + i, topic, "EASY", today);
        }
    }

    @Test
    void rankVsDenseRankWithTiesAndOptOut() throws Exception {
        AppUser a = rankedUser("Ananya", "CSE", 2027, true);
        AppUser b = rankedUser("Bharath", "IT", 2027, true);
        AppUser c = rankedUser("Charan", "CSE", 2027, true);
        AppUser hidden = rankedUser("Hidden", "CSE", 2027, false);   // opted out: never ranked
        solved(a, 5);
        solved(b, 5);
        solved(c, 3);
        solved(hidden, 50);

        mvc.perform(get("/api/leaderboard").param("period", "WEEK").with(users.as(c)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRanked").value(3))
                .andExpect(jsonPath("$.entries[0].user.displayName").value("Ananya"))
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[1].rank").value(1))        // tie
                .andExpect(jsonPath("$.entries[2].user.displayName").value("Charan"))
                .andExpect(jsonPath("$.entries[2].rank").value(3))        // RANK skips 2
                .andExpect(jsonPath("$.entries[2].denseRank").value(2))   // DENSE_RANK doesn't
                .andExpect(jsonPath("$.entries[2].me").value(true))
                .andExpect(jsonPath("$.me.rank").value(3));
    }

    @Test
    void cohortRankingIsPartitionedByBranchAndYear() throws Exception {
        AppUser a = rankedUser("Ananya", "CSE", 2027, true);
        AppUser b = rankedUser("Bharath", "IT", 2027, true);
        AppUser c = rankedUser("Charan", "CSE", 2027, true);
        AppUser d = rankedUser("Divya", "CSE", 2028, true);
        solved(b, 9);
        solved(a, 5);
        solved(c, 3);
        solved(d, 7);

        mvc.perform(get("/api/leaderboard").param("scope", "COHORT").with(users.as(c)))
                .andExpect(jsonPath("$.cohortBranch").value("CSE"))
                .andExpect(jsonPath("$.totalRanked").value(2))           // only CSE 2027
                .andExpect(jsonPath("$.entries[0].user.displayName").value("Ananya"))
                .andExpect(jsonPath("$.entries[0].rank").value(1))       // 1st in cohort (3rd overall)
                .andExpect(jsonPath("$.me.rank").value(2));
    }

    @Test
    void optedOutViewerSeesBoardButHasNoRank() throws Exception {
        AppUser viewer = rankedUser("Viewer", "CSE", 2027, false);
        solved(viewer, 4);
        mvc.perform(get("/api/leaderboard").with(users.as(viewer)))
                .andExpect(jsonPath("$.optedIn").value(false))
                .andExpect(jsonPath("$.me").doesNotExist());
        mvc.perform(get("/api/leaderboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void problemLogDashboardAndRoadmap() throws Exception {
        AppUser s = rankedUser("Solver", "CSE", 2027, false);
        String body = """
                {"title":"Two Sum","url":"https://leetcode.com/problems/two-sum/","topicCode":"dsa-hashing",
                 "difficulty":"EASY","solvedOn":"%s"}""".formatted(today);
        mvc.perform(post("/api/prep/problems").with(users.as(s)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topicName").value("Hashing"));
        mvc.perform(post("/api/prep/problems").with(users.as(s)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());   // same URL twice
        mvc.perform(post("/api/prep/problems").with(users.as(s)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace(today.toString(), today.plusDays(3).toString()).replace("two-sum", "x")))
                .andExpect(status().isBadRequest());   // future date

        Long item = jdbc.queryForObject("SELECT id FROM roadmap_item ORDER BY id LIMIT 1", Long.class);
        mvc.perform(put("/api/prep/roadmap/" + item).with(users.as(s)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"done\":true}")).andExpect(status().isNoContent());

        mvc.perform(get("/api/prep/dashboard").with(users.as(s)))
                .andExpect(jsonPath("$.totalSolved").value(1))
                .andExpect(jsonPath("$.solvedThisWeek").value(1))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.perWeek.length()").value(12))
                .andExpect(jsonPath("$.perWeek[11].solved").value(1))
                .andExpect(jsonPath("$.topicCoverage[?(@.code=='dsa-hashing')].solved").value(1))
                .andExpect(jsonPath("$.roadmapDone").value(1));
        mvc.perform(get("/api/prep/roadmap").with(users.as(s)))
                .andExpect(jsonPath("$[0].items[0].done").value(true));

        // someone else can't delete my problem (404, not 403: don't reveal it exists)
        Long pid = jdbc.queryForObject("SELECT id FROM problem_log WHERE user_id = ?", Long.class, s.getId());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/prep/problems/" + pid)
                .with(users.as(users.student())).with(csrf())).andExpect(status().isNotFound());
    }
}
