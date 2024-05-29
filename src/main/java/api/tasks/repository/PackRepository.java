package api.tasks.repository;

import api.tasks.model.Pack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackRepository extends JpaRepository<Pack, Integer> {
}
