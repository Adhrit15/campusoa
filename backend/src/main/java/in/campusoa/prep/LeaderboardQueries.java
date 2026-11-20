package in.campusoa.prep;

import in.campusoa.common.PageParams;
import in.campusoa.prep.PrepDtos.LeaderboardEntry;
import in.campusoa.prep.PrepDtos.MyRank;
import in.campusoa.user.UserDtos.AuthorDto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Leaderboards in hand-written SQL with window functions. REPORT.md §5 walks through this line
 * by line. The key ideas:
 * <ul>
 *   <li>A window function ({@code RANK() OVER (...)}) computes a value for each row from a
 *       "window" of related rows, WITHOUT collapsing them like GROUP BY does.</li>
 *   <li>{@code RANK}: ties share a rank and the next rank is skipped (1, 2, 2, 4).
 *       {@code DENSE_RANK}: ties share a rank, with no gap (1, 2, 2, 3).</li>
 *   <li>{@code PARTITION BY branch_code, grad_year} restarts the ranking for each cohort, so one
 *       query yields both "overall rank" and "rank within CSE 2027".</li>
 *   <li>Window functions run AFTER WHERE, so we rank in a CTE first and filter the cohort
 *       OUTSIDE it. Otherwise filtering would change the overall ranks.</li>
 * </ul>
 */
@Repository
public class LeaderboardQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public LeaderboardQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String RANKED_CTE = """
            WITH solved AS (                                        -- (1) problems per user in the period
                SELECT p.user_id, count(*) AS solved
                FROM problem_log p
                WHERE p.solved_on >= :from AND p.solved_on < :to     -- uses ix_problem_log_solved_user
                GROUP BY p.user_id
            ),
            ranked AS (                                             -- (2) rank only opted-in, non-banned users
                SELECT u.id, u.display_name, u.avatar_url, u.branch_code, u.grad_year, s.solved,
                       RANK()       OVER (ORDER BY s.solved DESC)                                  AS overall_rank,
                       DENSE_RANK() OVER (ORDER BY s.solved DESC)                                  AS overall_dense,
                       RANK()       OVER (PARTITION BY u.branch_code, u.grad_year ORDER BY s.solved DESC) AS cohort_rank,
                       DENSE_RANK() OVER (PARTITION BY u.branch_code, u.grad_year ORDER BY s.solved DESC) AS cohort_dense
                FROM solved s
                JOIN app_user u ON u.id = s.user_id
                WHERE u.leaderboard_opt_in AND NOT u.banned
            )
            """;

    /** (3) filter to the cohort if asked, AFTER ranking, then page through. */
    private static String scopeWhere(boolean cohort) {
        return cohort ? " WHERE branch_code = :branch AND grad_year = :year" : "";
    }

    public List<LeaderboardEntry> page(LocalDate from, LocalDate to, boolean cohort, String branch, Short year,
                                       PageParams page, Long viewerId) {
        String rankCol = cohort ? "cohort_rank" : "overall_rank";
        String denseCol = cohort ? "cohort_dense" : "overall_dense";
        String sql = RANKED_CTE + "SELECT * FROM ranked" + scopeWhere(cohort)
                + " ORDER BY " + rankCol + ", display_name, id LIMIT :limit OFFSET :offset";
        var p = params(from, to, branch, year).addValue("limit", page.size()).addValue("offset", page.offset());
        return jdbc.query(sql, p, (rs, i) -> new LeaderboardEntry(rs.getLong(rankCol), rs.getLong(denseCol),
                new AuthorDto(rs.getLong("id"), rs.getString("display_name"), rs.getString("avatar_url"),
                        rs.getString("branch_code"), rs.getObject("grad_year", Short.class)),
                rs.getLong("solved"), viewerId != null && viewerId == rs.getLong("id")));
    }

    public long count(LocalDate from, LocalDate to, boolean cohort, String branch, Short year) {
        return jdbc.queryForObject(RANKED_CTE + "SELECT count(*) FROM ranked" + scopeWhere(cohort),
                params(from, to, branch, year), Long.class);
    }

    public MyRank myRank(LocalDate from, LocalDate to, boolean cohort, long userId) {
        String rankCol = cohort ? "cohort_rank" : "overall_rank";
        var rows = jdbc.query(RANKED_CTE + "SELECT " + rankCol + " AS r, solved FROM ranked WHERE id = :me",
                params(from, to, null, null).addValue("me", userId),
                (rs, i) -> new MyRank(rs.getLong("r"), rs.getLong("solved")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static MapSqlParameterSource params(LocalDate from, LocalDate to, String branch, Short year) {
        return new MapSqlParameterSource("from", Date.valueOf(from)).addValue("to", Date.valueOf(to))
                .addValue("branch", branch).addValue("year", year);
    }
}
