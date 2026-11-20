package in.campusoa.experience;

import in.campusoa.common.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

final class ExperienceRepositories {
    private ExperienceRepositories() {}
}

interface InterviewExperienceRepository extends JpaRepository<InterviewExperience, Long> {
}

interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByExperienceIdAndStatusInOrderByCreatedAtAsc(Long experienceId, Collection<ContentStatus> statuses,
                                                                  Pageable pageable);
}
