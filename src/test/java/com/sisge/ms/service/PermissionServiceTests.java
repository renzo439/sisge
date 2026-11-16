package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sisge.ms.dto.permission.PermissionDtos;
import com.sisge.ms.entity.Permission;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.PermissionRepository;
import com.sisge.ms.repository.RoleRepository;
import com.sisge.ms.security.CurrentUser;
import java.util.List;
import java.util.Map;
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
class PermissionServiceTests {
    @Mock
    private PermissionRepository permissions;
    @Mock
    private RoleRepository roles;

    private PermissionService service;

    @BeforeEach
    void loginAsGlobalAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("PERMISOS_GESTIONAR"), Map.of(), true),
                null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private PermissionService service() {
        if (service == null) {
            service = new PermissionService(permissions, roles);
        }
        return service;
    }

    @Test
    void listsPermissionsOrderedByCode() {
        var importaciones = permission(1L, "IMPORTACIONES_VER", "Consulta");
        var rolesGestionar = permission(2L, "ROLES_GESTIONAR", "Roles");
        when(permissions.findAllByOrderByCodigoAsc()).thenReturn(List.of(importaciones, rolesGestionar));

        List<PermissionDtos.Summary> result = service().list();

        assertEquals(2, result.size());
        assertEquals("IMPORTACIONES_VER", result.get(0).codigo());
    }

    @Test
    void allowsListingWithTheRoleManagementPermission() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("ROLES_GESTIONAR"), Map.of(), true),
                null, List.of()));
        when(permissions.findAllByOrderByCodigoAsc()).thenReturn(List.of());

        assertEquals(0, service().list().size());
    }

    @Test
    void createsAPermissionNormalizingTheCode() {
        when(permissions.existsByCodigo("NUEVO_PERMISO")).thenReturn(false);
        when(permissions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PermissionDtos.Summary created = service().create(
                new PermissionDtos.Create("  nuevo_permiso ", "Descripcion"));

        ArgumentCaptor<Permission> saved = ArgumentCaptor.forClass(Permission.class);
        verify(permissions).save(saved.capture());
        assertEquals("NUEVO_PERMISO", saved.getValue().getCodigo());
        assertEquals("Descripcion", saved.getValue().getDescripcion());
        assertEquals("NUEVO_PERMISO", created.codigo());
    }

    @Test
    void rejectsCreatingADuplicatedCode() {
        when(permissions.existsByCodigo("ROLES_GESTIONAR")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service().create(
                new PermissionDtos.Create("ROLES_GESTIONAR", null)));

        assertEquals(409, error.status().value());
    }

    @Test
    void updatesAPermission() {
        Permission permission = new Permission();
        permission.setCodigo("OLD_CODE");
        permission.setDescripcion("Vieja");
        when(permissions.findById(5L)).thenReturn(Optional.of(permission));
        when(permissions.existsByCodigo("NEW_CODE")).thenReturn(false);
        when(permissions.save(permission)).thenReturn(permission);

        PermissionDtos.Summary updated = service().update(5L,
                new PermissionDtos.Update("new_code", "Nueva"));

        assertEquals("NEW_CODE", updated.codigo());
        assertEquals("Nueva", updated.descripcion());
    }

    @Test
    void rejectsUpdatingToACodeUsedByAnotherPermission() {
        Permission permission = new Permission();
        permission.setCodigo("OLD_CODE");
        when(permissions.findById(5L)).thenReturn(Optional.of(permission));
        when(permissions.existsByCodigo("OTRO")).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service().update(5L,
                new PermissionDtos.Update("otro", null)));

        assertEquals(409, error.status().value());
    }

    @Test
    void rejectsDeletingAPermissionAssignedToRoles() {
        Permission permission = permission(5L, "IMPORTACIONES_VER", "Consulta");
        when(permissions.findById(5L)).thenReturn(Optional.of(permission));
        when(roles.existsByPermisosId(5L)).thenReturn(true);

        ApiException error = assertThrows(ApiException.class, () -> service().delete(5L));

        assertEquals(409, error.status().value());
    }

    @Test
    void deletesAnUnusedPermission() {
        Permission permission = permission(5L, "NUEVO_PERMISO", "Descripcion");
        when(permissions.findById(5L)).thenReturn(Optional.of(permission));
        when(roles.existsByPermisosId(5L)).thenReturn(false);

        service().delete(5L);

        verify(permissions).delete(permission);
    }

    @Test
    void rejectsManagementWithoutThePermission() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("ROLES_GESTIONAR"), Map.of(), true),
                null, List.of()));

        ApiException error = assertThrows(ApiException.class, () -> service().create(
                new PermissionDtos.Create("NUEVO", null)));

        assertEquals(403, error.status().value());
    }

    private Permission permission(Long id, String codigo, String descripcion) {
        Permission permission = mock(Permission.class);
        org.mockito.Mockito.lenient().when(permission.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(permission.getCodigo()).thenReturn(codigo);
        org.mockito.Mockito.lenient().when(permission.getDescripcion()).thenReturn(descripcion);
        return permission;
    }
}
