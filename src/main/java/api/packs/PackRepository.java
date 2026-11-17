package api.packs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackRepository extends JpaRepository<Pack, Integer> {
    List<Pack> findAllByUserId(Integer userId);

    Optional<Pack> findByIdAndUserId(Integer id, Integer userId);
}
