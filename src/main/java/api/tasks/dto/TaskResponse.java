package api.tasks.dto;

public record TaskResponse(Integer id, String title, boolean completed, Integer packId) {}
