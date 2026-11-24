package api.tasks;

import api.tasks.dto.TaskResponse;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Integer> {

    @Query("""
            select new api.tasks.dto.TaskResponse(task.id, task.title, task.completed, task.pack.id)
            from Task task
            where task.pack.user.id = :userId and task.id > :afterId
            order by task.id
            """)
    List<TaskResponse> findPageByOwner(Integer userId, int afterId, Limit limit);

    @Query("""
            select new api.tasks.dto.TaskResponse(task.id, task.title, task.completed, task.pack.id)
            from Task task
            where task.pack.user.id = :userId and task.pack.id = :packId and task.id > :afterId
            order by task.id
            """)
    List<TaskResponse> findPageByOwnerAndPack(Integer userId, Integer packId, int afterId, Limit limit);

    @Query("""
            select new api.tasks.dto.TaskResponse(task.id, task.title, task.completed, task.pack.id)
            from Task task
            where task.id = :id and task.pack.user.id = :userId
            """)
    Optional<TaskResponse> findResponseByIdAndOwner(int id, Integer userId);

    Optional<Task> findByIdAndPackUserId(Integer id, Integer userId);
}
