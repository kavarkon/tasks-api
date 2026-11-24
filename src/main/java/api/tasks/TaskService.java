package api.tasks;

import api.common.CursorPage;
import api.common.PageQuery;
import api.common.ResourceNotFoundException;
import api.packs.Pack;
import api.packs.PackRepository;
import api.tasks.dto.TaskResponse;
import api.tasks.dto.TaskWriteRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final PackRepository packRepository;

    public TaskService(TaskRepository taskRepository, PackRepository packRepository) {
        this.taskRepository = taskRepository;
        this.packRepository = packRepository;
    }

    @Transactional
    public TaskResponse create(TaskWriteRequest request, Integer userId) {
        Pack pack = findPack(request.packId(), userId);
        Task task = new Task(request.title(), request.completed(), pack);

        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public CursorPage<TaskResponse> index(Integer userId, PageQuery page, Integer packId) {
        List<TaskResponse> tasks;
        if (packId == null) {
            tasks = taskRepository.findPageByOwner(userId, page.afterId(), page.fetchLimit());
        } else {
            if (!packRepository.existsByIdAndUserId(packId, userId)) {
                throw new ResourceNotFoundException("Pack", packId);
            }
            tasks = taskRepository.findPageByOwnerAndPack(
                    userId, packId, page.afterId(), page.fetchLimit());
        }
        return CursorPage.fromRows(tasks, page.limit(), Function.identity(), TaskResponse::id);
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(int id, Integer userId) {
        return taskRepository.findResponseByIdAndOwner(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    @Transactional
    public TaskResponse update(TaskWriteRequest request, int id, Integer userId) {
        Task task = taskRepository.findByIdAndPackUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        Pack pack = findPack(request.packId(), userId);

        task.setTitle(request.title());
        task.setCompleted(request.completed());
        task.setPack(pack);

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(int id, Integer userId) {
        Task task = taskRepository.findByIdAndPackUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        taskRepository.delete(task);
    }

    private Pack findPack(Integer id, Integer userId) {
        return packRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.isCompleted(),
                task.getPack().getId()
        );
    }
}
