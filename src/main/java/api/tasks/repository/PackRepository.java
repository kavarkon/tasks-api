package api.tasks.repository;

import api.tasks.model.Pack;
import org.springframework.data.repository.CrudRepository;

public interface PackRepository extends CrudRepository<Pack, Integer> {
}
