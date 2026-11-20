package in.campusoa.user;

import in.campusoa.auth.AppUserPrincipal;
import in.campusoa.user.UserDtos.BranchDto;
import in.campusoa.user.UserDtos.MeDto;
import in.campusoa.user.UserDtos.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controllers are thin: they translate HTTP to a service call and back. No business rules here.
 * {@code @AuthenticationPrincipal} injects the signed-in user from the security context.
 */
@RestController
@Tag(name = "Me & profile")
public class MeController {

    private final UserService users;
    private final BranchRepository branches;

    public MeController(UserService users, BranchRepository branches) {
        this.users = users;
        this.branches = branches;
    }

    @GetMapping("/api/me")
    @Operation(summary = "The signed-in user, including their own private fields (401 if signed out)")
    public MeDto me(@AuthenticationPrincipal AppUserPrincipal me) {
        return users.me(me.userId());
    }

    @PutMapping("/api/me/profile")
    @Operation(summary = "Update branch, graduation year, private CGPA/backlogs and leaderboard opt-in")
    public MeDto updateProfile(@AuthenticationPrincipal AppUserPrincipal me,
                               @Valid @RequestBody UpdateProfileRequest body) {
        return users.updateProfile(me.userId(), body);
    }

    @DeleteMapping("/api/me")
    @Operation(summary = "Permanently delete my account (posts are kept as 'deleted user')")
    public ResponseEntity<Void> deleteMe(@AuthenticationPrincipal AppUserPrincipal me, HttpServletRequest req) {
        users.deleteAccount(me.userId());
        HttpSession session = req.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/branches")
    @Operation(summary = "All branches (public)")
    public List<BranchDto> branches() {
        return branches.findAllByOrderBySortOrder().stream().map(BranchDto::of).toList();
    }
}
