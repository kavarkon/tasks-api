package api.packs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePackRequest {

    @NotBlank(message = "title cannot be blank")
    @Size(max = 255, message = "title cannot be longer than 255 characters")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
