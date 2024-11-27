package api.tasks.service;

import api.tasks.model.Task;
import api.tasks.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public void create(Task task) {
        taskRepository.save(task);
    }

    public List<Task> index() {
        return (List<Task>) taskRepository.findAll();
    }


    public boolean update(Task task, int id) {
        if (taskRepository.existsById(id)) {
            task.setId(id);
            taskRepository.save(task);
            return true;
        }
        return false;
    }

    public boolean delete(int id) {
        if (taskRepository.existsById(id)) {
            taskRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
