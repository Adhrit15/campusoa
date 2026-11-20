package in.campusoa.company;

import in.campusoa.common.PageParams;
import in.campusoa.company.CompanyDtos.CompanySummaryDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Hand-written SQL for the company list, which needs aggregates that would be clumsy in JPA.
 *
 * <pre>
 * LEFT JOIN LATERAL (...) l ON true
 * </pre>
 * A LATERAL subquery runs once per company row and may refer to it (c.id). Here it picks that
 * company's most recent academic year and sums its offers. LEFT JOIN ... ON true keeps
 * companies that have no stats at all (l.* is then NULL).
 */
@Repository
public class CompanyQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public CompanyQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String FROM_WHERE = """
            FROM company c
            LEFT JOIN LATERAL (
                SELECT ps.academic_year, max(ps.ctc_max_lpa) AS ctc, sum(ps.offers)::int AS offers
                FROM placement_stat ps
                WHERE ps.company_id = c.id
                GROUP BY ps.academic_year
                ORDER BY ps.academic_year DESC
                LIMIT 1
            ) l ON true
            WHERE (:q = '' OR c.name ILIKE '%' || :q || '%')
            """;

    public List<CompanySummaryDto> list(String q, String sort, PageParams page) {
        String orderBy = "name".equals(sort)
                ? "c.name"
                : "l.ctc DESC NULLS LAST, c.name";   // default: highest latest CTC first
        String sql = """
                SELECT c.id, c.slug, c.name, l.academic_year, l.ctc, l.offers,
                       (SELECT max(ps.ctc_max_lpa) FROM placement_stat ps WHERE ps.company_id = c.id) AS best_ctc,
                       (SELECT count(*) FROM interview_experience e
                         WHERE e.company_id = c.id AND e.status = 'PUBLISHED') AS experiences
                """ + FROM_WHERE + " ORDER BY " + orderBy + " LIMIT :limit OFFSET :offset";
        // orderBy is chosen from two constants above, never from user input: no SQL injection.
        return jdbc.query(sql, params(q).addValue("limit", page.size()).addValue("offset", page.offset()),
                (rs, i) -> new CompanySummaryDto(rs.getLong("id"), rs.getString("slug"), rs.getString("name"),
                        rs.getString("academic_year"), rs.getBigDecimal("ctc"),
                        (Integer) rs.getObject("offers", Integer.class), rs.getBigDecimal("best_ctc"),
                        rs.getLong("experiences")));
    }

    public long count(String q) {
        return jdbc.queryForObject("SELECT count(*) " + FROM_WHERE, params(q), Long.class);
    }

    private static MapSqlParameterSource params(String q) {
        return new MapSqlParameterSource("q", q == null ? "" : q.trim());
    }
}
