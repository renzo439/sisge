package com.sisge.ms.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sisge.ms.dto.organizacion.OrganizationDtos;
import com.sisge.ms.entity.Organizacion;
import com.sisge.ms.entity.Rol;
import com.sisge.ms.entity.UserOrganization;
import com.sisge.ms.entity.Usuario;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.repository.UserOrganizationRepository;
import com.sisge.ms.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthControllerTests {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCurrentUserPermissions() {
        CurrentUser user = new CurrentUser(7L, "user@example.com", "user.one", "Ana", "Pérez", "OPERADOR",
                List.of(new OrganizationDtos.Summary(3L, "Hospital Norte", "HN")),
                Set.of("PERMISOS_GESTIONAR"), Map.of(3L, Set.of("REEMPLAZANTES_VER")), false, 3L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AuthController.Me me = new AuthController(null, null, null).me();

        assertEquals(7L, me.id());
        assertEquals("Ana", me.nombre());
        assertEquals("Pérez", me.apellido());
        assertEquals("user.one", me.username());
        assertEquals("user@example.com", me.email());
        assertEquals("OPERADOR", me.rolGlobal());
        assertFalse(me.global());
        assertEquals(Set.of("PERMISOS_GESTIONAR", "REEMPLAZANTES_VER"), me.permissions());
        assertEquals(3L, me.organizacionId());
        assertEquals(new OrganizationDtos.Summary(3L, "Hospital Norte", "HN"), me.organizacion());
        assertEquals(List.of(new OrganizationDtos.Summary(3L, "Hospital Norte", "HN")), me.organizaciones());
        assertEquals(Set.of("PERMISOS_GESTIONAR"), me.globalPermissions());
        assertEquals(Map.of(3L, Set.of("REEMPLAZANTES_VER")), me.organizationPermissions());
    }

    @Test
    void resolvesTheIdentifierAsEmailOrUsername() {
        UserRepository repository = mock(UserRepository.class);
        Usuario user = mock(Usuario.class);
        when(repository.findByEmailOrUsername("operator.one", "operator.one")).thenReturn(Optional.of(user));
        var service = new UserAuthenticationService(repository, mock(UserOrganizationRepository.class),
                mock(OrganizationRepository.class));

        assertSame(user, service.entity(" Operator.One "));
    }

    @Test
    void loadsAllOrganizationsForAGlobalUser() {
        UserRepository repository = mock(UserRepository.class);
        UserOrganizationRepository memberships = mock(UserOrganizationRepository.class);
        OrganizationRepository organizations = mock(OrganizationRepository.class);
        Usuario user = mock(Usuario.class);
        Rol role = mock(Rol.class);
        when(role.isGlobalRole()).thenReturn(true);
        when(role.getPermisos()).thenReturn(Set.of());
        when(role.getNombre()).thenReturn("ADMIN_GENERAL");
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("admin@example.com");
        when(user.getUsername()).thenReturn("admin");
        when(user.getNombre()).thenReturn("Administrador");
        when(user.getApellido()).thenReturn("General");
        when(user.getEstado()).thenReturn("ACTIVO");
        when(user.getRolGlobal()).thenReturn(role);
        when(repository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(memberships.findByUsuarioIdAndEstado(1L, "ACTIVO")).thenReturn(List.of());
        Organizacion norte = organization(2L, "Hospital Norte", "HN");
        Organizacion sur = organization(3L, "Hospital Sur", "HS");
        when(organizations.findAll()).thenReturn(List.of(sur, norte));
        var service = new UserAuthenticationService(repository, memberships, organizations);

        CurrentUser current = service.load("admin@example.com");

        assertTrue(current.global());
        assertEquals("admin", current.username());
        assertEquals("Administrador", current.nombre());
        assertEquals("ADMIN_GENERAL", current.rolGlobal());
        assertEquals(List.of(new OrganizationDtos.Summary(2L, "Hospital Norte", "HN"),
                new OrganizationDtos.Summary(3L, "Hospital Sur", "HS")), current.organizaciones());
    }

    @Test
    void loadsOnlyAssignedOrganizationsForARegularUser() {
        UserRepository repository = mock(UserRepository.class);
        UserOrganizationRepository memberships = mock(UserOrganizationRepository.class);
        OrganizationRepository organizations = mock(OrganizationRepository.class);
        Usuario user = mock(Usuario.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("ana@example.com");
        when(user.getUsername()).thenReturn("ana");
        when(user.getNombre()).thenReturn("Ana");
        when(user.getApellido()).thenReturn("Pérez");
        when(user.getEstado()).thenReturn("ACTIVO");
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        Organizacion organizacion = organization(2L, "Hospital Norte", "HN");
        Rol role = mock(Rol.class);
        when(role.getPermisos()).thenReturn(Set.of());
        UserOrganization membership = mock(UserOrganization.class);
        when(membership.getOrganizacion()).thenReturn(organizacion);
        when(membership.getRol()).thenReturn(role);
        when(memberships.findByUsuarioIdAndEstado(7L, "ACTIVO")).thenReturn(List.of(membership));
        var service = new UserAuthenticationService(repository, memberships, organizations);

        CurrentUser current = service.load("ana@example.com");

        assertFalse(current.global());
        assertEquals(List.of(new OrganizationDtos.Summary(2L, "Hospital Norte", "HN")), current.organizaciones());
        assertEquals(Map.of(2L, Set.of()), current.organizationPermissions());
    }

    @Test
    void picksOldestActiveMembershipAsTheOrganizationScope() {
        UserRepository repository = mock(UserRepository.class);
        UserOrganizationRepository memberships = mock(UserOrganizationRepository.class);
        OrganizationRepository organizations = mock(OrganizationRepository.class);
        Usuario user = mock(Usuario.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("ana@example.com");
        when(user.getUsername()).thenReturn("ana");
        when(user.getNombre()).thenReturn("Ana");
        when(user.getApellido()).thenReturn("Pérez");
        when(user.getEstado()).thenReturn("ACTIVO");
        when(repository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));

        Organizacion norte = organization(2L, "Hospital Norte", "HN");
        Organizacion sur = organization(3L, "Hospital Sur", "HS");
        Rol role = mock(Rol.class);
        when(role.getPermisos()).thenReturn(Set.of());
        UserOrganization newer = mock(UserOrganization.class);
        when(newer.getOrganizacion()).thenReturn(norte);
        when(newer.getRol()).thenReturn(role);
        when(newer.getCreatedAt()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        UserOrganization older = mock(UserOrganization.class);
        when(older.getOrganizacion()).thenReturn(sur);
        when(older.getRol()).thenReturn(role);
        when(older.getCreatedAt()).thenReturn(Instant.parse("2025-01-01T00:00:00Z"));
        when(memberships.findByUsuarioIdAndEstado(7L, "ACTIVO")).thenReturn(List.of(newer, older));
        var service = new UserAuthenticationService(repository, memberships, organizations);

        CurrentUser current = service.load("ana@example.com");

        assertEquals(3L, current.organizacionId());
    }

    private Organizacion organization(Long id, String nombre, String udeo) {
        Organizacion organization = mock(Organizacion.class);
        when(organization.getId()).thenReturn(id);
        when(organization.getNombre()).thenReturn(nombre);
        when(organization.getUdeo()).thenReturn(udeo);
        return organization;
    }
}
