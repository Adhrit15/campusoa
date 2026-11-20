package in.campusoa.experience;

import in.campusoa.common.PageParams;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Experience search: PostgreSQL full-text search over the generated {@code search_vector}.
 *
 * <pre>
 * SELECT ...,
 *        ts_headline('english', e.body_md, query, 'StartSel=[[, StopSel=]], MaxWords=35, MinWords=15')
 * FROM interview_experience e
 * CROSS JOIN websearch_to_tsquery('english', :q) AS query        -- parse the search text once
 * WHERE e.status = 'PUBLISHED' AND e.search_vector @@ query      -- uses the GIN index
 * ORDER BY ts_rank(e.search_vector, query) DESC                  -- best matches first
 * </pre>
 * {@code ts_headline} returns a snippet around the matching words, wrapped in [[ ]] so the
 * frontend can highlight them without us ever sending HTML.
 */
@Repository
public class ExperienceQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public ExperienceQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Filter(String q, String companySlug, Short year, InterviewExperience.Outcome outcome, String sort) {}

    public record Row(long id, long companyId, String companySlug, String companyName, short year, String roleTitle,
                      short difficulty, InterviewExperience.Outcome outcome, boolean anonymous, Long authorId,
                      String authorName, String authorAvatar, String authorBranch, Short authorYear, String excerpt,
                      long votes, long comments, Instant createdAt) {}

    private record Sql(String from, String where, MapSqlParameterSource p, boolean hasQuery) {}

    private static Sql build(Filter f) {
        var p = new MapSqlParameterSource();
        boolean hasQuery = f.q() != null && !f.q().isBlank();
        String from = """
                 FROM interview_experience e
                JOIN company c ON c.id = e.company_id
                LEFT JOIN app_user u ON u.id = e.author_id""";
        StringBuilder w = new StringBuilder(" WHERE e.status = 'PUBLISHED'");
        if (hasQuery) {
            from += " CROSS JOIN websearch_to_tsquery('english', :q) AS query";
            w.append(" AND e.search_vector @@ query");
            p.addValue("q", f.q().trim());
        }
        if (f.companySlug() != null) {
            w.append(" AND c.slug = :company");
            p.addValue("company", f.companySlug());
        }
        if (f.year() != null) {
            w.append(" AND e.interview_year = :year");
            p.addValue("year", f.year());
        }
        if (f.outcome() != null) {
            w.append(" AND e.outcome = :outcome");
            p.addValue("outcome", f.outcome().name());
        }
        return new Sql(from, w.toString(), p, hasQuery);
    }

    public List<Row> search(Filter f, PageParams page) {
        Sql s = build(f);
        String excerpt = s.hasQuery()
                ? "ts_headline('english', e.body_md, query, 'StartSel=[[, StopSel=]], MaxWords=35, MinWords=15')"
                : "left(e.body_md, 240)";
        String order = switch (f.sort() == null ? "" : f.sort()) {
            case "top" -> "votes DESC, e.created_at DESC";
            case "new" -> "e.created_at DESC";
            default -> s.hasQuery() ? "ts_rank(e.search_vector, query) DESC, e.created_at DESC" : "e.created_at DESC";
        };
        String sql = """
                SELECT e.id, c.id AS company_id, c.slug, c.name, e.interview_year, e.role_title, e.difficulty,
                       e.outcome, e.anonymous, e.author_id, u.display_name, u.avatar_url, u.branch_code, u.grad_year,
                       e.created_at, %s AS excerpt,
                       (SELECT count(*) FROM experience_vote v WHERE v.experience_id = e.id) AS votes,
                       (SELECT count(*) FROM comment cm WHERE cm.experience_id = e.id AND cm.status = 'PUBLISHED') AS comments
                """.formatted(excerpt) + s.from() + s.where() + " ORDER BY " + order + " LIMIT :limit OFFSET :offset";
        s.p().addValue("limit", page.size()).addValue("offset", page.offset());
        return jdbc.query(sql, s.p(), (rs, i) -> new Row(
                rs.getLong("id"), rs.getLong("company_id"), rs.getString("slug"), rs.getString("name"),
                rs.getShort("interview_year"), rs.getString("role_title"), rs.getShort("difficulty"),
                InterviewExperience.Outcome.valueOf(rs.getString("outcome")), rs.getBoolean("anonymous"),
                rs.getObject("author_id", Long.class), rs.getString("display_name"), rs.getString("avatar_url"),
                rs.getString("branch_code"), rs.getObject("grad_year", Short.class), rs.getString("excerpt"),
                rs.getLong("votes"), rs.getLong("comments"), rs.getTimestamp("created_at").toInstant()));
    }

    public long count(Filter f) {
        Sql s = build(f);
        return jdbc.queryForObject("SELECT count(*)" + s.from() + s.where(), s.p(), Long.class);
    }
}
