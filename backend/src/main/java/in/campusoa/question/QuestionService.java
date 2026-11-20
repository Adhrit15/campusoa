package in.campusoa.question;

import in.campusoa.common.ApiException;
import in.campusoa.common.ContentStatus;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.common.Viewer;
import in.campusoa.common.Votes;
import in.campusoa.company.CompanyRepository;
import in.campusoa.question.QuestionDtos.CompanyRef;
import in.campusoa.question.QuestionDtos.QuestionDto;
import in.campusoa.question.QuestionDtos.QuestionRequest;
import in.campusoa.question.QuestionDtos.TopicDto;
import in.campusoa.user.UserDtos.AuthorDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questions;
    private final TopicRepository topics;
    private final QuestionQueries queries;
    private final CompanyRepository companies;
    private final Votes votes;

    public QuestionService(QuestionRepository questions, TopicRepository topics, QuestionQueries queries,
                           CompanyRepository companies, Votes votes) {
        this.questions = questions;
        this.topics = topics;
        this.queries = queries;
        this.companies = companies;
        this.votes = votes;
    }

    @Transactional(readOnly = true)
    public List<TopicDto> allTopics() {
        List<Topic> all = topics.findAllByOrderBySortOrder();
        Map<Long, String> codeById = all.stream().collect(Collectors.toMap(Topic::getId, Topic::getCode));
        return all.stream().map(t -> new TopicDto(t.getCode(), t.getName(),
                t.getParentId() == null ? null : codeById.get(t.getParentId()))).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<QuestionDto> search(QuestionQueries.Filter filter, PageParams page, Viewer viewer) {
        var rows = queries.search(filter, page);
        return PageResponse.of(toDtos(rows, viewer), page.page(), page.size(), queries.count(filter));
    }

    /** Questions attached to one experience (shown on its page). */
    @Transactional(readOnly = true)
    public List<QuestionDto> forExperience(long experienceId, Viewer viewer) {
        var filter = new QuestionQueries.Filter(null, null, null, null, experienceId, "new");
        return toDtos(queries.search(filter, new PageParams(0, PageParams.MAX_SIZE)), viewer);
    }

    @Transactional
    public long create(long authorId, QuestionRequest req) {
        return createAll(authorId, req.companyId(), null, List.of(req)).get(0);
    }

    /** Called by ExperienceService inside ITS transaction, so the experience and its questions commit together. */
    @Transactional
    public List<Long> createAll(long authorId, Long companyId, Long experienceId, List<QuestionRequest> reqs) {
        return reqs.stream().map(r -> {
            Long cid = r.companyId() != null ? r.companyId() : companyId;
            if (cid != null && !companies.existsById(cid)) {
                throw ApiException.badRequest("UNKNOWN_COMPANY", "Unknown company id " + cid);
            }
            Question q = new Question(authorId, cid, experienceId, r.bodyMd().trim(), r.difficulty(), resolveTopics(r.topicCodes()));
            return questions.save(q).getId();
        }).toList();
    }

    @Transactional
    public void vote(long questionId, long userId, boolean up) {
        Question q = questions.findById(questionId)
                .filter(x -> x.getStatus() == ContentStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Question"));
        if (up && Long.valueOf(userId).equals(q.getAuthorId())) {
            throw ApiException.badRequest("OWN_POST", "You can't upvote your own question");
        }
        if (up) votes.add(Votes.Target.QUESTION, questionId, userId);
        else votes.remove(Votes.Target.QUESTION, questionId, userId);
    }

    @Transactional
    public void delete(long questionId, Viewer viewer) {
        Question q = questions.findById(questionId).orElseThrow(() -> ApiException.notFound("Question"));
        if (!viewer.is(q.getAuthorId())) throw ApiException.forbidden("Only the author can delete this question");
        questions.delete(q);
    }

    private Set<Topic> resolveTopics(List<String> codes) {
        Set<String> wanted = new HashSet<>(codes);
        List<Topic> found = topics.findByCodeIn(wanted);
        if (found.size() != wanted.size()) {
            Set<String> known = found.stream().map(Topic::getCode).collect(Collectors.toSet());
            wanted.removeAll(known);
            throw ApiException.badRequest("UNKNOWN_TOPIC", "Unknown topic(s): " + String.join(", ", wanted));
        }
        return new HashSet<>(found);
    }

    private List<QuestionDto> toDtos(List<QuestionQueries.Row> rows, Viewer viewer) {
        var ids = rows.stream().map(QuestionQueries.Row::id).toList();
        var topicMap = queries.topicsFor(ids);
        var mine = votes.mine(Votes.Target.QUESTION, ids, viewer.userId());
        return rows.stream().map(r -> {
            // Anonymous: questions inherit anonymity from the experience they were posted with.
            boolean showAuthor = r.authorId() != null && (!r.anonymous() || viewer.canSeePrivateDetailsOf(r.authorId()));
            AuthorDto author = showAuthor
                    ? new AuthorDto(r.authorId(), r.authorName(), r.authorAvatar(), r.authorBranch(), r.authorYear())
                    : null;
            return new QuestionDto(r.id(), r.bodyMd(), r.difficulty(), topicMap.getOrDefault(r.id(), List.of()),
                    r.companyId() == null ? null : new CompanyRef(r.companyId(), r.companySlug(), r.companyName()),
                    r.experienceId(), author, r.anonymous(), r.votes(), mine.contains(r.id()), r.status(), r.createdAt());
        }).toList();
    }
}
