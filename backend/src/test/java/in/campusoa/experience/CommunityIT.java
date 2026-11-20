package in.campusoa.experience;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.campusoa.support.IntegrationTest;
import in.campusoa.support.TestUsers;
import in.campusoa.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Experiences, questions, votes, comments, search, reports and moderation, end to end. */
class CommunityIT extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired TestUsers users;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    private long companyId(String slug) {
        return jdbc.queryForObject("SELECT id FROM company WHERE slug = ?", Long.class, slug);
    }

    private long postExperience(AppUser author, String bodyWord, boolean anonymous) throws Exception {
        String body = """
                {"companyId":%d,"year":2026,"roleTitle":"SDE Intern","difficulty":4,"outcome":"SELECTED",
                 "bodyMd":"Round one had two problems; the second needed %s over a weighted graph. Interviewers were friendly and asked follow-ups.",
                 "rounds":[{"name":"Online assessment","description":"90 minutes"},{"name":"Technical interview"}],
                 "questions":[{"bodyMd":"Find the shortest path in a weighted graph with K stops","difficulty":"MEDIUM","topicCodes":["dsa-graphs"]}],
                 "anonymous":%s,"acceptGuidelines":true}""".formatted(companyId("microsoft"), bodyWord, anonymous);
        String res = mvc.perform(post("/api/experiences").with(users.as(author)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    @Test
    void guidelinesMustBeAccepted() throws Exception {
        mvc.perform(post("/api/experiences").with(users.as(users.student())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"companyId":1,"year":2026,"roleTitle":"SDE","difficulty":3,"outcome":"WAITING",
                                 "bodyMd":"short","acceptGuidelines":false}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("acceptGuidelines")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("bodyMd")));
    }

    @Test
    void anonymousAuthorIsHiddenFromOthersButNotModerators() throws Exception {
        AppUser author = users.student();
        long id = postExperience(author, "dijkstra", true);

        mvc.perform(get("/api/experiences/" + id).with(users.as(users.student())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anonymous").value(true))
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.questions[0].author").doesNotExist())   // questions inherit anonymity
                .andExpect(jsonPath("$.rounds.length()").value(2));
        mvc.perform(get("/api/experiences/" + id))   // signed out
                .andExpect(jsonPath("$.author").doesNotExist());
        mvc.perform(get("/api/experiences/" + id).with(users.as(users.moderator())))
                .andExpect(jsonPath("$.author.id").value(author.getId()));
        mvc.perform(get("/api/experiences/" + id).with(users.as(author)))
                .andExpect(jsonPath("$.author.id").value(author.getId()))
                .andExpect(jsonPath("$.mine").value(true));
        // the list endpoint applies the same rule
        mvc.perform(get("/api/experiences").param("q", "dijkstra"))
                .andExpect(jsonPath("$.content[0].author").doesNotExist());
    }

    @Test
    void fullTextSearchStemsWordsAndHighlights() throws Exception {
        postExperience(users.student(), "memoization", false);
        // "memoize" and "memoization" share the stem "memoiz"
        mvc.perform(get("/api/experiences").param("q", "memoize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].excerpt", containsString("[[memoization]]")));
        mvc.perform(get("/api/experiences").param("q", "zzzunlikelyword"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/questions").param("q", "shortest paths").param("topic", "dsa"))
                .andExpect(jsonPath("$.content[0].topics[0].code").value("dsa-graphs"));
    }

    @Test
    void votingIsIdempotentAndNotForYourOwnPost() throws Exception {
        AppUser author = users.student();
        AppUser fan = users.student();
        long id = postExperience(author, "bellman", false);
        mvc.perform(post("/api/experiences/" + id + "/vote").with(users.as(fan)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/experiences/" + id + "/vote").with(users.as(fan)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/experiences/" + id).with(users.as(fan)))
                .andExpect(jsonPath("$.votes").value(1))
                .andExpect(jsonPath("$.votedByMe").value(true));
        mvc.perform(post("/api/experiences/" + id + "/vote").with(users.as(author)).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commentsAndOwnership() throws Exception {
        AppUser author = users.student();
        long id = postExperience(author, "floyd", false);
        AppUser other = users.student();
        mvc.perform(post("/api/experiences/" + id + "/comments").with(users.as(other)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Thanks, very helpful!\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/experiences/" + id + "/comments"))
                .andExpect(jsonPath("$.content[0].body").value("Thanks, very helpful!"));
        // someone else can't edit the post
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/experiences/" + id)
                        .with(users.as(other)).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void threeReportsAutoHideThenModeratorDecides() throws Exception {
        AppUser author = users.student();
        long id = postExperience(author, "kruskal", false);
        String report = "{\"targetType\":\"EXPERIENCE\",\"targetId\":" + id + ",\"reason\":\"CONFIDENTIAL\"}";

        AppUser r1 = users.student();
        mvc.perform(post("/api/reports").with(users.as(r1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/reports").with(users.as(r1)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isConflict());   // same person twice
        mvc.perform(post("/api/reports").with(users.as(author)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(report))
                .andExpect(status().isBadRequest()); // can't report your own
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/reports").with(users.as(users.student())).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content(report)).andExpect(status().isCreated());
        }

        // hidden: public gets 404, author and moderators still see it
        mvc.perform(get("/api/experiences/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/experiences/" + id).with(users.as(author)))
                .andExpect(jsonPath("$.status").value("HIDDEN"));

        // moderation queue lists it; a student can't see the queue
        AppUser mod = users.moderator();
        mvc.perform(get("/api/mod/reports").with(users.as(users.student()))).andExpect(status().isForbidden());
        String queue = mvc.perform(get("/api/mod/reports").param("size", "50").with(users.as(mod)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long reportId = 0;
        for (var item : json.readTree(queue).get("content")) {
            if (item.get("targetId").asLong() == id) {
                reportId = item.get("reportId").asLong();
                org.assertj.core.api.Assertions.assertThat(item.get("openReportsForTarget").asLong()).isEqualTo(3);
            }
        }

        // moderator dismisses: the post comes back and all 3 reports close
        mvc.perform(post("/api/mod/reports/" + reportId + "/resolve").with(users.as(mod)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"DISMISS\",\"note\":\"own words, fine\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/experiences/" + id)).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM report WHERE status = 'OPEN' AND experience_id = ?", Long.class, id)).isZero();
        mvc.perform(get("/api/mod/actions").with(users.as(mod)))
                .andExpect(jsonPath("$.content[*].action", hasItem("AUTO_HIDE")))
                .andExpect(jsonPath("$.content[*].action", hasItem("DISMISS")));
    }
}
