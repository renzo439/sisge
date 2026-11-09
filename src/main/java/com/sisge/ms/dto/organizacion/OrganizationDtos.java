package com.sisge.ms.dto.organizacion;

public final class OrganizationDtos {
    private OrganizationDtos() {}

    public record Summary(Long id, String nombre, String udeo) {}
}
