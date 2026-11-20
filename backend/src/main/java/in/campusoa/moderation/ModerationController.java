package in.campusoa.moderation;

import in.campusoa.auth.AppUserPrincipal;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.moderation.ModerationDtos.ActionDto;
import in.campusoa.moderation.ModerationDtos.QueueItemDto;
import in.campusoa.moderation.ModerationDtos.ReportRequest;
import in.campusoa.moderation.ModerationDtos.ResolveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Moderation")
public class ModerationController {

    private final ModerationService service;

    public ModerationController(ModerationService service) {
        this.service = service;
    }

    @PostMapping("/api/reports")
    @Operation(summary = "Report an experience, question or comment (one report per user per item)")
    public ResponseEntity<Void> report(@AuthenticationPrincipal AppUserPrincipal me, @Valid @RequestBody ReportRequest body) {
        service.report(me.userId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/mod/reports")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Moderation queue (status=OPEN|RESOLVED|DISMISSED), oldest first")
    public PageResponse<QueueItemDto> queue(@RequestParam(defaultValue = "OPEN") Report.Status status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.queue(status.name(), new PageParams(page, size));
    }

    @PostMapping("/api/mod/reports/{id}/resolve")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Decide: DISMISS, HIDE, REMOVE or RESTORE; closes all open reports on that item")
    public ResponseEntity<Void> resolve(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                                        @Valid @RequestBody ResolveRequest body) {
        service.resolve(me.userId(), id, body);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/mod/actions")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Audit log of moderation decisions, newest first")
    public PageResponse<ActionDto> actions(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.auditLog(new PageParams(page, size));
    }
}
