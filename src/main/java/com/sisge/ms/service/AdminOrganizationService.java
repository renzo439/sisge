package com.sisge.ms.service;

import com.sisge.ms.dto.organizacion.OrganizationDtos;
import com.sisge.ms.entity.Organizacion;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.AuditoriaEventoRepository;
import com.sisge.ms.repository.DependenciaRepository;
import com.sisge.ms.repository.DesignacionRepository;
import com.sisge.ms.repository.ImportacionPersonalRepository;
import com.sisge.ms.repository.ImportacionRepository;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.repository.ReemplazanteHistoricoRepository;
import com.sisge.ms.repository.SectorRepository;
import com.sisge.ms.repository.UserOrganizationRepository;
import com.sisge.ms.security.CurrentUser;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrganizationService {
    private static final String ORGANIZACIONES_GESTIONAR = "ORGANIZACIONES_GESTIONAR";

    private final OrganizationRepository organizations;
    private final UserOrganizationRepository memberships;
    private final ImportacionRepository imports;
    private final ImportacionPersonalRepository personalImports;
    private final DesignacionRepository designaciones;
    private final ReemplazanteHistoricoRepository history;
    private final DependenciaRepository dependencias;
    private final SectorRepository sectors;
    private final AuditoriaEventoRepository auditEvents;

    public AdminOrganizationService(OrganizationRepository organizations, UserOrganizationRepository memberships,
            ImportacionRepository imports, ImportacionPersonalRepository personalImports,
            DesignacionRepository designaciones, ReemplazanteHistoricoRepository history,
            DependenciaRepository dependencias, SectorRepository sectors,
            AuditoriaEventoRepository auditEvents) {
        this.organizations = organizations;
        this.memberships = memberships;
        this.imports = imports;
        this.personalImports = personalImports;
        this.designaciones = designaciones;
        this.history = history;
        this.dependencias = dependencias;
        this.sectors = sectors;
        this.auditEvents = auditEvents;
    }

    @Transactional(readOnly = true)
    public List<OrganizationDtos.Detail> list() {
        CurrentUser.get().requireGlobal(ORGANIZACIONES_GESTIONAR);
        return organizations.findAll().stream()
                .sorted(Comparator.comparing(organization -> organization.getNombre().toLowerCase()))
                .map(this::detail)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationDtos.Detail get(Long id) {
        CurrentUser.get().requireGlobal(ORGANIZACIONES_GESTIONAR);
        return detail(findOrganization(id));
    }

    @Transactional
    public OrganizationDtos.Detail create(OrganizationDtos.Create request) {
        CurrentUser.get().requireGlobal(ORGANIZACIONES_GESTIONAR);
        String nombre = request.nombre().trim();
        String udeo = normalizeUdeo(request.udeo());
        if (udeo != null && organizations.findByUdeo(udeo).isPresent()) {
            throw ApiException.conflict("An organization with that UDEO already exists");
        }
        Organizacion organization = new Organizacion();
        organization.setNombre(nombre);
        organization.setUdeo(udeo);
        organization.setDescripcion(request.descripcion());
        return detail(organizations.save(organization));
    }

    @Transactional
    public OrganizationDtos.Detail update(Long id, OrganizationDtos.Update request) {
        CurrentUser.get().requireGlobal(ORGANIZACIONES_GESTIONAR);
        Organizacion organization = findOrganization(id);
        String nombre = request.nombre().trim();
        String udeo = normalizeUdeo(request.udeo());
        if (udeo != null) {
            organizations.findByUdeo(udeo)
                    .filter(existing -> !existing.getId().equals(organization.getId()))
                    .ifPresent(existing -> {
                        throw ApiException.conflict("An organization with that UDEO already exists");
                    });
        }
        organization.setNombre(nombre);
        organization.setUdeo(udeo);
        organization.setDescripcion(request.descripcion());
        return detail(organizations.save(organization));
    }

    @Transactional
    public void delete(Long id) {
        CurrentUser.get().requireGlobal(ORGANIZACIONES_GESTIONAR);
        Organizacion organization = findOrganization(id);
        List<String> blockers = usage(organization.getId());
        if (!blockers.isEmpty()) {
            throw ApiException.conflict("The organization has associated data and cannot be deleted: "
                    + String.join(", ", blockers));
        }
        organizations.delete(organization);
    }

    private List<String> usage(Long organizationId) {
        java.util.ArrayList<String> blockers = new java.util.ArrayList<>();
        if (memberships.existsByOrganizacionId(organizationId)) {
            blockers.add("usuarios asignados");
        }
        if (imports.existsByOrganizacionId(organizationId)) {
            blockers.add("importaciones de reemplazantes");
        }
        if (personalImports.existsByOrganizacionId(organizationId)) {
            blockers.add("importaciones de personal");
        }
        if (designaciones.existsByOrganizacionId(organizationId)) {
            blockers.add("designaciones");
        }
        if (history.existsByOrganizacionId(organizationId)) {
            blockers.add("historico de reemplazantes");
        }
        if (dependencias.existsByOrganizacionId(organizationId)) {
            blockers.add("dependencias");
        }
        if (sectors.existsByOrganizacionId(organizationId)) {
            blockers.add("sectores");
        }
        if (auditEvents.existsByOrganizacionId(organizationId)) {
            blockers.add("auditoria");
        }
        return List.copyOf(blockers);
    }

    private Organizacion findOrganization(Long id) {
        return organizations.findById(id)
                .orElseThrow(() -> ApiException.notFound("Organization not found"));
    }

    private String normalizeUdeo(String udeo) {
        if (udeo == null) {
            return null;
        }
        String value = udeo.trim();
        return value.isEmpty() ? null : value;
    }

    private OrganizationDtos.Detail detail(Organizacion organization) {
        return new OrganizationDtos.Detail(organization.getId(), organization.getNombre(),
                organization.getUdeo(), organization.getDescripcion(), organization.getCreatedAt());
    }
}
