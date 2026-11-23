package com.sisge.ms.service;

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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private static final String USER_MANAGEMENT = "USUARIOS_GESTIONAR";
    private static final Comparator<String> NAME_ORDER = Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);
    private static final char[] PASSWORD_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();

    private final UserRepository users;
    private final UserOrganizationRepository memberships;
    private final OrganizationRepository organizations;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public UserService(UserRepository users, UserOrganizationRepository memberships, OrganizationRepository organizations,
            RoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.memberships = memberships;
        this.organizations = organizations;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserDtos.Created createForOrganization(Long organizationId, UserDtos.Create request) {
        CurrentUser.get().requireForOrganization(organizationId, USER_MANAGEMENT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (users.findByEmail(email).isPresent()) {
            throw ApiException.conflict("A user with that email already exists");
        }
        if (users.findByUsername(username).isPresent()) {
            throw ApiException.conflict("A user with that username already exists");
        }

        var organization = organizations.findById(organizationId)
                .orElseThrow(() -> ApiException.notFound("Organization not found"));
        var role = roles.findByNombre(request.rol().trim())
                .orElseThrow(() -> ApiException.bad("Role not found"));
        if (role.isGlobalRole()) {
            throw ApiException.bad("A global role cannot be assigned to an organization user");
        }

        String password = request.password();
        boolean manualPassword = password != null && !password.isBlank();
        if (manualPassword && password.length() < 8) {
            throw ApiException.bad("Password must have at least 8 characters");
        }
        String effectivePassword = manualPassword ? password : temporaryPassword();
        Usuario user = new Usuario();
        user.register(request.nombre().trim(), request.apellido().trim(), email, username,
                passwordEncoder.encode(effectivePassword));
        users.save(user);

        UserOrganization membership = new UserOrganization();
        membership.assign(user, organization, role);
        memberships.save(membership);

        return new UserDtos.Created(user.getId(), user.getNombre(), user.getApellido(), user.getUsername(),
                user.getEmail(), organization.getId(), organization.getNombre(), role.getNombre(), effectivePassword);
    }

    @Transactional
    public UserDtos.MembershipRoleUpdated changeMembershipRole(Long organizationId, Long usuarioId,
            UserDtos.RoleChange request) {
        CurrentUser.get().requireForOrganization(organizationId, USER_MANAGEMENT);
        Usuario user = users.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        Organizacion organization = organizations.findById(organizationId)
                .orElseThrow(() -> ApiException.notFound("Organization not found"));
        Rol role = roles.findByNombre(request.rol().trim())
                .orElseThrow(() -> ApiException.bad("Role not found"));
        if (role.isGlobalRole()) {
            throw ApiException.bad("A global role cannot be assigned to an organization user");
        }
        UserOrganization membership = memberships.findByUsuarioIdAndOrganizacionId(usuarioId, organizationId)
                .orElseThrow(() -> ApiException.notFound("User does not belong to the organization"));
        membership.assign(user, organization, role);
        memberships.save(membership);
        return new UserDtos.MembershipRoleUpdated(user.getId(), user.getEmail(), organization.getId(),
                organization.getNombre(), role.getNombre());
    }

    @Transactional
    public UserDtos.GlobalRoleUpdated changeGlobalRole(Long usuarioId, UserDtos.GlobalRoleChange request) {
        CurrentUser.get().requireGlobal(USER_MANAGEMENT);
        Usuario user = users.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        Rol role = null;
        if (request.rol() != null && !request.rol().isBlank()) {
            role = roles.findByNombre(request.rol().trim())
                    .orElseThrow(() -> ApiException.bad("Role not found"));
            if (!role.isGlobalRole()) {
                throw ApiException.bad("Only a global role can be assigned as the global role");
            }
        }
        user.setRolGlobal(role);
        users.save(user);
        return new UserDtos.GlobalRoleUpdated(user.getId(), user.getEmail(),
                role == null ? null : role.getNombre());
    }

    @Transactional(readOnly = true)
    public List<UserDtos.Membership> listForOrganization(Long organizationId) {
        CurrentUser.get().requireForOrganization(organizationId, USER_MANAGEMENT);
        if (!organizations.existsById(organizationId)) {
            throw ApiException.notFound("Organization not found");
        }
        return memberships.findByOrganizacionId(organizationId).stream()
                .sorted(Comparator
                        .comparing((UserOrganization membership) -> membership.getUsuario().getApellido(), NAME_ORDER)
                        .thenComparing(membership -> membership.getUsuario().getNombre(), NAME_ORDER))
                .map(this::membershipDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDtos.Membership getMembership(Long organizationId, Long usuarioId) {
        CurrentUser.get().requireForOrganization(organizationId, USER_MANAGEMENT);
        UserOrganization membership = memberships.findByUsuarioIdAndOrganizacionId(usuarioId, organizationId)
                .orElseThrow(() -> ApiException.notFound("User does not belong to the organization"));
        return membershipDto(membership);
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserSummary> listUsers(String query) {
        CurrentUser.get().requireGlobal(USER_MANAGEMENT);
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return users.findAll(Sort.by(Sort.Order.asc("apellido"), Sort.Order.asc("nombre"))).stream()
                .filter(user -> matches(user, term))
                .map(this::userSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserDtos.UserOrganizationRow> listUserOrganizations(String query) {
        CurrentUser.get().requireGlobal(USER_MANAGEMENT);
        String term = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Map<Long, List<UserOrganization>> byUser = memberships.findAll().stream()
                .collect(Collectors.groupingBy(membership -> membership.getUsuario().getId()));
        List<UserDtos.UserOrganizationRow> rows = new ArrayList<>();
        for (Usuario user : users.findAll()) {
            List<UserOrganization> userMemberships = byUser.getOrDefault(user.getId(), List.of());
            if (userMemberships.isEmpty()) {
                rows.add(organizationRow(user, null));
            } else {
                userMemberships.forEach(membership -> rows.add(organizationRow(user, membership)));
            }
        }
        return rows.stream()
                .filter(row -> matchesOrganizationRow(row, term))
                .sorted(Comparator
                        .comparing(UserDtos.UserOrganizationRow::organizacionNombre, NAME_ORDER)
                        .thenComparing(UserDtos.UserOrganizationRow::apellido, NAME_ORDER)
                        .thenComparing(UserDtos.UserOrganizationRow::nombre, NAME_ORDER)
                        .thenComparing(UserDtos.UserOrganizationRow::username, NAME_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDtos.UserSummary getUser(Long usuarioId) {
        CurrentUser.get().requireGlobal(USER_MANAGEMENT);
        return userSummary(users.findById(usuarioId)
                .orElseThrow(() -> ApiException.notFound("User not found")));
    }

    private UserDtos.Membership membershipDto(UserOrganization membership) {
        Usuario user = membership.getUsuario();
        return new UserDtos.Membership(user.getId(), user.getNombre(), user.getApellido(), user.getUsername(),
                user.getEmail(), membership.getEstado(), membership.getRol().getNombre(), membership.getCreatedAt());
    }

    private UserDtos.UserSummary userSummary(Usuario user) {
        return new UserDtos.UserSummary(user.getId(), user.getNombre(), user.getApellido(), user.getUsername(),
                user.getEmail(), user.getEstado(), user.getRolGlobal() == null ? null : user.getRolGlobal().getNombre());
    }

    private UserDtos.UserOrganizationRow organizationRow(Usuario user, UserOrganization membership) {
        return new UserDtos.UserOrganizationRow(user.getId(), user.getNombre(), user.getApellido(),
                user.getUsername(), user.getEmail(), user.getEstado(),
                user.getRolGlobal() == null ? null : user.getRolGlobal().getNombre(),
                membership == null ? null : membership.getOrganizacion().getId(),
                membership == null ? null : membership.getOrganizacion().getNombre(),
                membership == null ? null : membership.getRol().getNombre(),
                membership == null ? null : membership.getEstado(),
                membership == null ? null : membership.getCreatedAt());
    }

    private boolean matchesOrganizationRow(UserDtos.UserOrganizationRow row, String term) {
        if (term.isEmpty()) {
            return true;
        }
        return contains(row.email(), term) || contains(row.username(), term)
                || contains(row.nombre(), term) || contains(row.apellido(), term)
                || contains(row.organizacionNombre(), term) || contains(row.rol(), term)
                || contains(row.rolGlobal(), term);
    }

    private boolean matches(Usuario user, String term) {
        if (term.isEmpty()) {
            return true;
        }
        return contains(user.getEmail(), term) || contains(user.getUsername(), term)
                || contains(user.getNombre(), term) || contains(user.getApellido(), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    private String temporaryPassword() {
        StringBuilder password = new StringBuilder(16);
        for (int index = 0; index < 16; index++) {
            password.append(PASSWORD_CHARACTERS[random.nextInt(PASSWORD_CHARACTERS.length)]);
        }
        return password.toString();
    }
}
