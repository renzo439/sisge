package com.sisge.ms.dto.personal;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class PersonalDtos {
    private PersonalDtos() {}
    public record RowReview(@NotBlank @Size(max=20) String dni, @Size(max=20) String cuil,
            @NotBlank @Size(max=100) String nombres, @NotBlank @Size(max=100) String apellido, @Size(max=50) String sexo,
            LocalDate fechaNacimiento, @NotNull Long organizacionId, Long sectorId, @Size(max=255) String escalafon,
            @Size(max=255) String categoria, @Size(max=255) String situacionRevista, @Size(max=255) String agrupamiento,
            @Size(max=255) String funcion, @NotNull LocalDate fechaInicio, @Min(0) @Max(100) Integer antiguedadAnios,
            @NotNull Boolean afectado) {}
    public record Row(Long id, int filaOrigen, String valoresCrudos, String dni, String cuil, String nombres, String apellido,
            String sexo, LocalDate fechaNacimiento, Long organizacionId, Long sectorId, String escalafon, String categoria,
            String situacionRevista, String agrupamiento, String funcion, LocalDate fechaInicio, Integer antiguedadAnios,
            Boolean afectado, boolean revisada, String errores) {}
    public record ImportDetail(Long id, Long organizacionId, String archivoOrigen, String estado, List<Row> filas) {}
    public record Person(Long id, String dni, String cuil, String nombres, String apellido, String sexo,
            LocalDate fechaNacimiento, Long organizacionId, String organizacion, Long sectorId, String sector,
            String escalafon, String categoria, String situacionRevista, String agrupamiento, String funcion,
            LocalDate fechaInicio, Integer antiguedadAnios, boolean afectado) {}
}
