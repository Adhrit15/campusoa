package in.campusoa.prep;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StreaksTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    private static LocalDate d(int daysAgo) {
        return TODAY.minusDays(daysAgo);
    }

    @Test
    void empty() {
        assertThat(Streaks.compute(List.of(), TODAY)).isEqualTo(new Streaks.Result(0, 0));
    }

    @Test
    void currentStreakIncludingToday() {
        assertThat(Streaks.compute(List.of(d(0), d(1), d(2), d(5)), TODAY)).isEqualTo(new Streaks.Result(3, 3));
    }

    @Test
    void yesterdayKeepsTheStreakAlive() {
        assertThat(Streaks.compute(List.of(d(1), d(2)), TODAY).current()).isEqualTo(2);
    }

    @Test
    void gapOfTwoDaysBreaksIt() {
        var r = Streaks.compute(List.of(d(2), d(3), d(4), d(5)), TODAY);
        assertThat(r.current()).isZero();
        assertThat(r.longest()).isEqualTo(4);
    }

    @Test
    void longestCanBeInThePast() {
        var r = Streaks.compute(List.of(d(0), d(10), d(11), d(12), d(13), d(14)), TODAY);
        assertThat(r).isEqualTo(new Streaks.Result(1, 5));
    }
}
