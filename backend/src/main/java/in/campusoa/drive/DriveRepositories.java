package in.campusoa.drive;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

final class DriveRepositories {
    private DriveRepositories() {}
}

interface DriveStageRepository extends JpaRepository<DriveStage, Long> {
    List<DriveStage> findByDriveIdOrderByStartsAtAscIdAsc(Long driveId);

    List<DriveStage> findByDriveIdIn(Collection<Long> driveIds);

    List<DriveStage> findByDriveId(Long driveId);
}

interface DriveCriteriaRepository extends JpaRepository<DriveCriteria, Long> {
    List<DriveCriteria> findByDriveIdIn(Collection<Long> driveIds);
}
