package api.auth;

import api.auth.dto.RegisterRequest;
import api.auth.dto.LoginRequest;
import api.auth.dto.UserResponse;
import api.security.JwtService;
import api.users.User;
import api.users.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId());
        UserResponse response = new UserResponse(user.getId(), user.getEmail());
        return new LoginResult(token, response);
    }

    public record LoginResult(String token, UserResponse user) {
    }

    public UserResponse register(RegisterRequest request) {
        if (request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidPasswordException("Пароль не может быть длиннее 72 байт в UTF-8");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException();
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), passwordHash);

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new UserResponse(savedUser.getId(), savedUser.getEmail());
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmail(exception)) {
                throw new EmailAlreadyExistsException();
            }
            throw exception;
        }
    }

    private boolean isDuplicateEmail(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "uk_users_email".equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
