package in.campusoa.moderation;

import in.campusoa.common.ContentStatus;
import in.campusoa.common.PageParams;
import in.campusoa.moderation.ModerationDtos.ActionDto;
import in.campusoa.moderation.ModerationDtos.QueueItemDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** SQL for the moderation queue and for changing a target's status. */
@Repository
public class ModerationQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public ModerationQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Current status and author of the reported content, or empty if it doesn't exist. */
    public record TargetInfo(ContentStatus status, Long authorId) {}

    public Optional<TargetInfo> target(ContentTarget t, long id) {
        var rows = jdbc.query("SELECT status, author_id FROM " + t.table + " WHERE id = :id",
                new MapSqlParameterSource("id", id),
                (rs, i) -> new TargetInfo(ContentStatus.valueOf(rs.getString("status")),
                        rs.getObject("author_id", Long.class)));
        return rows.stream().findFirst();
    }

    public void setStatus(ContentTarget t, long id, ContentStatus status) {
        jdbc.update("UPDATE " + t.table + " SET status = :status WHERE id = :id",
                new MapSqlParameterSource("status", status.name()).addValue("id", id));
    }

    public long openReportsFor(ContentTarget t, long id) {
        return jdbc.queryForObject("SELECT count(*) FROM report WHERE status = 'OPEN' AND " + t.reportColumn + " = :id",
                new MapSqlParameterSource("id", id), Long.class);
    }

    /** Closes every open report about the target, when a moderator decides. */
    public int closeOpenReports(ContentTarget t, long id, String newStatus, long moderatorId, String note) {
        return jdbc.update("""
                UPDATE report SET status = :st, resolved_by = :mod, resolved_at = now(), resolution_note = :note
                WHERE status = 'OPEN' AND\s""" + t.reportColumn + " = :id",
                new MapSqlParameterSource("st", newStatus).addValue("mod", moderatorId).addValue("note", note)
                        .addValue("id", id));
    }

    /**
     * The queue: one row per report, with a preview of the reported text pulled from whichever
     * table the report points at. COALESCE picks the first non-NULL of the three LEFT JOINs.
     */
    public List<QueueItemDto> queue(String status, PageParams page) {
        String sql = """
                SELECT r.id, r.reason, r.details, r.created_at,
                       CASE WHEN r.experience_id IS NOT NULL THEN 'EXPERIENCE'
                            WHEN r.question_id IS NOT NULL THEN 'QUESTION' ELSE 'COMMENT' END AS target_type,
                       coalesce(r.experience_id, r.question_id, r.comment_id) AS target_id,
                       coalesce(e.id, q.experience_id, cm.experience_id) AS experience_link,
                       coalesce(left(e.role_title || ': ' || e.body_md, 300), left(q.body_md, 300), left(cm.body, 300)) AS preview,
                       coalesce(e.status, q.status, cm.status) AS target_status,
                       ru.display_name AS reporter_name,
                       (SELECT count(*) FROM report r2 WHERE r2.status = 'OPEN'
                          AND (r2.experience_id = r.experience_id OR r2.question_id = r.question_id
                               OR r2.comment_id = r.comment_id)) AS open_for_target
                FROM report r
                LEFT JOIN interview_experience e ON e.id = r.experience_id
                LEFT JOIN question q ON q.id = r.question_id
                LEFT JOIN comment cm ON cm.id = r.comment_id
                LEFT JOIN app_user ru ON ru.id = r.reporter_id
                WHERE r.status = :status
                ORDER BY r.created_at
                LIMIT :limit OFFSET :offset""";
        return jdbc.query(sql, new MapSqlParameterSource("status", status).addValue("limit", page.size())
                        .addValue("offset", page.offset()),
                (rs, i) -> new QueueItemDto(rs.getLong("id"), ContentTarget.valueOf(rs.getString("target_type")),
                        rs.getLong("target_id"), rs.getObject("experience_link", Long.class), rs.getString("preview"),
                        rs.getString("target_status") == null ? null : ContentStatus.valueOf(rs.getString("target_status")),
                        ModerationDtos.Reason.valueOf(rs.getString("reason")), rs.getString("details"),
                        rs.getString("reporter_name"), rs.getLong("open_for_target"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    public long queueCount(String status) {
        return jdbc.queryForObject("SELECT count(*) FROM report WHERE status = :status",
                new MapSqlParameterSource("status", status), Long.class);
    }

    public List<ActionDto> actions(PageParams page) {
        return jdbc.query("""
                SELECT a.id, u.display_name, a.action, a.note, a.created_at,
                       CASE WHEN a.experience_id IS NOT NULL THEN 'EXPERIENCE'
                            WHEN a.question_id IS NOT NULL THEN 'QUESTION'
                            WHEN a.comment_id IS NOT NULL THEN 'COMMENT' END AS target_type,
                       coalesce(a.experience_id, a.question_id, a.comment_id) AS target_id
                FROM moderation_action a LEFT JOIN app_user u ON u.id = a.moderator_id
                ORDER BY a.created_at DESC LIMIT :limit OFFSET :offset""",
                new MapSqlParameterSource("limit", page.size()).addValue("offset", page.offset()),
                (rs, i) -> new ActionDto(rs.getLong("id"),
                        rs.getString("display_name") == null ? "automatic" : rs.getString("display_name"),
                        rs.getString("action"),
                        rs.getString("target_type") == null ? null : ContentTarget.valueOf(rs.getString("target_type")),
                        rs.getObject("target_id", Long.class), rs.getString("note"),
                        rs.getTimestamp("created_at").toInstant()));
    }

    public long actionCount() {
        return jdbc.getJdbcTemplate().queryForObject("SELECT count(*) FROM moderation_action", Long.class);
    }
}
