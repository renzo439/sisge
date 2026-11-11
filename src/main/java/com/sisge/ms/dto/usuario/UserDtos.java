package com.sisge.ms.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {
    private UserDtos() {}

    public record Create(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(max = 100) String rol) {}

    public record Created(Long id, String nombre, String apellido, String email,
            Long organizacionId, String organizacionNombre, String rol, String passwordTemporal) {}

    public record RoleChange(@NotBlank @Size(max = 100) String rol) {}

    public record GlobalRoleChange(@Size(max = 100) String rol) {}

    public record MembershipRoleUpdated(Long usuarioId, String email, Long organizacionId,
            String organizacionNombre, String rol) {}

    public record GlobalRoleUpdated(Long usuarioId, String email, String rol) {}

    public record Membership(Long usuarioId, String nombre, String apellido, String email,
            String estado, String rol, Instant createdAt) {}

    public record UserSummary(Long id, String nombre, String apellido, String email,
            String estado, String rolGlobal) {}
}
