package api.tasks.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
@Entity
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "Title should not be empty")
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private Boolean completed;

    @Column(nullable = false)
    private Integer packId;

    @ManyToOne
    @JoinColumn(name = "packId", referencedColumnName = "id", insertable=false, updatable=false)
    private Pack pack;

    public Task(String title, Boolean completed, Integer packId) {
        this.title = title;
        this.completed = completed;
        this.packId = packId;
    }
}
