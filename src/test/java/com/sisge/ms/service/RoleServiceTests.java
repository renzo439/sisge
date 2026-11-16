package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sisge.ms.dto.role.RoleDtos;
import com.sisge.ms.entity.Permission;
import com.sisge.ms.entity.Rol;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.PermissionRepository;
import com.sisge.ms.repository.RoleRepository;
import com.sisge.ms.repository.UserOrganizationRepository;
import com.sisge.ms.repository.UserRepository;
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
class RoleServiceTests {
    @Mock
    private RoleRepository roles;
    @Mock
    private PermissionRepository permissions;
    @Mock
    private UserRepository users;
    @Mock
    private UserOrganizationRepository memberships;

    private RoleService service;

    @BeforeEach
    void loginAsGlobalAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("ROLES_GESTIONAR"), java.util.Map.of(), true),
                null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsARoleWithResolvedPermissions() {
        var permission = mock(Permission.class);
        when(permission.getCodigo()).thenReturn("IMPORTACIONES_VER");
        when(roles.findByNombre("Supervisor")).thenReturn(Optional.empty());
        when(permissions.findByCodigoIn(any())).thenReturn(List.of(permission));
        when(roles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new RoleService(roles, permissions, users, memberships);

        RoleDtos.Summary created = service.create(
                new RoleDtos.Create(" Supervisor ", "Revisa importaciones", false, Set.of("IMPORTACIONES_VER")));

        ArgumentCaptor<Rol> saved = ArgumentCaptor.forClass(Rol.class);
        verify(roles).save(saved.capture());
        assertEquals("Supervisor", saved.getValue().getNombre());
        assertEquals("Revisa importaciones", saved.getValue().getDescripcion());
        assertEquals(1, saved.getValue().getPermisos().size());
        assertEquals(Set.of("IMPORTACIONES_VER"), created.permisos());
    }

    @Test
    void rejectsDuplicatedRoleName() {
        when(roles.findByNombre("OPERADOR")).thenReturn(Optional.of(mock(Rol.class)));
        service = new RoleService(roles, permissions, users, memberships);

        ApiException error = assertThrows(ApiException.class, () -> service.create(
                new RoleDtos.Create("OPERADOR", null, false, Set.of())));

        assertEquals(409, error.status().value());
    }

    @Test
    void rejectsUnknownPermissions() {
        when(roles.findByNombre("NUEVO")).thenReturn(Optional.empty());
        when(permissions.findByCodigoIn(any())).thenReturn(List.of());
        service = new RoleService(roles, permissions, users, memberships);

        ApiException error = assertThrows(ApiException.class, () -> service.create(
                new RoleDtos.Create("NUEVO", null, false, Set.of("PERMISO_INEXISTENTE"))));

        assertEquals(400, error.status().value());
    }

    @Test
    void updatesAnExistingRole() {
        Rol role = mock(Rol.class);
        when(roles.findById(5L)).thenReturn(Optional.of(role));
        when(role.getId()).thenReturn(5L);
        when(roles.findByNombre("SUPERVISOR")).thenReturn(Optional.empty());
        when(role.getPermisos()).thenReturn(Set.of());
        when(roles.save(role)).thenReturn(role);
        when(role.getNombre()).thenReturn("SUPERVISOR");
        when(role.getDescripcion()).thenReturn("Nueva descripcion");
        when(role.isGlobalRole()).thenReturn(true);
        service = new RoleService(roles, permissions, users, memberships);

        RoleDtos.Summary updated = service.update(5L,
                new RoleDtos.Update("SUPERVISOR", "Nueva descripcion", true, Set.of()));

        verify(role).setNombre("SUPERVISOR");
        verify(role).setGlobalRole(true);
        assertEquals("SUPERVISOR", updated.nombre());
        assertTrue(updated.globalRole());
    }

    @Test
    void rejectsUpdatingToANameUsedByAnotherRole() {
        Rol role = mock(Rol.class);
        Rol other = mock(Rol.class);
        when(roles.findById(5L)).thenReturn(Optional.of(role));
        when(role.getId()).thenReturn(5L);
        when(roles.findByNombre("OPERADOR")).thenReturn(Optional.of(other));
        when(other.getId()).thenReturn(9L);
        service = new RoleService(roles, permissions, users, memberships);

        ApiException error = assertThrows(ApiException.class, () -> service.update(5L,
                new RoleDtos.Update("OPERADOR", null, false, Set.of())));

        assertEquals(409, error.status().value());
    }

    @Test
    void rejectsDeletingARoleAssignedToOrganizationUsers() {
        Rol role = mock(Rol.class);
        when(roles.findById(5L)).thenReturn(Optional.of(role));
        when(role.getId()).thenReturn(5L);
        when(memberships.existsByRolId(5L)).thenReturn(true);
        service = new RoleService(roles, permissions, users, memberships);

        ApiException error = assertThrows(ApiException.class, () -> service.delete(5L));

        assertEquals(409, error.status().value());
    }

    @Test
    void rejectsDeletingARoleAssignedAsGlobalRole() {
        Rol role = mock(Rol.class);
        when(roles.findById(5L)).thenReturn(Optional.of(role));
        when(role.getId()).thenReturn(5L);
        when(memberships.existsByRolId(5L)).thenReturn(false);
        when(users.existsByRolGlobalId(5L)).thenReturn(true);
        service = new RoleService(roles, permissions, users, memberships);

        ApiException error = assertThrows(ApiException.class, () -> service.delete(5L));

        assertEquals(409, error.status().value());
    }

    @Test
    void deletesAnUnusedRole() {
        Rol role = mock(Rol.class);
        when(roles.findById(5L)).thenReturn(Optional.of(role));
        when(role.getId()).thenReturn(5L);
        when(memberships.existsByRolId(5L)).thenReturn(false);
        when(users.existsByRolGlobalId(5L)).thenReturn(false);
        when(roles.save(role)).thenReturn(role);
        service = new RoleService(roles, permissions, users, memberships);

        service.delete(5L);

        verify(role).setPermisos(Set.of());
        verify(roles).delete(role);
    }
}
