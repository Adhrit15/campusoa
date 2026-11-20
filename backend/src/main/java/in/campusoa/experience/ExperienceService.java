package in.campusoa.experience;

import in.campusoa.common.ApiException;
import in.campusoa.common.ContentStatus;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.common.Viewer;
import in.campusoa.common.Votes;
import in.campusoa.company.Company;
import in.campusoa.company.CompanyRepository;
import in.campusoa.experience.ExperienceDtos.CommentDto;
import in.campusoa.experience.ExperienceDtos.CommentRequest;
import in.campusoa.experience.ExperienceDtos.CompanyRef;
import in.campusoa.experience.ExperienceDtos.ExperienceDetailDto;
import in.campusoa.experience.ExperienceDtos.ExperienceRequest;
import in.campusoa.experience.ExperienceDtos.ExperienceSummaryDto;
import in.campusoa.experience.ExperienceDtos.RoundDto;
import in.campusoa.question.QuestionService;
import in.campusoa.user.AppUser;
import in.campusoa.user.AppUserRepository;
import in.campusoa.user.UserDtos.AuthorDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Interview experiences. The anonymity rule lives in ONE place ({@link #authorFor}), so every
 * endpoint that shows an author applies it the same way.
 */
@Service
public class ExperienceService {

    private final InterviewExperienceRepository experiences;
    private final CommentRepository comments;
    private final ExperienceQueries queries;
    private final CompanyRepository companies;
    private final AppUserRepository users;
    private final QuestionService questions;
    private final Votes votes;

    public ExperienceService(InterviewExperienceRepository experiences, CommentRepository comments,
                             ExperienceQueries queries, CompanyRepository companies, AppUserRepository users,
                             QuestionService questions, Votes votes) {
        this.experiences = experiences;
        this.comments = comments;
        this.queries = queries;
        this.companies = companies;
        this.users = users;
        this.questions = questions;
        this.votes = votes;
    }

    // ---------- reads ----------

    @Transactional(readOnly = true)
    public PageResponse<ExperienceSummaryDto> search(ExperienceQueries.Filter filter, PageParams page, Viewer viewer) {
        var rows = queries.search(filter, page);
        Set<Long> mine = votes.mine(Votes.Target.EXPERIENCE, rows.stream().map(ExperienceQueries.Row::id).toList(),
                viewer.userId());
        var content = rows.stream().map(r -> {
            AuthorDto author = r.authorId() == null ? null
                    : new AuthorDto(r.authorId(), r.authorName(), r.authorAvatar(), r.authorBranch(), r.authorYear());
            return new ExperienceSummaryDto(r.id(), new CompanyRef(r.companyId(), r.companySlug(), r.companyName()),
                    r.year(), r.roleTitle(), r.difficulty(), r.outcome(), r.anonymous(),
                    authorFor(r.anonymous(), r.authorId(), author, viewer),
                    r.excerpt(), r.votes(), r.comments(), mine.contains(r.id()), r.createdAt());
        }).toList();
        return PageResponse.of(content, page.page(), page.size(), queries.count(filter));
    }

    @Transactional(readOnly = true)
    public ExperienceDetailDto detail(long id, Viewer viewer) {
        InterviewExperience e = experiences.findById(id).orElseThrow(() -> ApiException.notFound("Experience"));
        // Non-public posts are visible only to their author and moderators. Everyone else gets 404
        // (not 403), so we don't even reveal that the post exists.
        if (e.getStatus() != ContentStatus.PUBLISHED && !viewer.canSeePrivateDetailsOf(e.getAuthorId())) {
            throw ApiException.notFound("Experience");
        }
        Company c = companies.findById(e.getCompanyId()).orElseThrow();
        AuthorDto author = e.getAuthorId() == null ? null
                : users.findById(e.getAuthorId()).map(AuthorDto::of).orElse(null);
        long voteCount = votes.counts(Votes.Target.EXPERIENCE, List.of(id)).getOrDefault(id, 0L);
        return new ExperienceDetailDto(e.getId(), new CompanyRef(c.getId(), c.getSlug(), c.getName()),
                e.getInterviewYear(), e.getRoleTitle(), e.getDifficulty(), e.getOutcome(), e.isAnonymous(),
                authorFor(e.isAnonymous(), e.getAuthorId(), author, viewer), e.getBodyMd(),
                e.getRounds().stream().map(r -> new RoundDto(r.getPosition(), r.getName(), r.getDescription())).toList(),
                questions.forExperience(id, viewer), voteCount,
                votes.mine(Votes.Target.EXPERIENCE, List.of(id), viewer.userId()).contains(id),
                viewer.is(e.getAuthorId()), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt());
    }

    /** The anonymity rule: hide the author of an anonymous post from everyone but the author and moderators. */
    static AuthorDto authorFor(boolean anonymous, Long authorId, AuthorDto author, Viewer viewer) {
        if (!anonymous) return author;
        return viewer.canSeePrivateDetailsOf(authorId) ? author : null;
    }

    // ---------- writes ----------

    @Transactional
    public long create(long authorId, ExperienceRequest req) {
        requireCompany(req.companyId());
        InterviewExperience e = new InterviewExperience(authorId);
        apply(e, req);
        experiences.save(e);
        if (req.questions() != null && !req.questions().isEmpty()) {
            questions.createAll(authorId, req.companyId(), e.getId(), req.questions());
        }
        return e.getId();
    }

    @Transactional
    public void update(long id, Viewer viewer, ExperienceRequest req) {
        InterviewExperience e = ownedBy(id, viewer);
        requireCompany(req.companyId());
        apply(e, req);
        // (Questions are edited separately; re-posting them here would duplicate the bank.)
    }

    @Transactional
    public void delete(long id, Viewer viewer) {
        experiences.delete(ownedBy(id, viewer));
    }

    @Transactional
    public void vote(long id, long userId, boolean up) {
        InterviewExperience e = experiences.findById(id)
                .filter(x -> x.getStatus() == ContentStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Experience"));
        if (up && Long.valueOf(userId).equals(e.getAuthorId())) {
            throw ApiException.badRequest("OWN_POST", "You can't upvote your own post");
        }
        if (up) votes.add(Votes.Target.EXPERIENCE, id, userId);
        else votes.remove(Votes.Target.EXPERIENCE, id, userId);
    }

    // ---------- comments ----------

    @Transactional(readOnly = true)
    public PageResponse<CommentDto> comments(long experienceId, PageParams page, Viewer viewer) {
        detail(experienceId, viewer);   // same visibility rules as the post itself
        Set<ContentStatus> visible = viewer.moderator() ? EnumSet.allOf(ContentStatus.class) : EnumSet.of(ContentStatus.PUBLISHED);
        var p = comments.findByExperienceIdAndStatusInOrderByCreatedAtAsc(experienceId, visible,
                PageRequest.of(page.page(), page.size()));
        Map<Long, AppUser> authors = users.findAllById(p.getContent().stream().map(Comment::getAuthorId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return PageResponse.of(p, c -> new CommentDto(c.getId(),
                c.getAuthorId() == null || !authors.containsKey(c.getAuthorId()) ? null : AuthorDto.of(authors.get(c.getAuthorId())),
                c.getBody(), viewer.is(c.getAuthorId()), c.getCreatedAt()));
    }

    @Transactional
    public long addComment(long experienceId, long authorId, CommentRequest req) {
        experiences.findById(experienceId).filter(x -> x.getStatus() == ContentStatus.PUBLISHED)
                .orElseThrow(() -> ApiException.notFound("Experience"));
        return comments.save(new Comment(experienceId, authorId, req.body().trim())).getId();
    }

    @Transactional
    public void deleteComment(long commentId, Viewer viewer) {
        Comment c = comments.findById(commentId).orElseThrow(() -> ApiException.notFound("Comment"));
        if (!viewer.is(c.getAuthorId())) throw ApiException.forbidden("Only the author can delete this comment");
        comments.delete(c);
    }

    // ---------- helpers ----------

    private InterviewExperience ownedBy(long id, Viewer viewer) {
        InterviewExperience e = experiences.findById(id).orElseThrow(() -> ApiException.notFound("Experience"));
        if (!viewer.is(e.getAuthorId())) throw ApiException.forbidden("Only the author can change this post");
        return e;
    }

    private void requireCompany(long companyId) {
        if (!companies.existsById(companyId)) {
            throw ApiException.badRequest("UNKNOWN_COMPANY", "Unknown company id " + companyId);
        }
    }

    private static void apply(InterviewExperience e, ExperienceRequest req) {
        e.update(req.companyId(), req.year(), req.roleTitle().trim(), req.difficulty(), req.outcome(),
                req.bodyMd().trim(), req.anonymous());
        List<ExperienceRound> rounds = new ArrayList<>();
        if (req.rounds() != null) {
            short pos = 1;
            for (var r : req.rounds()) rounds.add(new ExperienceRound(pos++, r.name().trim(), r.description()));
        }
        e.replaceRounds(rounds);
    }
}
