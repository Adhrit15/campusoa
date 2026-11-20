package in.campusoa.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

final class QuestionRepositories {
    private QuestionRepositories() {}
}

interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExperienceIdOrderById(Long experienceId);
}
