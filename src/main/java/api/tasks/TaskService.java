package api.tasks;

import api.common.ResourceNotFoundException;
import api.packs.Pack;
import api.packs.PackRepository;
import api.tasks.dto.CreateTaskRequest;
import api.tasks.dto.TaskResponse;
import api.tasks.dto.UpdateTaskRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final PackRepository packRepository;

    public TaskService(TaskRepository taskRepository, PackRepository packRepository) {
        this.taskRepository = taskRepository;
        this.packRepository = packRepository;
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Pack pack = findPack(request.getPackId());
        Task task = new Task(request.getTitle(), request.getCompleted(), pack);

        return toResponse(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> index() {
        return taskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TaskResponse update(UpdateTaskRequest request, int id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        Pack pack = findPack(request.getPackId());

        task.setTitle(request.getTitle());
        task.setCompleted(request.getCompleted());
        task.setPack(pack);

        return toResponse(taskRepository.save(task));
    }

    @Transactional
    public void delete(int id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        taskRepository.delete(task);
    }

    private Pack findPack(Integer id) {
        return packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getCompleted(),
                task.getPack().getId()
        );
    }
}
