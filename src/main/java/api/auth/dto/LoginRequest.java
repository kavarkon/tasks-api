package api.auth.dto;

import api.users.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный адрес электронной почты")
    @Size(max = 255, message = "Email не может быть длиннее 255 символов")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = EmailNormalizer.normalize(email);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
