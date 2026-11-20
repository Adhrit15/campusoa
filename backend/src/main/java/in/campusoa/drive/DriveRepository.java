package in.campusoa.drive;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriveRepository extends JpaRepository<Drive, Long> {
    Optional<Drive> findByPdfUrl(String pdfUrl);

    boolean existsByPdfUrl(String pdfUrl);
}
