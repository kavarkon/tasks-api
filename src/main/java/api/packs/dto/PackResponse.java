package api.packs.dto;

public class PackResponse {

    private final Integer id;
    private final String title;

    public PackResponse(Integer id, String title) {
        this.id = id;
        this.title = title;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
