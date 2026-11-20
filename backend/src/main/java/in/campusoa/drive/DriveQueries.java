package in.campusoa.drive;

import in.campusoa.common.PageParams;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtered drive list, written as SQL so you can read exactly what the database does.
 * <p>
 * The WHERE clause is built from optional filters. Values are ALWAYS bound as named parameters
 * (:company, :branch ...), never concatenated into the SQL string, which is what prevents SQL
 * injection. Only fixed snippets written here are appended.
 */
@Repository
public class DriveQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public DriveQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Filter(String companySlug, String branch, StageType stage, LocalDate from, LocalDate to, String q) {}

    public record Row(long id, String title, LocalDate noticeDate, String pdfUrl, Drive.Source source,
                      Long companyId, String companySlug, String companyName, String companyGuess,
                      String roleTitle, String ctcText) {}

    private record Sql(String where, MapSqlParameterSource params) {}

    private static Sql where(Filter f) {
        StringBuilder w = new StringBuilder(" WHERE d.merged_into_id IS NULL AND NOT d.hidden");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (f.companySlug() != null) {
            w.append(" AND c.slug = :company");
            p.addValue("company", f.companySlug());
        }
        if (f.from() != null) {
            w.append(" AND d.notice_date >= :from");
            p.addValue("from", Date.valueOf(f.from()));
        }
        if (f.to() != null) {
            w.append(" AND d.notice_date <= :to");
            p.addValue("to", Date.valueOf(f.to()));
        }
        if (f.stage() != null) {
            w.append(" AND EXISTS (SELECT 1 FROM drive_stage s WHERE s.drive_id = d.id AND s.stage_type = :stage)");
            p.addValue("stage", f.stage().name());
        }
        if (f.branch() != null) {
            // Keep drives the branch MIGHT be eligible for: criteria unknown, open to all, or listed.
            // (the leading space matters: text blocks strip common indentation)
            w.append(" " + """
                     AND (NOT EXISTS (SELECT 1 FROM drive_criteria dc WHERE dc.drive_id = d.id)
                          OR NOT EXISTS (SELECT 1 FROM drive_eligible_branch b WHERE b.drive_id = d.id)
                          OR EXISTS (SELECT 1 FROM drive_eligible_branch b
                                     WHERE b.drive_id = d.id AND b.branch_code = :branch))""");
            p.addValue("branch", f.branch());
        }
        if (f.q() != null && !f.q().isBlank()) {
            w.append(" AND (d.title ILIKE :q OR c.name ILIKE :q OR d.company_guess ILIKE :q)");
            p.addValue("q", "%" + f.q().trim() + "%");
        }
        return new Sql(w.toString(), p);
    }

    private static final String FROM = " FROM drive d LEFT JOIN company c ON c.id = d.company_id";

    public List<Row> list(Filter f, PageParams page) {
        Sql s = where(f);
        String sql = """
                SELECT d.id, d.title, d.notice_date, d.pdf_url, d.source, d.company_id, c.slug, c.name,
                       d.company_guess, d.role_title, d.ctc_text""" + FROM + s.where()
                + " ORDER BY d.notice_date DESC, d.id DESC LIMIT :limit OFFSET :offset";
        s.params().addValue("limit", page.size()).addValue("offset", page.offset());
        return jdbc.query(sql, s.params(), (rs, i) -> new Row(rs.getLong("id"), rs.getString("title"),
                rs.getDate("notice_date").toLocalDate(), rs.getString("pdf_url"),
                Drive.Source.valueOf(rs.getString("source")), (Long) rs.getObject("company_id", Long.class),
                rs.getString("slug"), rs.getString("name"), rs.getString("company_guess"),
                rs.getString("role_title"), rs.getString("ctc_text")));
    }

    public long count(Filter f) {
        Sql s = where(f);
        return jdbc.queryForObject("SELECT count(*)" + FROM + s.where(), s.params(), Long.class);
    }

    public record EventRow(long driveId, String title, String companyName, String companySlug, String kind,
                           LocalDate date, Instant startsAt) {}

    /**
     * Calendar events in [from, to]: each visible drive's notice date, plus every stage with a
     * start time. UNION ALL stacks two result sets with the same columns.
     * The stage branch uses ix_drive_stage_starts_at; the notice branch uses ix_drive_visible_notice_date.
     */
    public List<EventRow> events(LocalDate from, LocalDate to, String zoneId) {
        String sql = """
                SELECT d.id, d.title, coalesce(c.name, d.company_guess) AS company, c.slug,
                       'NOTICE' AS kind, d.notice_date AS day, NULL::timestamptz AS starts_at
                FROM drive d LEFT JOIN company c ON c.id = d.company_id
                WHERE d.merged_into_id IS NULL AND NOT d.hidden
                  AND d.notice_date BETWEEN :from AND :to
                UNION ALL
                SELECT d.id, d.title, coalesce(c.name, d.company_guess), c.slug,
                       s.stage_type, (s.starts_at AT TIME ZONE :zone)::date, s.starts_at
                FROM drive_stage s
                JOIN drive d ON d.id = s.drive_id
                LEFT JOIN company c ON c.id = d.company_id
                WHERE d.merged_into_id IS NULL AND NOT d.hidden
                  AND s.starts_at >= (CAST(:from AS date) AT TIME ZONE :zone)
                  AND s.starts_at <  ((CAST(:to AS date) + 1) AT TIME ZONE :zone)
                ORDER BY day, starts_at NULLS FIRST
                """;
        var p = new MapSqlParameterSource("from", Date.valueOf(from)).addValue("to", Date.valueOf(to))
                .addValue("zone", zoneId);
        List<EventRow> out = new ArrayList<>();
        jdbc.query(sql, p, rs -> {
            var ts = rs.getTimestamp("starts_at");
            out.add(new EventRow(rs.getLong("id"), rs.getString("title"), rs.getString("company"),
                    rs.getString("slug"), rs.getString("kind"), rs.getDate("day").toLocalDate(),
                    ts == null ? null : ts.toInstant()));
        });
        return out;
    }
}
