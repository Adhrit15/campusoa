package in.campusoa.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data generates the implementation; the method name is parsed into ORDER BY sort_order. */
public interface BranchRepository extends JpaRepository<Branch, String> {
    List<Branch> findAllByOrderBySortOrder();
}
