package api.packs;

import api.packs.dto.CreatePackRequest;
import api.packs.dto.PackDetailsResponse;
import api.packs.dto.PackResponse;
import api.packs.dto.UpdatePackRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/packs")
public class PackController {

    private final PackService packService;

    public PackController(PackService packService) {
        this.packService = packService;
    }

    @PostMapping
    public ResponseEntity<PackResponse> create(@Valid @RequestBody CreatePackRequest request) {
        PackResponse response = packService.create(request);
        URI location = URI.create("/api/packs/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PackResponse>> index() {
        return ResponseEntity.ok(packService.index());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PackDetailsResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(packService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PackResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdatePackRequest request
    ) {
        return ResponseEntity.ok(packService.update(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        packService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
