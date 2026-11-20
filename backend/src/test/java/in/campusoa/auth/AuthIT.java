package in.campusoa.auth;

import in.campusoa.support.IntegrationTest;
import in.campusoa.support.TestUsers;
import in.campusoa.user.AppUser;
import in.campusoa.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Authentication, authorisation, CSRF and privacy rules, exercised through real HTTP handling. */
class AuthIT extends IntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired TestUsers users;
    @Autowired AppUserRepository userRepo;

    private static final String PROFILE = """
            {"displayName":"Asha","branchCode":"CSE","gradYear":2028,"cgpa":8.4,"activeBacklogs":0,
             "leaderboardOptIn":true}""";

    @Test
    void anonymousGets401JsonOnPrivateEndpoint() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void anonymousCanReadPublicLists() throws Exception {
        mvc.perform(get("/api/branches")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void loginStartRedirectsToGoogle() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.startsWith("https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.containsString("redirect_uri=http://localhost:5173/login/oauth2/code/google")));
    }

    @Test
    void studentSeesOwnPrivateFields() throws Exception {
        AppUser s = users.student();
        mvc.perform(put("/api/me/profile").with(users.as(s)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cgpa").value(8.4))
                .andExpect(jsonPath("$.profileComplete").value(true));
        mvc.perform(get("/api/me").with(users.as(s)))
                .andExpect(jsonPath("$.branchCode").value("CSE"))
                .andExpect(jsonPath("$.activeBacklogs").value(0));
    }

    @Test
    void writesWithoutCsrfTokenAreRejected() throws Exception {
        AppUser s = users.student();
        mvc.perform(put("/api/me/profile").with(users.as(s))
                        .contentType(MediaType.APPLICATION_JSON).content(PROFILE))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidProfileGivesFieldErrors() throws Exception {
        AppUser s = users.student();
        mvc.perform(put("/api/me/profile").with(users.as(s)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"\",\"cgpa\":11,\"leaderboardOptIn\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void studentCannotReachAdminOrModeratorRoutes() throws Exception {
        AppUser s = users.student();
        mvc.perform(get("/api/admin/users").with(users.as(s))).andExpect(status().isForbidden());
        mvc.perform(get("/api/mod/reports").with(users.as(s))).andExpect(status().isForbidden());
    }

    @Test
    void moderatorCannotReachAdminRoutes() throws Exception {
        mvc.perform(get("/api/admin/users").with(users.as(users.moderator()))).andExpect(status().isForbidden());
    }

    @Test
    void adminListsUsersWithoutPrivateFields() throws Exception {
        AppUser s = users.student();
        mvc.perform(put("/api/me/profile").with(users.as(s)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(PROFILE)).andExpect(status().isOk());

        mvc.perform(get("/api/admin/users").param("q", s.getEmail()).with(users.as(users.admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(s.getEmail()))
                .andExpect(jsonPath("$.content[0].cgpa").doesNotExist())
                .andExpect(jsonPath("$.content[0].activeBacklogs").doesNotExist());
    }

    @Test
    void adminCanChangeRolesButNotTheirOwn() throws Exception {
        AppUser admin = users.admin();
        AppUser s = users.student();
        mvc.perform(put("/api/admin/users/" + s.getId() + "/role").with(users.as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"MODERATOR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("MODERATOR"));
        mvc.perform(put("/api/admin/users/" + admin.getId() + "/role").with(users.as(admin)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"STUDENT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCanDeleteTheirAccount() throws Exception {
        AppUser s = users.student();
        mvc.perform(delete("/api/me").with(users.as(s)).with(csrf())).andExpect(status().isNoContent());
        assertThat(userRepo.findById(s.getId())).isEmpty();
    }
}
