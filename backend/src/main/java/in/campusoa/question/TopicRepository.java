package in.campusoa.question;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {

    List<Topic> findAllByOrderBySortOrder();

    List<Topic> findByCodeIn(Collection<String> codes);
}
