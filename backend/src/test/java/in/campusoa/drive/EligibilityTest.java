package in.campusoa.drive;

import in.campusoa.drive.Eligibility.Rules;
import in.campusoa.drive.Eligibility.Status;
import in.campusoa.drive.Eligibility.Student;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityTest {

    private static final Rules STRICT = new Rules(new BigDecimal("7.5"), (short) 0, (short) 2027, Set.of("CSE", "IT"));
    private static final Student GOOD = new Student("CSE", (short) 2027, new BigDecimal("8.1"), (short) 0);

    @Test
    void noCriteriaMeansUnknown() {
        assertThat(Eligibility.evaluate(null, GOOD).status()).isEqualTo(Status.UNKNOWN);
    }

    @Test
    void meetsEveryRule() {
        var r = Eligibility.evaluate(STRICT, GOOD);
        assertThat(r.status()).isEqualTo(Status.ELIGIBLE);
        assertThat(r.reasons()).isEmpty();
    }

    @Test
    void cgpaExactlyAtCutoffIsEligible() {
        var s = new Student("IT", (short) 2027, new BigDecimal("7.50"), (short) 0);
        assertThat(Eligibility.evaluate(STRICT, s).status()).isEqualTo(Status.ELIGIBLE);
    }

    @Test
    void listsEveryFailedRule() {
        var s = new Student("ECE", (short) 2028, new BigDecimal("6.9"), (short) 2);
        var r = Eligibility.evaluate(STRICT, s);
        assertThat(r.status()).isEqualTo(Status.NOT_ELIGIBLE);
        assertThat(r.reasons()).hasSize(4);
        assertThat(r.reasons()).contains("Minimum CGPA is 7.5", "No active backlogs allowed");
    }

    @Test
    void missingProfileDataIsUnknownWithHint() {
        var s = new Student("CSE", (short) 2027, null, null);
        var r = Eligibility.evaluate(STRICT, s);
        assertThat(r.status()).isEqualTo(Status.UNKNOWN);
        assertThat(r.reasons().get(0)).contains("CGPA").contains("active backlogs");
    }

    @Test
    void definiteFailureBeatsMissingData() {
        var s = new Student("MECH", null, null, null);
        assertThat(Eligibility.evaluate(STRICT, s).status()).isEqualTo(Status.NOT_ELIGIBLE);
    }

    @Test
    void emptyBranchSetMeansAllBranches() {
        var open = new Rules(null, null, null, Set.of());
        assertThat(Eligibility.evaluate(open, new Student(null, null, null, null)).status()).isEqualTo(Status.ELIGIBLE);
    }

    @Test
    void backlogLimitAboveZero() {
        var rules = new Rules(null, (short) 1, null, Set.of());
        assertThat(Eligibility.evaluate(rules, new Student("CSE", null, null, (short) 1)).status()).isEqualTo(Status.ELIGIBLE);
        assertThat(Eligibility.evaluate(rules, new Student("CSE", null, null, (short) 2)).reasons())
                .containsExactly("At most 1 active backlog(s) allowed");
    }
}
