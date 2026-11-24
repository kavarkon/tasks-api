package api.tasks;

import api.common.CursorPage;
import api.common.PageQuery;

import api.tasks.dto.TaskResponse;
import api.tasks.dto.TaskWriteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody TaskWriteRequest request
    ) {
        TaskResponse response = taskService.create(request, userId);
        URI location = URI.create("/api/tasks/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<CursorPage<TaskResponse>> index(
            @AuthenticationPrincipal Integer userId,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String afterId,
            @RequestParam(required = false) String packId
    ) {
        PageQuery page = PageQuery.parse(limit, afterId);
        Integer selectedPackId = packId == null ? null : PageQuery.parsePositiveInteger("packId", packId);
        return ResponseEntity.ok(taskService.index(userId, page, selectedPackId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getById(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer userId
    ) {
        return ResponseEntity.ok(taskService.getById(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody TaskWriteRequest request
    ) {
        return ResponseEntity.ok(taskService.update(request, id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, @AuthenticationPrincipal Integer userId) {
        taskService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
