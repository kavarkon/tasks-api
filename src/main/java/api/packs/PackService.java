package api.packs;

import api.common.ResourceNotFoundException;
import api.packs.dto.CreatePackRequest;
import api.packs.dto.PackDetailsResponse;
import api.packs.dto.PackResponse;
import api.packs.dto.UpdatePackRequest;
import api.tasks.TaskRepository;
import api.tasks.dto.TaskResponse;
import api.users.User;
import api.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PackService {

    private final PackRepository packRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public PackService(PackRepository packRepository, TaskRepository taskRepository, UserRepository userRepository) {
        this.packRepository = packRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PackDetailsResponse getById(int id, Integer userId) {
        Pack pack = packRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));

        List<TaskResponse> tasks = taskRepository.findAllByPackIdOrderByIdAsc(id)
                .stream()
                .map(task -> new TaskResponse(
                        task.getId(), task.getTitle(), task.getCompleted(), id))
                .toList();

        return new PackDetailsResponse(pack.getId(), pack.getTitle(), tasks);
    }

    @Transactional
    public PackResponse create(CreatePackRequest request, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Pack pack = new Pack(request.getTitle(), user);
        return toResponse(packRepository.save(pack));
    }

    @Transactional(readOnly = true)
    public List<PackResponse> index(Integer userId) {
        return packRepository.findAllByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PackResponse update(UpdatePackRequest request, int id, Integer userId) {
        Pack pack = packRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));

        pack.setTitle(request.getTitle());
        return toResponse(packRepository.save(pack));
    }

    @Transactional
    public void delete(int id, Integer userId) {
        Pack pack = packRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));
        packRepository.delete(pack);
    }

    private PackResponse toResponse(Pack pack) {
        return new PackResponse(pack.getId(), pack.getTitle());
    }
}
