package in.campusoa.user;

import in.campusoa.auth.EmailDomainPolicy;
import in.campusoa.auth.LoginRejectedException;
import in.campusoa.auth.SessionRevoker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test: no Spring, no database. Mockito "mocks" stand in for the repositories, so we test
 * only UserService's own rules, in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock AppUserRepository users;
    @Mock PrivateProfileRepository profiles;
    @Mock BranchRepository branches;
    @Mock EmailDomainPolicy policy;
    @Mock SessionRevoker revoker;

    UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(users, profiles, branches, policy, revoker, Clock.systemUTC());
    }

    private static UserService.GoogleProfile google(String email, boolean verified) {
        return new UserService.GoogleProfile("sub-1", email, verified, "Asha", "https://pic");
    }

    @Test
    void rejectsUnverifiedEmail() {
        assertThatThrownBy(() -> service.registerLogin(google("a@cbit.edu", false)))
                .isInstanceOf(LoginRejectedException.class)
                .extracting(e -> ((LoginRejectedException) e).code()).isEqualTo("email_unverified");
    }

    @Test
    void rejectsDomainNotOnAllowList() {
        when(policy.isAllowed("a@gmail.com")).thenReturn(false);
        assertThatThrownBy(() -> service.registerLogin(google("a@gmail.com", true)))
                .extracting(e -> ((LoginRejectedException) e).code()).isEqualTo("domain_not_allowed");
        verify(users, never()).save(any());
    }

    @Test
    void createsNewStudentOnFirstLogin() {
        when(policy.isAllowed("a@cbit.edu")).thenReturn(true);
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AppUser u = service.registerLogin(google("A@CBIT.edu", true));

        assertThat(u.getEmail()).isEqualTo("a@cbit.edu");   // normalised to lowercase
        assertThat(u.getRole()).isEqualTo(Role.STUDENT);
        assertThat(u.isLeaderboardOptIn()).isFalse();        // privacy default
    }

    @Test
    void promotesBootstrapAdmin() {
        when(policy.isAllowed("boss@cbit.edu")).thenReturn(true);
        when(policy.isBootstrapAdmin("boss@cbit.edu")).thenReturn(true);
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.empty());
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.registerLogin(google("boss@cbit.edu", true)).getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void rejectsBannedUser() {
        AppUser banned = new AppUser("a@cbit.edu", "sub-1", "Asha");
        banned.setBanned(true);
        when(policy.isAllowed("a@cbit.edu")).thenReturn(true);
        when(users.findByGoogleSub("sub-1")).thenReturn(Optional.of(banned));

        assertThatThrownBy(() -> service.registerLogin(google("a@cbit.edu", true)))
                .extracting(e -> ((LoginRejectedException) e).code()).isEqualTo("account_banned");
    }
}
