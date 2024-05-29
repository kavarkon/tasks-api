package api.tasks.service;


import api.tasks.model.Pack;
import api.tasks.repository.PackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PackService {
    @Autowired
    private PackRepository packRepository;

    public Pack create(Pack pack) {
        return packRepository.save(pack);
    }

    public List<Pack> index() {
        return (List<Pack>) packRepository.findAll();
    }


    public boolean update(Pack pack, int id) {
        if (packRepository.existsById(id)) {
            pack.setId(id);
            packRepository.save(pack);
            return true;
        }
        return false;
    }

    public boolean delete(int id) {
        if (packRepository.existsById(id)) {
            packRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
