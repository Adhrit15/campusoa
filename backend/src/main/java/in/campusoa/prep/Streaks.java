package in.campusoa.prep;

import java.time.LocalDate;
import java.util.List;

/**
 * Streak maths, kept pure for unit testing. A "streak" is a run of consecutive days with at least
 * one solved problem. The current streak is still alive if the last solve was today OR yesterday
 * (you haven't missed today yet).
 */
public final class Streaks {

    private Streaks() {}

    public record Result(int current, int longest) {}

    /** @param daysDesc distinct solve dates, newest first */
    public static Result compute(List<LocalDate> daysDesc, LocalDate today) {
        if (daysDesc.isEmpty()) return new Result(0, 0);

        int longest = 1;
        int run = 1;
        for (int i = 1; i < daysDesc.size(); i++) {
            if (daysDesc.get(i).plusDays(1).equals(daysDesc.get(i - 1))) {
                run++;
                longest = Math.max(longest, run);
            } else {
                run = 1;
            }
        }

        int current = 0;
        LocalDate newest = daysDesc.get(0);
        if (newest.equals(today) || newest.equals(today.minusDays(1))) {
            current = 1;
            for (int i = 1; i < daysDesc.size() && daysDesc.get(i).plusDays(1).equals(daysDesc.get(i - 1)); i++) {
                current++;
            }
        }
        return new Result(current, longest);
    }
}
