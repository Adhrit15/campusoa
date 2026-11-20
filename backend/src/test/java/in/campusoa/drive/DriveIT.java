package in.campusoa.drive;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.campusoa.support.IntegrationTest;
import in.campusoa.support.TestUsers;
import in.campusoa.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Drive admin flows + eligibility end-to-end (admin enters criteria -> student sees their status). */
class DriveIT extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired TestUsers users;
    @Autowired ObjectMapper json;

    private long createDrive(AppUser admin, String title, String date) throws Exception {
        String body = """
                {"title":"%s","noticeDate":"%s","hidden":false}""".formatted(title, date);
        String res = mvc.perform(post("/api/admin/drives").with(users.as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(res).get("id").asLong();
    }

    private void setProfile(AppUser u, String branch, int year, String cgpa, int backlogs) throws Exception {
        mvc.perform(put("/api/me/profile").with(users.as(u)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"displayName":"S","branchCode":"%s","gradYear":%d,"cgpa":%s,"activeBacklogs":%d,
                         "leaderboardOptIn":false}""".formatted(branch, year, cgpa, backlogs)))
                .andExpect(status().isOk());
    }

    @Test
    void studentsCannotCreateDrives() throws Exception {
        mvc.perform(post("/api/admin/drives").with(users.as(users.student())).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\",\"noticeDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eligibilityFollowsCriteriaAndPrivateProfile() throws Exception {
        AppUser admin = users.admin();
        long id = createDrive(admin, "Eligibility Corp OA", "2026-03-10");

        AppUser good = users.student();
        setProfile(good, "CSE", 2027, "8.0", 0);
        AppUser weak = users.student();
        setProfile(weak, "CSE", 2027, "6.5", 0);

        // before criteria: unknown
        mvc.perform(get("/api/drives/" + id).with(users.as(good)))
                .andExpect(jsonPath("$.eligibility.status").value("UNKNOWN"))
                .andExpect(jsonPath("$.criteria").doesNotExist());

        mvc.perform(put("/api/admin/drives/" + id + "/criteria").with(users.as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minCgpa\":7.0,\"maxBacklogs\":0,\"gradYear\":2027,\"branches\":[\"CSE\",\"IT\"]}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/drives/" + id).with(users.as(good)))
                .andExpect(jsonPath("$.eligibility.status").value("ELIGIBLE"));
        mvc.perform(get("/api/drives/" + id).with(users.as(weak)))
                .andExpect(jsonPath("$.eligibility.status").value("NOT_ELIGIBLE"))
                .andExpect(jsonPath("$.eligibility.reasons[0]").value("Minimum CGPA is 7"));
        // signed out: no eligibility at all, and no private data anywhere
        mvc.perform(get("/api/drives/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligibility").doesNotExist());
    }

    @Test
    void branchFilterHidesDrivesClosedToThatBranch() throws Exception {
        AppUser admin = users.admin();
        long closed = createDrive(admin, "MechOnly Ltd Interviews", "2026-04-01");
        mvc.perform(put("/api/admin/drives/" + closed + "/criteria").with(users.as(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"branches\":[\"MECH\"]}")).andExpect(status().isOk());
        long open = createDrive(admin, "OpenToAll Ltd OA", "2026-04-02");

        String res = mvc.perform(get("/api/drives").param("branch", "CSE").param("q", "Ltd").param("size", "50"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode content = json.readTree(res).get("content");
        org.assertj.core.api.Assertions.assertThat(content.findValuesAsText("title"))
                .contains("OpenToAll Ltd OA").doesNotContain("MechOnly Ltd Interviews");
        org.assertj.core.api.Assertions.assertThat(open).isNotEqualTo(closed);
    }

    @Test
    void mergeMovesStagesAndHidesSource() throws Exception {
        AppUser admin = users.admin();
        long ppt = createDrive(admin, "MergeCo PPT", "2026-05-01");
        long oa = createDrive(admin, "MergeCo OA", "2026-05-03");
        mvc.perform(put("/api/admin/drives/" + ppt + "/stages").with(users.as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stages\":[{\"stageType\":\"PPT\",\"startsAt\":\"2026-05-01T04:30:00Z\",\"mode\":\"ONLINE\"}]}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/drives/" + ppt + "/merge").with(users.as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetDriveId\":" + oa + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages[*].stageType", hasItem("PPT")));

        mvc.perform(get("/api/drives/" + ppt)).andExpect(jsonPath("$.mergedIntoId").value(oa));
        mvc.perform(get("/api/drives").param("q", "MergeCo"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void calendarShowsNoticesAndStagesInIst() throws Exception {
        AppUser admin = users.admin();
        long id = createDrive(admin, "CalCo Drive", "2026-06-10");
        // 20:00 UTC on the 11th is 01:30 IST on the 12th: the event must land on the 12th.
        mvc.perform(put("/api/admin/drives/" + id + "/stages").with(users.as(admin)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"stages\":[{\"stageType\":\"OA\",\"startsAt\":\"2026-06-11T20:00:00Z\"}]}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/drives/events").param("from", "2026-06-01").param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='CalCo Drive' && @.kind=='NOTICE')].date").value("2026-06-10"))
                .andExpect(jsonPath("$[?(@.title=='CalCo Drive' && @.kind=='OA')].date").value("2026-06-12"));

        mvc.perform(get("/api/drives/events").param("from", "2026-01-01").param("to", "2026-12-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void companyPageShowsSeededStatsWithSource() throws Exception {
        mvc.perform(get("/api/companies/jpmorgan-chase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placements[0].academicYear").value("2025-26"))
                .andExpect(jsonPath("$.placements[0].ctcLabel").value("19.8 LPA"))
                .andExpect(jsonPath("$.placements[0].sourceNote").value("Source: CBIT public placement records (approx.)"))
                .andExpect(jsonPath("$.internships[0].stipendInrMonth").value(75000))
                .andExpect(jsonPath("$.aliases", hasItem("JPMC")));
        mvc.perform(get("/api/companies/infosys"))
                .andExpect(jsonPath("$.placements[0].ctcLabel").value("up to 9.5 LPA"));
        mvc.perform(get("/api/companies").param("size", "3"))
                .andExpect(jsonPath("$.content[0].slug").value("super-money"))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(22)));
    }

    @Test
    void adminEditsCompanyPrepGuide() throws Exception {
        AppUser admin = users.admin();
        mvc.perform(post("/api/admin/companies").with(users.as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Prep & Co\",\"prepMarkdown\":\"## Focus on DP\",\"aliases\":[\"PrepCo\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("prep-and-co"));
        mvc.perform(get("/api/companies/prep-and-co")).andExpect(jsonPath("$.prepMarkdown").value("## Focus on DP"));
    }
}
