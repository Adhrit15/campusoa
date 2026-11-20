package in.campusoa.question;

import in.campusoa.auth.AppUserPrincipal;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.common.Viewer;
import in.campusoa.question.QuestionDtos.QuestionDto;
import in.campusoa.question.QuestionDtos.QuestionRequest;
import in.campusoa.question.QuestionDtos.TopicDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Question bank")
public class QuestionController {

    private final QuestionService service;

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @GetMapping("/api/topics")
    @Operation(summary = "All topics; sub-topics carry their parent's code")
    public List<TopicDto> topics() {
        return service.allTopics();
    }

    @GetMapping("/api/questions")
    @Operation(summary = "Search the question bank (q = full-text, topic, difficulty, company; sort=relevance|new|top)")
    public PageResponse<QuestionDto> search(@AuthenticationPrincipal AppUserPrincipal me,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(required = false) String topic,
                                            @RequestParam(required = false) Question.Difficulty difficulty,
                                            @RequestParam(required = false) String company,
                                            @RequestParam(required = false) String sort,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        var filter = new QuestionQueries.Filter(q, blank(topic), difficulty, blank(company), null, sort);
        return service.search(filter, new PageParams(page, size), Viewer.of(me));
    }

    @PostMapping("/api/questions")
    @Operation(summary = "Add a question to the bank")
    public ResponseEntity<Map<String, Long>> create(@AuthenticationPrincipal AppUserPrincipal me,
                                                    @Valid @RequestBody QuestionRequest body) {
        long id = service.create(me.userId(), body);
        return ResponseEntity.created(URI.create("/api/questions?id=" + id)).body(Map.of("id", id));
    }

    @PostMapping("/api/questions/{id}/vote")
    public ResponseEntity<Void> vote(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        service.vote(id, me.userId(), true);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/questions/{id}/vote")
    public ResponseEntity<Void> unvote(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        service.vote(id, me.userId(), false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/questions/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id) {
        service.delete(id, Viewer.of(me));
        return ResponseEntity.noContent().build();
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
