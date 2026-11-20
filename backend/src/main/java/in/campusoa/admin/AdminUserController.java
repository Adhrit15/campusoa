package in.campusoa.admin;

import in.campusoa.auth.AppUserPrincipal;
import in.campusoa.auth.SessionRevoker;
import in.campusoa.common.ApiException;
import in.campusoa.common.PageResponse;
import in.campusoa.user.AppUser;
import in.campusoa.user.AppUserRepository;
import in.campusoa.user.Role;
import in.campusoa.user.UserDtos.AdminUserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only user management. Protected twice: the URL rule in SecurityConfig ("/api/admin/**"
 * needs ADMIN) AND @PreAuthorize here, so a future URL refactor can't silently open it.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: users")
public class AdminUserController {

    private final AppUserRepository users;
    private final SessionRevoker sessions;

    public AdminUserController(AppUserRepository users, SessionRevoker sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    public record RoleRequest(@NotNull Role role) {}

    public record BanRequest(boolean banned) {}

    @GetMapping
    @Operation(summary = "Search users by email or name")
    public PageResponse<AdminUserDto> list(@RequestParam(required = false) String q,
                                           @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC)
                                           Pageable pageable) {
        String term = (q == null || q.isBlank()) ? null : q.trim();
        return PageResponse.of(users.search(term, pageable), AdminUserDto::of);
    }

    @PutMapping("/{id}/role")
    @Transactional
    @Operation(summary = "Change a user's role (they are signed out so the new role takes effect)")
    public AdminUserDto setRole(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                                @Valid @RequestBody RoleRequest body) {
        if (id == me.userId()) throw ApiException.badRequest("SELF_ROLE_CHANGE", "You can't change your own role");
        AppUser u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setRole(body.role());
        sessions.revokeAll(id);
        return AdminUserDto.of(u);
    }

    @PutMapping("/{id}/ban")
    @Transactional
    @Operation(summary = "Ban or unban a user (banning signs them out everywhere)")
    public AdminUserDto setBan(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                               @RequestBody BanRequest body) {
        if (id == me.userId()) throw ApiException.badRequest("SELF_BAN", "You can't ban yourself");
        AppUser u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        u.setBanned(body.banned());
        if (body.banned()) sessions.revokeAll(id);
        return AdminUserDto.of(u);
    }
}
