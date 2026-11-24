package api.tasks.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskWriteRequest(
        @NotBlank(message = "title cannot be blank")
        @Size(max = 255, message = "title cannot be longer than 255 characters")
        String title,
        @NotNull(message = "completed is required") Boolean completed,
        @NotNull(message = "packId is required") Integer packId
) {}
