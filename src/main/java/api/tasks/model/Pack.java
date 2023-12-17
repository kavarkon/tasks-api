package api.tasks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import javax.validation.constraints.NotEmpty;
import java.util.Set;

@Entity
public class Pack {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @NotEmpty(message = "Title should not be empty")
    @Column(nullable = false)
    private String title;

    @OneToMany(mappedBy = "pack", orphanRemoval = true)
    private Set<Task> tasks;

    public Pack() {}

    public Pack(String title) {
        this.title = title;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
