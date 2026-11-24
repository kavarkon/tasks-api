package api.packs;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackRepository extends JpaRepository<Pack, Integer> {
    List<Pack> findByUserIdAndIdGreaterThanOrderByIdAsc(Integer userId, int afterId, Limit limit);

    boolean existsByIdAndUserId(Integer id, Integer userId);

    Optional<Pack> findByIdAndUserId(Integer id, Integer userId);
}
