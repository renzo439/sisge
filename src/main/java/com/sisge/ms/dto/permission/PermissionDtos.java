package com.sisge.ms.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class PermissionDtos {
    private PermissionDtos() {}

    public record Create(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "[A-Za-z0-9_]+", message = "must contain only letters, digits and underscores")
            String codigo,
            @Size(max = 255) String descripcion) {}

    public record Update(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "[A-Za-z0-9_]+", message = "must contain only letters, digits and underscores")
            String codigo,
            @Size(max = 255) String descripcion) {}

    public record Summary(Long id, String codigo, String descripcion) {}
}
