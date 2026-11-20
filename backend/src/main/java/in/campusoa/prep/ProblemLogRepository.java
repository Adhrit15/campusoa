package in.campusoa.prep;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProblemLogRepository extends JpaRepository<ProblemLog, Long> {

    Page<ProblemLog> findByUserIdOrderBySolvedOnDescIdDesc(Long userId, Pageable pageable);

    Optional<ProblemLog> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndUrl(Long userId, String url);

    /** Distinct days with at least one solve, newest first: the input for streak calculation. */
    @Query("SELECT DISTINCT p.solvedOn FROM ProblemLog p WHERE p.userId = :userId ORDER BY p.solvedOn DESC")
    List<LocalDate> solvedDaysDesc(Long userId);
}
