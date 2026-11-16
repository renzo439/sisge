package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sisge.ms.dto.usuario.UserDtos;
import com.sisge.ms.entity.Organizacion;
import com.sisge.ms.entity.Rol;
import com.sisge.ms.entity.UserOrganization;
import com.sisge.ms.entity.Usuario;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.repository.RoleRepository;
import com.sisge.ms.repository.UserOrganizationRepository;
import com.sisge.ms.repository.UserRepository;
import com.sisge.ms.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    @Mock
    private UserRepository users;
    @Mock
    private UserOrganizationRepository memberships;
    @Mock
    private OrganizationRepository organizations;
    @Mock
    private RoleRepository roles;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsAnOrganizationUserWithAnEncryptedTemporaryPassword() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of(), Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var organization = org.mockito.Mockito.mock(Organizacion.class);
        when(organization.getId()).thenReturn(2L);
        when(organization.getNombre()).thenReturn("Hospital Norte");
        var role = org.mockito.Mockito.mock(Rol.class);
        when(role.getNombre()).thenReturn("OPERADOR");
        when(role.isGlobalRole()).thenReturn(false);
        when(users.findByEmail("operator@example.com")).thenReturn(Optional.empty());
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(roles.findByNombre("OPERADOR")).thenReturn(Optional.of(role));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        UserDtos.Created created = service.createForOrganization(2L,
                new UserDtos.Create("Operator", "One", "Operator@Example.com", "OPERADOR"));

        ArgumentCaptor<Usuario> user = ArgumentCaptor.forClass(Usuario.class);
        verify(users).save(user.capture());
        verify(memberships).save(org.mockito.ArgumentMatchers.any());
        assertEquals("operator@example.com", created.email());
        assertEquals(16, created.passwordTemporal().length());
        assertTrue(encoder.matches(created.passwordTemporal(), user.getValue().getPasswordHash()));
    }

    @Test
    void changesTheRoleOfAnOrganizationMembership() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of(), Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var user = org.mockito.Mockito.mock(Usuario.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("operator@example.com");
        var organization = org.mockito.Mockito.mock(Organizacion.class);
        when(organization.getId()).thenReturn(2L);
        when(organization.getNombre()).thenReturn("Hospital Norte");
        var role = org.mockito.Mockito.mock(Rol.class);
        when(role.getNombre()).thenReturn("CONSULTA");
        when(role.isGlobalRole()).thenReturn(false);
        var membership = org.mockito.Mockito.mock(UserOrganization.class);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(roles.findByNombre("CONSULTA")).thenReturn(Optional.of(role));
        when(memberships.findByUsuarioIdAndOrganizacionId(7L, 2L)).thenReturn(Optional.of(membership));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        UserDtos.MembershipRoleUpdated updated = service.changeMembershipRole(2L, 7L,
                new UserDtos.RoleChange("CONSULTA"));

        verify(membership).assign(user, organization, role);
        verify(memberships).save(membership);
        assertEquals("CONSULTA", updated.rol());
        assertEquals("Hospital Norte", updated.organizacionNombre());
    }

    @Test
    void rejectsChangingAMembershipToAGlobalRole() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of(), Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var user = org.mockito.Mockito.mock(Usuario.class);
        var organization = org.mockito.Mockito.mock(Organizacion.class);
        var role = org.mockito.Mockito.mock(Rol.class);
        when(role.isGlobalRole()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(roles.findByNombre("ADMIN_GENERAL")).thenReturn(Optional.of(role));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        ApiException error = assertThrows(ApiException.class, () -> service.changeMembershipRole(2L, 7L,
                new UserDtos.RoleChange("ADMIN_GENERAL")));

        assertEquals(400, error.status().value());
    }

    @Test
    void rejectsChangingTheRoleOfAUserOutsideTheOrganization() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of(), Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var user = org.mockito.Mockito.mock(Usuario.class);
        var organization = org.mockito.Mockito.mock(Organizacion.class);
        var role = org.mockito.Mockito.mock(Rol.class);
        when(role.isGlobalRole()).thenReturn(false);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(organizations.findById(2L)).thenReturn(Optional.of(organization));
        when(roles.findByNombre("CONSULTA")).thenReturn(Optional.of(role));
        when(memberships.findByUsuarioIdAndOrganizacionId(7L, 2L)).thenReturn(Optional.empty());
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        ApiException error = assertThrows(ApiException.class, () -> service.changeMembershipRole(2L, 7L,
                new UserDtos.RoleChange("CONSULTA")));

        assertEquals(404, error.status().value());
    }

    @Test
    void assignsAGlobalRoleToAnExistingUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("USUARIOS_GESTIONAR"), Map.of(), true),
                null, List.of()));
        var user = org.mockito.Mockito.mock(Usuario.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("nuevo@example.com");
        var role = org.mockito.Mockito.mock(Rol.class);
        when(role.getNombre()).thenReturn("ADMIN_GENERAL");
        when(role.isGlobalRole()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(roles.findByNombre("ADMIN_GENERAL")).thenReturn(Optional.of(role));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        UserDtos.GlobalRoleUpdated updated = service.changeGlobalRole(7L,
                new UserDtos.GlobalRoleChange("ADMIN_GENERAL"));

        verify(user).setRolGlobal(role);
        assertEquals("ADMIN_GENERAL", updated.rol());
    }

    @Test
    void removesTheGlobalRoleWhenTheRequestIsBlank() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("USUARIOS_GESTIONAR"), Map.of(), true),
                null, List.of()));
        var user = org.mockito.Mockito.mock(Usuario.class);
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("nuevo@example.com");
        when(users.findById(7L)).thenReturn(Optional.of(user));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        UserDtos.GlobalRoleUpdated updated = service.changeGlobalRole(7L,
                new UserDtos.GlobalRoleChange("   "));

        verify(user).setRolGlobal(null);
        assertNull(updated.rol());
    }

    @Test
    void rejectsANonGlobalRoleAsGlobalRole() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("USUARIOS_GESTIONAR"), Map.of(), true),
                null, List.of()));
        var user = org.mockito.Mockito.mock(Usuario.class);
        var role = org.mockito.Mockito.mock(Rol.class);
        when(role.isGlobalRole()).thenReturn(false);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(roles.findByNombre("OPERADOR")).thenReturn(Optional.of(role));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        ApiException error = assertThrows(ApiException.class, () -> service.changeGlobalRole(7L,
                new UserDtos.GlobalRoleChange("OPERADOR")));

        assertEquals(400, error.status().value());
    }

    @Test
    void requiresGlobalPermissionToChangeAGlobalRole() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "operador@example.com", Set.of(), Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        ApiException error = assertThrows(ApiException.class, () -> service.changeGlobalRole(7L,
                new UserDtos.GlobalRoleChange("ADMIN_GENERAL")));

        assertEquals(403, error.status().value());
    }

    @Test
    void listsTheUsersOfAnOrganizationWithTheirCurrentRole() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of(), Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var membership = membership(7L, "Perez", "Ana", "ana@example.com", "CONSULTA");
        when(organizations.existsById(2L)).thenReturn(true);
        when(memberships.findByOrganizacionId(2L)).thenReturn(List.of(membership));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        List<UserDtos.Membership> result = service.listForOrganization(2L);

        assertEquals(1, result.size());
        assertEquals("ana@example.com", result.get(0).email());
        assertEquals("CONSULTA", result.get(0).rol());
        assertEquals("ACTIVO", result.get(0).estado());
    }

    @Test
    void returnsTheCurrentRoleOfAMembership() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of(), Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var membership = membership(7L, "Perez", "Ana", "ana@example.com", "OPERADOR");
        when(memberships.findByUsuarioIdAndOrganizacionId(7L, 2L)).thenReturn(Optional.of(membership));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        UserDtos.Membership current = service.getMembership(2L, 7L);

        assertEquals("OPERADOR", current.rol());
        assertEquals(7L, current.usuarioId());
    }

    @Test
    void rejectsListingUsersOutsideTheOrganization() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "operador@example.com", Set.of(), Map.of(3L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        ApiException error = assertThrows(ApiException.class, () -> service.listForOrganization(2L));

        assertEquals(403, error.status().value());
    }

    @Test
    void listsAllUsersForGlobalAdminAndFiltersByQuery() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("USUARIOS_GESTIONAR"), Map.of(), true),
                null, List.of()));
        var ana = user(7L, "Perez", "Ana", "ana@example.com", null);
        var globalRole = org.mockito.Mockito.mock(Rol.class);
        when(globalRole.getNombre()).thenReturn("ADMIN_GENERAL");
        var bruno = user(8L, "Gomez", "Bruno", "bruno@example.com", globalRole);
        when(users.findAll(org.mockito.ArgumentMatchers.any(Sort.class))).thenReturn(List.of(ana, bruno));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        List<UserDtos.UserSummary> result = service.listUsers("ana");

        assertEquals(1, result.size());
        assertEquals("ana@example.com", result.get(0).email());
        assertNull(result.get(0).rolGlobal());

        List<UserDtos.UserSummary> all = service.listUsers(null);
        assertEquals(2, all.size());
    }

    @Test
    void rejectsListingAllUsersWithoutGlobalPermission() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "operador@example.com", Set.of(), Map.of(2L, Set.of("USUARIOS_GESTIONAR")), false),
                null, List.of()));
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        ApiException error = assertThrows(ApiException.class, () -> service.listUsers(null));

        assertEquals(403, error.status().value());
    }

    @Test
    void rejectsGettingAnUnknownUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("USUARIOS_GESTIONAR"), Map.of(), true),
                null, List.of()));
        when(users.findById(99L)).thenReturn(Optional.empty());
        var encoder = new BCryptPasswordEncoder();
        var service = new UserService(users, memberships, organizations, roles, encoder);

        ApiException error = assertThrows(ApiException.class, () -> service.getUser(99L));

        assertEquals(404, error.status().value());
    }

    private UserOrganization membership(Long userId, String apellido, String nombre, String email, String roleName) {
        var user = user(userId, apellido, nombre, email, null);
        var role = org.mockito.Mockito.mock(Rol.class);
        org.mockito.Mockito.lenient().when(role.getNombre()).thenReturn(roleName);
        var membership = org.mockito.Mockito.mock(UserOrganization.class);
        org.mockito.Mockito.lenient().when(membership.getUsuario()).thenReturn(user);
        org.mockito.Mockito.lenient().when(membership.getRol()).thenReturn(role);
        org.mockito.Mockito.lenient().when(membership.getEstado()).thenReturn("ACTIVO");
        org.mockito.Mockito.lenient().when(membership.getCreatedAt()).thenReturn(java.time.Instant.now());
        return membership;
    }

    private Usuario user(Long id, String apellido, String nombre, String email, Rol globalRole) {
        var user = org.mockito.Mockito.mock(Usuario.class);
        org.mockito.Mockito.lenient().when(user.getId()).thenReturn(id);
        org.mockito.Mockito.lenient().when(user.getApellido()).thenReturn(apellido);
        org.mockito.Mockito.lenient().when(user.getNombre()).thenReturn(nombre);
        org.mockito.Mockito.lenient().when(user.getEmail()).thenReturn(email);
        org.mockito.Mockito.lenient().when(user.getEstado()).thenReturn("ACTIVO");
        org.mockito.Mockito.lenient().when(user.getRolGlobal()).thenReturn(globalRole);
        return user;
    }
}
