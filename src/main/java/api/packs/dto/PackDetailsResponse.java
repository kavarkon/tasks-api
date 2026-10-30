package api.packs.dto;

import api.tasks.dto.TaskResponse;

import java.util.List;

public class PackDetailsResponse {

    private final Integer id;
    private final String title;
    private final List<TaskResponse> tasks;

    public PackDetailsResponse(Integer id, String title, List<TaskResponse> tasks) {
        this.id = id;
        this.title = title;
        this.tasks = tasks;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<TaskResponse> getTasks() {
        return tasks;
    }
}
