package api.tasks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Integer> {
    List<Task> findAllByPackIdOrderByIdAsc(Integer packId);

    List<Task> findAllByPackUserId(Integer userId);

    Optional<Task> findByIdAndPackUserId(Integer id, Integer userId);
}
