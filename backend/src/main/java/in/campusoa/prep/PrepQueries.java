package in.campusoa.prep;

import in.campusoa.prep.PrepDtos.RoadmapItemDto;
import in.campusoa.prep.PrepDtos.RoadmapSectionDto;
import in.campusoa.prep.PrepDtos.TopicCount;
import in.campusoa.prep.PrepDtos.WeekCount;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Roadmap and dashboard SQL. */
@Repository
public class PrepQueries {

    private final NamedParameterJdbcTemplate jdbc;

    public PrepQueries(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The whole roadmap with this user's ticks: LEFT JOIN keeps unticked items (rp.* is NULL). */
    public List<RoadmapSectionDto> roadmap(long userId) {
        Map<String, String> sectionNames = new LinkedHashMap<>();
        Map<String, List<RoadmapItemDto>> items = new LinkedHashMap<>();
        jdbc.query("""
                SELECT s.code, s.name, i.id, i.position, i.title, i.description, i.resource_url,
                       rp.item_id IS NOT NULL AS done
                FROM roadmap_section s
                JOIN roadmap_item i ON i.section_code = s.code
                LEFT JOIN roadmap_progress rp ON rp.item_id = i.id AND rp.user_id = :user
                ORDER BY s.sort_order, i.position""", new MapSqlParameterSource("user", userId), rs -> {
            String code = rs.getString("code");
            sectionNames.putIfAbsent(code, rs.getString("name"));
            items.computeIfAbsent(code, k -> new ArrayList<>()).add(new RoadmapItemDto(rs.getLong("id"),
                    rs.getInt("position"), rs.getString("title"), rs.getString("description"),
                    rs.getString("resource_url"), rs.getBoolean("done")));
        });
        List<RoadmapSectionDto> out = new ArrayList<>();
        sectionNames.forEach((code, name) -> {
            var list = items.get(code);
            int done = (int) list.stream().filter(RoadmapItemDto::done).count();
            out.add(new RoadmapSectionDto(code, name, list, done, list.size()));
        });
        return out;
    }

    public boolean roadmapItemExists(long itemId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM roadmap_item WHERE id = :id)",
                new MapSqlParameterSource("id", itemId), Boolean.class));
    }

    public void tick(long userId, long itemId, boolean done) {
        var p = new MapSqlParameterSource("user", userId).addValue("item", itemId);
        if (done) {
            jdbc.update("INSERT INTO roadmap_progress (user_id, item_id) VALUES (:user, :item) ON CONFLICT DO NOTHING", p);
        } else {
            jdbc.update("DELETE FROM roadmap_progress WHERE user_id = :user AND item_id = :item", p);
        }
    }

    /**
     * Problems per week for the last 12 weeks, including weeks with ZERO solves.
     * generate_series makes one row per week; LEFT JOIN + count(p.id) turns "no rows" into 0.
     * date_trunc('week', ...) gives the Monday of that week (ISO weeks).
     */
    public List<WeekCount> perWeek(long userId, LocalDate today) {
        return jdbc.query("""
                SELECT w.week_start::date AS week_start, count(p.id) AS solved
                FROM generate_series(date_trunc('week', CAST(:today AS date)) - interval '11 weeks',
                                     date_trunc('week', CAST(:today AS date)),
                                     interval '1 week') AS w(week_start)
                LEFT JOIN problem_log p
                       ON p.user_id = :user
                      AND p.solved_on >= w.week_start
                      AND p.solved_on <  w.week_start + interval '7 days'
                GROUP BY w.week_start
                ORDER BY w.week_start""",
                new MapSqlParameterSource("user", userId).addValue("today", Date.valueOf(today)),
                (rs, i) -> new WeekCount(rs.getDate("week_start").toLocalDate(), rs.getLong("solved")));
    }

    /** Solved count for every DSA sub-topic (0 for untouched ones, so the UI can show gaps). */
    public List<TopicCount> topicCoverage(long userId) {
        return jdbc.query("""
                SELECT t.code, t.name, count(p.id) AS solved
                FROM topic t
                JOIN topic parent ON parent.id = t.parent_id AND parent.code = 'dsa'
                LEFT JOIN problem_log p ON p.topic_id = t.id AND p.user_id = :user
                GROUP BY t.id, t.code, t.name, t.sort_order
                ORDER BY t.sort_order""", new MapSqlParameterSource("user", userId),
                (rs, i) -> new TopicCount(rs.getString("code"), rs.getString("name"), rs.getLong("solved")));
    }

    /** One pass over the user's log with FILTER clauses: several counts in a single scan. */
    public Map<String, Long> totals(long userId, LocalDate weekStart, LocalDate monthStart) {
        return jdbc.queryForObject("""
                SELECT count(*) AS total,
                       count(*) FILTER (WHERE solved_on >= :week)  AS week,
                       count(*) FILTER (WHERE solved_on >= :month) AS month,
                       count(*) FILTER (WHERE difficulty = 'EASY')   AS easy,
                       count(*) FILTER (WHERE difficulty = 'MEDIUM') AS medium,
                       count(*) FILTER (WHERE difficulty = 'HARD')   AS hard
                FROM problem_log WHERE user_id = :user""",
                new MapSqlParameterSource("user", userId).addValue("week", Date.valueOf(weekStart))
                        .addValue("month", Date.valueOf(monthStart)),
                (rs, i) -> Map.of("total", rs.getLong("total"), "week", rs.getLong("week"),
                        "month", rs.getLong("month"), "easy", rs.getLong("easy"),
                        "medium", rs.getLong("medium"), "hard", rs.getLong("hard")));
    }
}
