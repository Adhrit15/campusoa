package in.campusoa.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

final class ModerationRepositories {
    private ModerationRepositories() {}
}

interface ReportRepository extends JpaRepository<Report, Long> {
}

interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {
}
