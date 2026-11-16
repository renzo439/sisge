package com.sisge.ms.security;

import com.sisge.ms.exception.ApiException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserAuthenticationService users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserAuthenticationService users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record Login(@Email String email, @NotBlank String password) {
    }

    public record Token(String accessToken, String tokenType) {
    }

    public record Me(Long id, String email, boolean global, Set<String> globalPermissions,
            Map<Long, Set<String>> organizationPermissions) {
    }

    @PostMapping("/login")
    public Token login(@RequestBody @jakarta.validation.Valid Login login) {
        var user = users.entity(login.email());
        if (!"ACTIVO".equals(user.getEstado()) || !encoder.matches(login.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new Token(jwt.create(users.load(user.getEmail())), "Bearer");
    }

    @GetMapping("/me")
    public Me me() {
        CurrentUser user = CurrentUser.get();
        return new Me(user.id(), user.email(), user.global(), user.globalPermissions(),
                user.organizationPermissions());
    }
}
