package api.packs;

import api.common.ResourceNotFoundException;
import api.packs.dto.CreatePackRequest;
import api.packs.dto.PackDetailsResponse;
import api.packs.dto.PackResponse;
import api.packs.dto.UpdatePackRequest;
import api.tasks.TaskRepository;
import api.tasks.dto.TaskResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PackService {

    private final PackRepository packRepository;
    private final TaskRepository taskRepository;

    public PackService(PackRepository packRepository, TaskRepository taskRepository) {
        this.packRepository = packRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public PackDetailsResponse getById(int id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));

        List<TaskResponse> tasks = taskRepository.findAllByPackId(id)
                .stream()
                .map(task -> new TaskResponse(
                        task.getId(), task.getTitle(), task.getCompleted(), id))
                .toList();

        return new PackDetailsResponse(pack.getId(), pack.getTitle(), tasks);
    }

    @Transactional
    public PackResponse create(CreatePackRequest request) {
        Pack pack = new Pack(request.getTitle());
        return toResponse(packRepository.save(pack));
    }

    @Transactional(readOnly = true)
    public List<PackResponse> index() {
        return packRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PackResponse update(UpdatePackRequest request, int id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));

        pack.setTitle(request.getTitle());
        return toResponse(packRepository.save(pack));
    }

    @Transactional
    public void delete(int id) {
        Pack pack = packRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));
        packRepository.delete(pack);
    }

    private PackResponse toResponse(Pack pack) {
        return new PackResponse(pack.getId(), pack.getTitle());
    }
}
