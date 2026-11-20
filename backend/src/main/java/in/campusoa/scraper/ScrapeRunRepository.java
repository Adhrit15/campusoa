package in.campusoa.scraper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface ScrapeRunRepository extends JpaRepository<ScrapeRun, Long> {

    Page<ScrapeRun> findAllByOrderByStartedAtDesc(Pageable pageable);

    Optional<ScrapeRun> findFirstByOrderByStartedAtDesc();

    /** Latest run that fetched the page (200 or 304), for ETag/Last-Modified and the 6-hour check. */
    Optional<ScrapeRun> findFirstByStatusInOrderByStartedAtDesc(Collection<ScrapeRun.Status> statuses);
}
