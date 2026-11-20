package in.campusoa.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Package-private repositories for the company child tables (see CompanyChildren). */
final class CompanyRepositories {
    private CompanyRepositories() {}
}

interface CompanyAliasRepository extends JpaRepository<CompanyAlias, Long> {
    List<CompanyAlias> findByCompanyId(Long companyId);

    @Modifying
    @Query("DELETE FROM CompanyAlias a WHERE a.companyId = :companyId")
    void deleteByCompanyId(Long companyId);
}

interface PlacementStatRepository extends JpaRepository<PlacementStat, Long> {
    List<PlacementStat> findByCompanyIdOrderByAcademicYearDescCtcMaxLpaDesc(Long companyId);
}

interface InternshipStatRepository extends JpaRepository<InternshipStat, Long> {
    List<InternshipStat> findByCompanyIdOrderByAcademicYearDesc(Long companyId);

    Optional<InternshipStat> findByCompanyIdAndAcademicYear(Long companyId, String academicYear);
}

interface SelectionStageRepository extends JpaRepository<SelectionStage, Long> {
    List<SelectionStage> findByCompanyIdOrderByPosition(Long companyId);

    @Modifying
    @Query("DELETE FROM SelectionStage s WHERE s.companyId = :companyId")
    void deleteByCompanyId(Long companyId);
}
