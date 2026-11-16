package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AdminOrganizationServiceTests {
    @Mock
    private OrganizationRepository organizations;
    @Mock
    private UserOrganizationRepository memberships;
    @Mock
    private ImportacionRepository imports;
    @Mock
    private ImportacionPersonalRepository personalImports;
    @Mock
    private DesignacionRepository designaciones;
    @Mock
    private ReemplazanteHistoricoRepository history;
    @Mock
    private DependenciaRepository dependencias;
    @Mock
    private SectorRepository sectors;
    @Mock
    private AuditoriaEventoRepository auditEvents;

    private AdminOrganizationService service;

    @BeforeEach
    void loginAsGlobalAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("ORGANIZACIONES_GESTIONAR"), java.util.Map.of(), true),
                null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private AdminOrganizationService service() {
        if (service == null) {
            service = new AdminOrganizationService(organizations, memberships, imports, personalImports,
                    designaciones, history, dependencias, sectors, auditEvents);
        }
        return service;
    }

    @Test
    void createsAnOrganization() {
        when(organizations.findByUdeo("UDEO-1")).thenReturn(Optional.empty());
        when(organizations.save(org.mockito.ArgumentMatchers.any(Organizacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationDtos.Detail created = service().create(
                new OrganizationDtos.Create(" Hospital Norte ", "UDEO-1", "Un hospital"));

        ArgumentCaptor<Organizacion> saved = ArgumentCaptor.forClass(Organizacion.class);
        verify(organizations).save(saved.capture());
        assertEquals("Hospital Norte", saved.getValue().getNombre());
        assertEquals("UDEO-1", saved.getValue().getUdeo());
        assertEquals("Un hospital", saved.getValue().getDescripcion());
        assertEquals("Hospital Norte", created.nombre());
    }

    @Test
    void rejectsCreatingAnOrganizationWithAnExistingUdeo() {
        when(organizations.findByUdeo("UDEO-1")).thenReturn(Optional.of(mock(Organizacion.class)));

        ApiException error = assertThrows(ApiException.class, () -> service().create(
                new OrganizationDtos.Create("Hospital Sur", "UDEO-1", null)));

        assertEquals(409, error.status().value());
    }

    @Test
    void updatesAnOrganization() {
        var organization = mock(Organizacion.class);
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(organization.getId()).thenReturn(2L);
        when(organizations.findByUdeo("UDEO-2")).thenReturn(Optional.of(organization));
        when(organizations.save(organization)).thenReturn(organization);
        when(organization.getNombre()).thenReturn("Hospital Centro");
        when(organization.getUdeo()).thenReturn("UDEO-2");
        when(organization.getDescripcion()).thenReturn("Descripcion");
        when(organization.getCreatedAt()).thenReturn(java.time.Instant.now());

        OrganizationDtos.Detail updated = service().update(2L,
                new OrganizationDtos.Update("Hospital Centro", "UDEO-2", "Descripcion"));

        verify(organization).setNombre("Hospital Centro");
        assertEquals("UDEO-2", updated.udeo());
    }

    @Test
    void rejectsUpdatingToAUdeoOwnedByAnotherOrganization() {
        var organization = mock(Organizacion.class);
        var other = mock(Organizacion.class);
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(organization.getId()).thenReturn(2L);
        when(organizations.findByUdeo("UDEO-9")).thenReturn(Optional.of(other));
        when(other.getId()).thenReturn(9L);

        ApiException error = assertThrows(ApiException.class, () -> service().update(2L,
                new OrganizationDtos.Update("Hospital Centro", "UDEO-9", null)));

        assertEquals(409, error.status().value());
    }

    @Test
    void clearsBlankUdeoWhenUpdating() {
        var organization = new Organizacion();
        organization.setNombre("Hospital Centro");
        organization.setUdeo("UDEO-2");
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(organizations.save(organization)).thenReturn(organization);

        OrganizationDtos.Detail updated = service().update(2L,
                new OrganizationDtos.Update("Hospital Centro", "  ", null));

        assertNull(updated.udeo());
        assertNull(organization.getUdeo());
    }

    @Test
    void rejectsDeletingAnOrganizationWithAssociatedData() {
        var organization = mock(Organizacion.class);
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(organization.getId()).thenReturn(2L);
        when(memberships.existsByOrganizacionId(2L)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service().delete(2L));

        assertEquals(409, error.status().value());
    }

    @Test
    void deletesAnUnusedOrganization() {
        var organization = mock(Organizacion.class);
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(organization.getId()).thenReturn(2L);
        when(memberships.existsByOrganizacionId(2L)).thenReturn(false);
        when(imports.existsByOrganizacionId(2L)).thenReturn(false);
        when(personalImports.existsByOrganizacionId(2L)).thenReturn(false);
        when(designaciones.existsByOrganizacionId(2L)).thenReturn(false);
        when(history.existsByOrganizacionId(2L)).thenReturn(false);
        when(dependencias.existsByOrganizacionId(2L)).thenReturn(false);
        when(sectors.existsByOrganizacionId(2L)).thenReturn(false);
        when(auditEvents.existsByOrganizacionId(2L)).thenReturn(false);

        service().delete(2L);

        verify(organizations).delete(organization);
    }

    @Test
    void requiresTheGlobalManagementPermission() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "operador@example.com", Set.of(), java.util.Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));

        ApiException error = assertThrows(ApiException.class, () -> service().list());

        assertEquals(403, error.status().value());
    }
}
