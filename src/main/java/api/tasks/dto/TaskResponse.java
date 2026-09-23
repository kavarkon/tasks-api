package api.tasks.dto;

public class TaskResponse {

    private final Integer id;
    private final String title;
    private final Boolean completed;
    private final Integer packId;

    public TaskResponse(Integer id, String title, Boolean completed, Integer packId) {
        this.id = id;
        this.title = title;
        this.completed = completed;
        this.packId = packId;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Integer getPackId() {
        return packId;
    }
}
