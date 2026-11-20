package in.campusoa.question;

import in.campusoa.common.PageParams;
import in.campusoa.question.QuestionDtos.TopicDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Question-bank search. Full-text search uses the GIN-indexed {@code search_vector} column:
 * <pre>
 *   q.search_vector @@ websearch_to_tsquery('english', :q)
 * </pre>
 * {@code websearch_to_tsquery} accepts Google-like input ({@code "linked list" -array or dp})
 * and stems words with the English dictionary, so "graphs" also matches "graph".
 * {@code ts_rank} orders the matches by how well they fit.
 */
@Repository
public class QuestionQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public QuestionQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Filter(String q, String topicCode, Question.Difficulty difficulty, String companySlug,
                         Long experienceId, String sort) {}

    public record Row(long id, String bodyMd, Question.Difficulty difficulty, String status, Instant createdAt,
                      Long experienceId, Long authorId, boolean anonymous, String authorName, String authorAvatar,
                      String authorBranch, Short authorYear, Long companyId, String companySlug, String companyName,
                      long votes) {}

    private record Sql(String where, MapSqlParameterSource p, boolean hasQuery) {}

    private static Sql where(Filter f) {
        var p = new MapSqlParameterSource();
        StringBuilder w = new StringBuilder(" WHERE q.status = 'PUBLISHED' AND (e.id IS NULL OR e.status = 'PUBLISHED')");
        boolean hasQuery = f.q() != null && !f.q().isBlank();
        if (hasQuery) {
            w.append(" AND q.search_vector @@ websearch_to_tsquery('english', :q)");
            p.addValue("q", f.q().trim());
        }
        if (f.topicCode() != null) {
            // matches the topic itself OR any of its sub-topics ("dsa" includes "dsa-graphs")
            w.append(" AND EXISTS (SELECT 1 FROM question_topic qt JOIN topic t ON t.id = qt.topic_id"
                    + " LEFT JOIN topic parent ON parent.id = t.parent_id"
                    + " WHERE qt.question_id = q.id AND (t.code = :topic OR parent.code = :topic))");
            p.addValue("topic", f.topicCode());
        }
        if (f.difficulty() != null) {
            w.append(" AND q.difficulty = :difficulty");
            p.addValue("difficulty", f.difficulty().name());
        }
        if (f.companySlug() != null) {
            w.append(" AND c.slug = :company");
            p.addValue("company", f.companySlug());
        }
        if (f.experienceId() != null) {
            w.append(" AND q.experience_id = :experience");
            p.addValue("experience", f.experienceId());
        }
        return new Sql(w.toString(), p, hasQuery);
    }

    private static final String FROM = """
             FROM question q
            LEFT JOIN interview_experience e ON e.id = q.experience_id
            LEFT JOIN company c ON c.id = q.company_id
            LEFT JOIN app_user u ON u.id = q.author_id""";

    public List<Row> search(Filter f, PageParams page) {
        Sql s = where(f);
        String order = switch (f.sort() == null ? "" : f.sort()) {
            case "top" -> "votes DESC, q.created_at DESC";
            case "new" -> "q.created_at DESC";
            default -> s.hasQuery()
                    ? "ts_rank(q.search_vector, websearch_to_tsquery('english', :q)) DESC, q.created_at DESC"
                    : "q.created_at DESC";
        };
        String sql = """
                SELECT q.id, q.body_md, q.difficulty, q.status, q.created_at, q.experience_id, q.author_id,
                       coalesce(e.anonymous, false) AS anonymous,
                       u.display_name, u.avatar_url, u.branch_code, u.grad_year,
                       c.id AS company_id, c.slug, c.name,
                       (SELECT count(*) FROM question_vote v WHERE v.question_id = q.id) AS votes"""
                + FROM + s.where() + " ORDER BY " + order + " LIMIT :limit OFFSET :offset";
        s.p().addValue("limit", page.size()).addValue("offset", page.offset());
        return jdbc.query(sql, s.p(), (rs, i) -> new Row(
                rs.getLong("id"), rs.getString("body_md"), Question.Difficulty.valueOf(rs.getString("difficulty")),
                rs.getString("status"), rs.getTimestamp("created_at").toInstant(),
                rs.getObject("experience_id", Long.class), rs.getObject("author_id", Long.class),
                rs.getBoolean("anonymous"), rs.getString("display_name"), rs.getString("avatar_url"),
                rs.getString("branch_code"), rs.getObject("grad_year", Short.class),
                rs.getObject("company_id", Long.class), rs.getString("slug"), rs.getString("name"),
                rs.getLong("votes")));
    }

    public long count(Filter f) {
        Sql s = where(f);
        return jdbc.queryForObject("SELECT count(*)" + FROM + s.where(), s.p(), Long.class);
    }

    /** Topics for a page of questions, in one query. */
    public Map<Long, List<TopicDto>> topicsFor(Collection<Long> questionIds) {
        Map<Long, List<TopicDto>> out = new HashMap<>();
        if (questionIds.isEmpty()) return out;
        jdbc.query("""
                SELECT qt.question_id, t.code, t.name, parent.code AS parent_code
                FROM question_topic qt
                JOIN topic t ON t.id = qt.topic_id
                LEFT JOIN topic parent ON parent.id = t.parent_id
                WHERE qt.question_id IN (:ids)
                ORDER BY t.sort_order""", new MapSqlParameterSource("ids", questionIds), rs -> {
            out.computeIfAbsent(rs.getLong("question_id"), k -> new ArrayList<>())
                    .add(new TopicDto(rs.getString("code"), rs.getString("name"), rs.getString("parent_code")));
        });
        return out;
    }

    static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
