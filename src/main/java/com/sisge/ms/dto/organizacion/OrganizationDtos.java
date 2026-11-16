package com.sisge.ms.dto.organizacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class OrganizationDtos {
    private OrganizationDtos() {}

    public record Summary(Long id, String nombre, String udeo) {}

    public record Create(
            @NotBlank @Size(max = 255) String nombre,
            @Size(max = 255) String udeo,
            String descripcion) {}

    public record Update(
            @NotBlank @Size(max = 255) String nombre,
            @Size(max = 255) String udeo,
            String descripcion) {}

    public record Detail(Long id, String nombre, String udeo, String descripcion, Instant createdAt) {}
}
