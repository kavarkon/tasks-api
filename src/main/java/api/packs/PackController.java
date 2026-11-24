package api.packs;

import api.common.CursorPage;
import api.common.PageQuery;

import api.packs.dto.PackResponse;
import api.packs.dto.PackWriteRequest;
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
@RequestMapping("/api/packs")
public class PackController {

    private final PackService packService;

    public PackController(PackService packService) {
        this.packService = packService;
    }

    @PostMapping
    public ResponseEntity<PackResponse> create(
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody PackWriteRequest request
    ) {
        PackResponse response = packService.create(request, userId);
        URI location = URI.create("/api/packs/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<CursorPage<PackResponse>> index(
            @AuthenticationPrincipal Integer userId,
            @RequestParam(required = false) String limit,
            @RequestParam(required = false) String afterId
    ) {
        PageQuery page = PageQuery.parse(limit, afterId);
        return ResponseEntity.ok(packService.index(userId, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackResponse> getById(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer userId
    ) {
        return ResponseEntity.ok(packService.getById(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PackResponse> update(
            @PathVariable Integer id,
            @AuthenticationPrincipal Integer userId,
            @Valid @RequestBody PackWriteRequest request
    ) {
        return ResponseEntity.ok(packService.update(request, id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, @AuthenticationPrincipal Integer userId) {
        packService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
