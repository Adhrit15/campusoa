package in.campusoa.common;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Upvotes for experiences and questions, in plain SQL.
 * <p>
 * {@code INSERT ... ON CONFLICT DO NOTHING} makes voting idempotent: clicking twice is harmless,
 * and the composite primary key (target_id, user_id) guarantees one vote per person, even if
 * two requests race.
 * <p>
 * Table and column names come from the {@link Target} enum (constants written here), never from
 * user input, so building the SQL string is safe.
 */
@Repository
public class Votes {

    public enum Target {
        EXPERIENCE("experience_vote", "experience_id"),
        QUESTION("question_vote", "question_id");

        final String table;
        final String column;

        Target(String table, String column) {
            this.table = table;
            this.column = column;
        }
    }

    private final NamedParameterJdbcTemplate jdbc;

    public Votes(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void add(Target t, long targetId, long userId) {
        jdbc.update("INSERT INTO " + t.table + " (" + t.column + ", user_id) VALUES (:id, :user) ON CONFLICT DO NOTHING",
                new MapSqlParameterSource("id", targetId).addValue("user", userId));
    }

    public void remove(Target t, long targetId, long userId) {
        jdbc.update("DELETE FROM " + t.table + " WHERE " + t.column + " = :id AND user_id = :user",
                new MapSqlParameterSource("id", targetId).addValue("user", userId));
    }

    /** id -> vote count, for a page of ids in ONE query (GROUP BY). */
    public Map<Long, Long> counts(Target t, Collection<Long> ids) {
        Map<Long, Long> out = new HashMap<>();
        if (ids.isEmpty()) return out;
        jdbc.query("SELECT " + t.column + " AS id, count(*) AS n FROM " + t.table + " WHERE " + t.column
                        + " IN (:ids) GROUP BY " + t.column,
                new MapSqlParameterSource("ids", ids), rs -> {
                    out.put(rs.getLong("id"), rs.getLong("n"));
                });
        return out;
    }

    /** Which of these ids has this user voted for? */
    public Set<Long> mine(Target t, Collection<Long> ids, Long userId) {
        Set<Long> out = new HashSet<>();
        if (ids.isEmpty() || userId == null) return out;
        jdbc.query("SELECT " + t.column + " AS id FROM " + t.table + " WHERE user_id = :user AND " + t.column + " IN (:ids)",
                new MapSqlParameterSource("ids", ids).addValue("user", userId), rs -> {
                    out.add(rs.getLong("id"));
                });
        return out;
    }
}
