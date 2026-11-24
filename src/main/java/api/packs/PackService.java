package api.packs;

import api.common.CursorPage;
import api.common.PageQuery;
import api.common.ResourceNotFoundException;
import api.packs.dto.PackResponse;
import api.packs.dto.PackWriteRequest;
import api.users.User;
import api.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PackService {

    private final PackRepository packRepository;
    private final UserRepository userRepository;

    public PackService(PackRepository packRepository, UserRepository userRepository) {
        this.packRepository = packRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PackResponse getById(int id, Integer userId) {
        Pack pack = packRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));

        return toResponse(pack);
    }

    @Transactional
    public PackResponse create(PackWriteRequest request, Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Pack pack = new Pack(request.title(), user);
        return toResponse(packRepository.save(pack));
    }

    @Transactional(readOnly = true)
    public CursorPage<PackResponse> index(Integer userId, PageQuery page) {
        List<Pack> packs = packRepository.findByUserIdAndIdGreaterThanOrderByIdAsc(userId, page.afterId(), page.fetchLimit());
        return CursorPage.fromRows(packs, page.limit(), this::toResponse, Pack::getId);
    }

    @Transactional
    public PackResponse update(PackWriteRequest request, int id, Integer userId) {
        Pack pack = packRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Pack", id));

        pack.setTitle(request.title());
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
