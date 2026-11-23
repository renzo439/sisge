package com.sisge.ms.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {
    private UserDtos() {}

    public record Create(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "[A-Za-z0-9_.-]+", message = "Username only allows letters, digits, dots, dashes and underscores")
            String username,
            @NotBlank @Email @Size(max = 255) String email,
            @Size(max = 100) String password,
            @NotBlank @Size(max = 100) String rol) {}

    public record Created(Long id, String nombre, String apellido, String username, String email,
            Long organizacionId, String organizacionNombre, String rol, String password) {}

    public record RoleChange(@NotBlank @Size(max = 100) String rol) {}

    public record GlobalRoleChange(@Size(max = 100) String rol) {}

    public record MembershipRoleUpdated(Long usuarioId, String email, Long organizacionId,
            String organizacionNombre, String rol) {}

    public record GlobalRoleUpdated(Long usuarioId, String email, String rol) {}

    public record Membership(Long usuarioId, String nombre, String apellido, String username, String email,
            String estado, String rol, Instant createdAt) {}

    public record UserSummary(Long id, String nombre, String apellido, String username, String email,
            String estado, String rolGlobal) {}

    public record UserOrganizationRow(Long usuarioId, String nombre, String apellido, String username,
            String email, String estado, String rolGlobal, Long organizacionId, String organizacionNombre,
            String rol, String membresiaEstado, Instant createdAt) {}
}
