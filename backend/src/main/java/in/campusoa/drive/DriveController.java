package in.campusoa.drive;

import in.campusoa.auth.AppUserPrincipal;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.drive.DriveDtos.CalendarEventDto;
import in.campusoa.drive.DriveDtos.CriteriaDto;
import in.campusoa.drive.DriveDtos.CriteriaRequest;
import in.campusoa.drive.DriveDtos.DriveDetailDto;
import in.campusoa.drive.DriveDtos.DriveRequest;
import in.campusoa.drive.DriveDtos.DriveStageDto;
import in.campusoa.drive.DriveDtos.DriveSummaryDto;
import in.campusoa.drive.DriveDtos.MergeRequest;
import in.campusoa.drive.DriveDtos.StagesRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@Tag(name = "Drives")
public class DriveController {

    private final DriveService service;

    public DriveController(DriveService service) {
        this.service = service;
    }

    /** Signed-out visitors get a null principal: they see drives but no eligibility. */
    private static Long viewer(AppUserPrincipal me) {
        return me == null ? null : me.userId();
    }

    @GetMapping("/api/drives")
    @Operation(summary = "Drive list, newest first. Filters: company (slug), branch, stage, from/to, q")
    public PageResponse<DriveSummaryDto> list(
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) StageType stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var filter = new DriveQueries.Filter(blankToNull(company), blankToNull(branch), stage, from, to, q);
        return service.list(filter, new PageParams(page, size), viewer(me));
    }

    @GetMapping("/api/drives/events")
    @Operation(summary = "Calendar events between from and to (max ~3 months)")
    public List<CalendarEventDto> events(
            @AuthenticationPrincipal AppUserPrincipal me,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.events(from, to, viewer(me));
    }

    @GetMapping("/api/drives/{id}")
    @Operation(summary = "Drive detail with stages, criteria and (if signed in) your eligibility")
    public DriveDetailDto detail(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        return service.detail(id, viewer(me));
    }

    // ---------- admin ----------

    @PostMapping("/api/admin/drives")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add a drive manually", tags = "Admin: drives")
    public ResponseEntity<DriveDetailDto> create(@Valid @RequestBody DriveRequest body) {
        DriveDetailDto d = service.create(body);
        return ResponseEntity.created(URI.create("/api/drives/" + d.id())).body(d);
    }

    @PutMapping("/api/admin/drives/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit a drive (link a company, fix title/date, hide)", tags = "Admin: drives")
    public DriveDetailDto update(@PathVariable long id, @Valid @RequestBody DriveRequest body) {
        return service.update(id, body);
    }

    @PostMapping("/api/admin/drives/{id}/merge")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Merge this drive into another (stages move, this one is hidden)", tags = "Admin: drives")
    public DriveDetailDto merge(@PathVariable long id, @Valid @RequestBody MergeRequest body) {
        return service.merge(id, body.targetDriveId());
    }

    @PutMapping("/api/admin/drives/{id}/criteria")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set eligibility criteria (empty branches = all branches)", tags = "Admin: drives")
    public CriteriaDto putCriteria(@PathVariable long id, @Valid @RequestBody CriteriaRequest body) {
        return service.putCriteria(id, body);
    }

    @DeleteMapping("/api/admin/drives/{id}/criteria")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove criteria (back to 'criteria unknown')", tags = "Admin: drives")
    public ResponseEntity<Void> deleteCriteria(@PathVariable long id) {
        service.deleteCriteria(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/drives/{id}/stages")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Replace the drive's stages (PPT/OA/GD/interview with times)", tags = "Admin: drives")
    public List<DriveStageDto> replaceStages(@PathVariable long id, @Valid @RequestBody StagesRequest body) {
        return service.replaceStages(id, body);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
