package in.campusoa.moderation;

import in.campusoa.common.ApiException;
import in.campusoa.common.ContentStatus;
import in.campusoa.common.PageParams;
import in.campusoa.common.PageResponse;
import in.campusoa.config.AppProperties;
import in.campusoa.moderation.ModerationDtos.ActionDto;
import in.campusoa.moderation.ModerationDtos.Decision;
import in.campusoa.moderation.ModerationDtos.QueueItemDto;
import in.campusoa.moderation.ModerationDtos.ReportRequest;
import in.campusoa.moderation.ModerationDtos.ResolveRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Post-moderation: content goes live immediately, anyone can report it, and once
 * {@code auto-hide-report-threshold} (3) different users have reported something it is hidden
 * automatically until a moderator decides. Every decision is written to the audit log.
 */
@Service
public class ModerationService {

    private final ReportRepository reports;
    private final ModerationActionRepository actions;
    private final ModerationQueries queries;
    private final int autoHideThreshold;

    public ModerationService(ReportRepository reports, ModerationActionRepository actions, ModerationQueries queries,
                             AppProperties props) {
        this.reports = reports;
        this.actions = actions;
        this.queries = queries;
        this.autoHideThreshold = props.moderation().autoHideReportThreshold();
    }

    @Transactional
    public void report(long reporterId, ReportRequest req) {
        var target = queries.target(req.targetType(), req.targetId())
                .filter(t -> t.status() != ContentStatus.REMOVED)
                .orElseThrow(() -> ApiException.notFound("Reported content"));
        if (Long.valueOf(reporterId).equals(target.authorId())) {
            throw ApiException.badRequest("OWN_POST", "You can't report your own post (delete it instead)");
        }
        try {
            reports.saveAndFlush(new Report(reporterId, req.targetType(), req.targetId(), req.reason(), req.details()));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("ALREADY_REPORTED", "You've already reported this");
        }
        if (target.status() == ContentStatus.PUBLISHED
                && queries.openReportsFor(req.targetType(), req.targetId()) >= autoHideThreshold) {
            queries.setStatus(req.targetType(), req.targetId(), ContentStatus.HIDDEN);
            actions.save(new ModerationAction(null, ModerationAction.Action.AUTO_HIDE, req.targetType(), req.targetId(),
                    autoHideThreshold + " reports"));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<QueueItemDto> queue(String status, PageParams page) {
        return PageResponse.of(queries.queue(status, page), page.page(), page.size(), queries.queueCount(status));
    }

    @Transactional(readOnly = true)
    public PageResponse<ActionDto> auditLog(PageParams page) {
        return PageResponse.of(queries.actions(page), page.page(), page.size(), queries.actionCount());
    }

    /** Apply a moderator's decision to the reported content and close every open report about it. */
    @Transactional
    public void resolve(long moderatorId, long reportId, ResolveRequest req) {
        Report r = reports.findById(reportId).orElseThrow(() -> ApiException.notFound("Report"));
        ContentTarget t = r.target();
        long id = r.targetId();
        switch (req.decision()) {
            case DISMISS -> {
                // The report was unfounded. If it had been auto-hidden, put it back.
                queries.target(t, id).filter(x -> x.status() == ContentStatus.HIDDEN)
                        .ifPresent(x -> queries.setStatus(t, id, ContentStatus.PUBLISHED));
            }
            case HIDE -> queries.setStatus(t, id, ContentStatus.HIDDEN);
            case REMOVE -> queries.setStatus(t, id, ContentStatus.REMOVED);
            case RESTORE -> queries.setStatus(t, id, ContentStatus.PUBLISHED);
        }
        queries.closeOpenReports(t, id, req.decision() == Decision.DISMISS || req.decision() == Decision.RESTORE
                ? "DISMISSED" : "RESOLVED", moderatorId, req.note());
        actions.save(new ModerationAction(moderatorId, ModerationAction.Action.valueOf(req.decision().name()), t, id, req.note()));
    }
}
