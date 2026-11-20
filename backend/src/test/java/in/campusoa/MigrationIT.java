package in.campusoa;

import in.campusoa.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Migrations apply to a real Postgres, seed data is present, and key constraints actually bite. */
class MigrationIT extends IntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void referenceDataIsSeeded() {
        assertThat(count("branch")).isEqualTo(12);
        assertThat(count("roadmap_section")).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM topic WHERE parent_id IS NOT NULL", Long.class))
                .isGreaterThanOrEqualTo(10);
    }

    @Test
    void companyAggregatesAreSeededWithSource() {
        BigDecimal msCtc = jdbc.queryForObject("""
                SELECT ps.ctc_max_lpa FROM placement_stat ps JOIN company c ON c.id = ps.company_id
                WHERE c.slug = 'microsoft' AND ps.academic_year = '2024-25'""", BigDecimal.class);
        assertThat(msCtc).isEqualByComparingTo("51");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM placement_stat WHERE source_note IS NULL", Long.class)).isZero();
        // "Infosys up to 9.5" is stored as an open-ended range
        assertThat(jdbc.queryForObject("""
                SELECT ps.ctc_min_lpa IS NULL FROM placement_stat ps JOIN company c ON c.id = ps.company_id
                WHERE c.slug = 'infosys'""", Boolean.class)).isTrue();
        assertThat(count("internship_stat")).isEqualTo(9);
    }

    @Test
    void reportMustTargetExactlyOneThing() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO report (reason) VALUES ('SPAM')"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_report_one_target");
    }

    @Test
    void cgpaIsRangeChecked() {
        jdbc.update("""
                INSERT INTO app_user (email, google_sub, display_name) VALUES ('cgpa@x.edu', 'sub-cgpa', 'T')""");
        Long id = jdbc.queryForObject("SELECT id FROM app_user WHERE email = 'cgpa@x.edu'", Long.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO private_profile (user_id, cgpa) VALUES (?, 11)", id))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }
}
