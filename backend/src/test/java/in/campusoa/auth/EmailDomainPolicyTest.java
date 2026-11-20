package in.campusoa.auth;

import in.campusoa.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDomainPolicyTest {

    private final EmailDomainPolicy policy = new EmailDomainPolicy(new AppProperties(
            List.of(" CBIT.example.edu ", "staff.example.edu"), List.of("Boss@cbit.example.edu"),
            "http://x", "http://x", null, null, null, null));

    @ParameterizedTest
    @ValueSource(strings = {"a@cbit.example.edu", "A.B@CBIT.EXAMPLE.EDU", "t@staff.example.edu"})
    void allowsListedDomainsCaseInsensitively(String email) {
        assertThat(policy.isAllowed(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@gmail.com", "a@evil-cbit.example.edu", "a@cbit.example.edu.evil.com",
            "a@sub.cbit.example.edu", "no-at-sign", "@cbit.example.edu", "a@"})
    void rejectsEverythingElse(String email) {
        assertThat(policy.isAllowed(email)).isFalse();
    }

    @Test
    void emptyAllowListFailsClosed() {
        var closed = new EmailDomainPolicy(new AppProperties(List.of(), List.of(), "x", "x", null, null, null, null));
        assertThat(closed.isAllowed("a@cbit.example.edu")).isFalse();
    }

    @Test
    void recognisesBootstrapAdminCaseInsensitively() {
        assertThat(policy.isBootstrapAdmin("boss@CBIT.example.edu")).isTrue();
        assertThat(policy.isBootstrapAdmin("other@cbit.example.edu")).isFalse();
    }
}
