package api.packs;

import api.common.ResourceNotFoundException;
import api.packs.dto.CreatePackRequest;
import api.packs.dto.PackResponse;
import api.packs.dto.UpdatePackRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PackService {

    private final PackRepository packRepository;

    public PackService(PackRepository packRepository) {
        this.packRepository = packRepository;
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
