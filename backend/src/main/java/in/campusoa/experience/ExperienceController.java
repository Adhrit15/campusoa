package in.campusoa.experience;

import in.campusoa.auth.AppUserPrincipal;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.common.Viewer;
import in.campusoa.experience.ExperienceDtos.CommentDto;
import in.campusoa.experience.ExperienceDtos.CommentRequest;
import in.campusoa.experience.ExperienceDtos.ExperienceDetailDto;
import in.campusoa.experience.ExperienceDtos.ExperienceRequest;
import in.campusoa.experience.ExperienceDtos.ExperienceSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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

@RestController
@Tag(name = "Interview experiences")
public class ExperienceController {

    private final ExperienceService service;

    public ExperienceController(ExperienceService service) {
        this.service = service;
    }

    @GetMapping("/api/experiences")
    @Operation(summary = "Search experiences (q = full-text; company, year, outcome; sort=relevance|new|top)")
    public PageResponse<ExperienceSummaryDto> search(@AuthenticationPrincipal AppUserPrincipal me,
                                                     @RequestParam(required = false) String q,
                                                     @RequestParam(required = false) String company,
                                                     @RequestParam(required = false) Short year,
                                                     @RequestParam(required = false) InterviewExperience.Outcome outcome,
                                                     @RequestParam(required = false) String sort,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        var filter = new ExperienceQueries.Filter(q, company == null || company.isBlank() ? null : company, year, outcome, sort);
        return service.search(filter, new PageParams(page, size), Viewer.of(me));
    }

    @GetMapping("/api/experiences/{id}")
    public ExperienceDetailDto detail(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        return service.detail(id, Viewer.of(me));
    }

    @PostMapping("/api/experiences")
    @Operation(summary = "Post an experience (optionally anonymous) with rounds and tagged questions")
    public ResponseEntity<ExperienceDetailDto> create(@AuthenticationPrincipal AppUserPrincipal me,
                                                      @Valid @RequestBody ExperienceRequest body) {
        long id = service.create(me.userId(), body);
        return ResponseEntity.created(URI.create("/api/experiences/" + id)).body(service.detail(id, Viewer.of(me)));
    }

    @PutMapping("/api/experiences/{id}")
    public ExperienceDetailDto update(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                                      @Valid @RequestBody ExperienceRequest body) {
        service.update(id, Viewer.of(me), body);
        return service.detail(id, Viewer.of(me));
    }

    @DeleteMapping("/api/experiences/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        service.delete(id, Viewer.of(me));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/experiences/{id}/vote")
    public ResponseEntity<Void> vote(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        service.vote(id, me.userId(), true);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/experiences/{id}/vote")
    public ResponseEntity<Void> unvote(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        service.vote(id, me.userId(), false);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/experiences/{id}/comments")
    public PageResponse<CommentDto> comments(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return service.comments(id, new PageParams(page, size), Viewer.of(me));
    }

    @PostMapping("/api/experiences/{id}/comments")
    public ResponseEntity<Void> addComment(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                                           @Valid @RequestBody CommentRequest body) {
        long commentId = service.addComment(id, me.userId(), body);
        return ResponseEntity.created(URI.create("/api/experiences/" + id + "/comments#" + commentId)).build();
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long commentId) {
        service.deleteComment(commentId, Viewer.of(me));
        return ResponseEntity.noContent().build();
    }
}
