package in.campusoa.drive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The eligibility checker: pure functions with no Spring or database, so every rule has a
 * fast, readable unit test (EligibilityTest).
 *
 * <p>Outcome rules:
 * <ul>
 *   <li>No criteria entered for the drive: {@code UNKNOWN} ("criteria unknown").</li>
 *   <li>Any rule the student definitely fails: {@code NOT_ELIGIBLE}, listing every failed rule.</li>
 *   <li>Otherwise, if a rule needs profile data the student hasn't filled in: {@code UNKNOWN},
 *       with a "fill in your profile" reason.</li>
 *   <li>Otherwise: {@code ELIGIBLE}.</li>
 * </ul>
 * "Not eligible" wins over "missing data": if you're in the wrong branch, a missing CGPA
 * doesn't matter.
 */
public final class Eligibility {

    private Eligibility() {}

    public enum Status { ELIGIBLE, NOT_ELIGIBLE, UNKNOWN }

    /** The drive's requirements. Null fields mean "no requirement"; empty branches means "all". */
    public record Rules(BigDecimal minCgpa, Short maxBacklogs, Short gradYear, Set<String> branches) {}

    /** The student's data (from their private profile). Null fields mean "not filled in". */
    public record Student(String branchCode, Short gradYear, BigDecimal cgpa, Short activeBacklogs) {}

    public record Result(Status status, List<String> reasons) {
        public static Result unknownCriteria() {
            return new Result(Status.UNKNOWN, List.of("The eligibility criteria for this drive haven't been added yet"));
        }
    }

    public static Result evaluate(Rules rules, Student s) {
        if (rules == null) return Result.unknownCriteria();

        List<String> failed = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (!rules.branches().isEmpty()) {
            if (s.branchCode() == null) missing.add("branch");
            else if (!rules.branches().contains(s.branchCode())) failed.add("Your branch isn't eligible for this drive");
        }
        if (rules.gradYear() != null) {
            if (s.gradYear() == null) missing.add("graduation year");
            else if (!rules.gradYear().equals(s.gradYear()))
                failed.add("This drive is for the " + rules.gradYear() + " graduating batch");
        }
        if (rules.minCgpa() != null) {
            if (s.cgpa() == null) missing.add("CGPA");
            else if (s.cgpa().compareTo(rules.minCgpa()) < 0)
                failed.add("Minimum CGPA is " + rules.minCgpa().stripTrailingZeros().toPlainString());
        }
        if (rules.maxBacklogs() != null) {
            if (s.activeBacklogs() == null) missing.add("active backlogs");
            else if (s.activeBacklogs() > rules.maxBacklogs())
                failed.add(rules.maxBacklogs() == 0 ? "No active backlogs allowed"
                        : "At most " + rules.maxBacklogs() + " active backlog(s) allowed");
        }

        if (!failed.isEmpty()) return new Result(Status.NOT_ELIGIBLE, List.copyOf(failed));
        if (!missing.isEmpty()) {
            return new Result(Status.UNKNOWN,
                    List.of("Add your " + String.join(", ", missing) + " in your profile to check eligibility"));
        }
        return new Result(Status.ELIGIBLE, List.of());
    }
}
