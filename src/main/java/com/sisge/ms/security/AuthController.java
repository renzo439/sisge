package com.sisge.ms.security;

import com.sisge.ms.dto.organizacion.OrganizationDtos;
import com.sisge.ms.exception.ApiException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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

    public record Login(@NotBlank @Size(max = 255) String email, @NotBlank String password) {
    }

    public record Token(String accessToken, String tokenType) {
    }

    public record Me(Long id, String nombre, String apellido, String username, String email, String rolGlobal,
            boolean global, Set<String> permissions, Long organizacionId, OrganizationDtos.Summary organizacion,
            List<OrganizationDtos.Summary> organizaciones, Set<String> globalPermissions,
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
        OrganizationDtos.Summary organizacion = user.organizacionId() == null ? null : user.organizaciones().stream()
                .filter(candidate -> user.organizacionId().equals(candidate.id()))
                .findFirst()
                .orElse(null);
        return new Me(user.id(), user.nombre(), user.apellido(), user.username(), user.email(), user.rolGlobal(),
                user.global(), user.permissions(), user.organizacionId(), organizacion, user.organizaciones(),
                user.globalPermissions(), user.organizationPermissions());
    }
}
