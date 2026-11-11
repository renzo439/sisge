package com.sisge.ms.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public final class RoleDtos {
    private RoleDtos() {}

    public record Create(
            @NotBlank @Size(max = 100) String nombre,
            @Size(max = 255) String descripcion,
            boolean globalRole,
            Set<String> permisos) {}

    public record Update(
            @NotBlank @Size(max = 100) String nombre,
            @Size(max = 255) String descripcion,
            boolean globalRole,
            Set<String> permisos) {}

    public record Summary(Long id, String nombre, String descripcion, boolean globalRole, Set<String> permisos) {}
}
