package com.sisge.ms.service;

import com.sisge.ms.dto.role.RoleDtos;
import com.sisge.ms.entity.Permission;
import com.sisge.ms.entity.Rol;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.PermissionRepository;
import com.sisge.ms.repository.RoleRepository;
import com.sisge.ms.repository.UserOrganizationRepository;
import com.sisge.ms.repository.UserRepository;
import com.sisge.ms.security.CurrentUser;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {
    private static final String ROLES_GESTIONAR = "ROLES_GESTIONAR";

    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final UserRepository users;
    private final UserOrganizationRepository memberships;

    public RoleService(RoleRepository roles, PermissionRepository permissions, UserRepository users,
            UserOrganizationRepository memberships) {
        this.roles = roles;
        this.permissions = permissions;
        this.users = users;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public java.util.List<RoleDtos.Summary> list() {
        CurrentUser.get().requireGlobal(ROLES_GESTIONAR);
        return roles.findAll(Sort.by("nombre")).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public RoleDtos.Summary get(Long id) {
        CurrentUser.get().requireGlobal(ROLES_GESTIONAR);
        return summary(findRole(id));
    }

    @Transactional
    public RoleDtos.Summary create(RoleDtos.Create request) {
        CurrentUser.get().requireGlobal(ROLES_GESTIONAR);
        String nombre = request.nombre().trim();
        if (roles.findByNombre(nombre).isPresent()) {
            throw ApiException.conflict("A role with that name already exists");
        }
        Rol role = new Rol();
        role.setNombre(nombre);
        role.setDescripcion(request.descripcion());
        role.setGlobalRole(request.globalRole());
        role.setPermisos(resolvePermissions(request.permisos()));
        return summary(roles.save(role));
    }

    @Transactional
    public RoleDtos.Summary update(Long id, RoleDtos.Update request) {
        CurrentUser.get().requireGlobal(ROLES_GESTIONAR);
        Rol role = findRole(id);
        String nombre = request.nombre().trim();
        roles.findByNombre(nombre)
                .filter(existing -> !existing.getId().equals(role.getId()))
                .ifPresent(existing -> {
                    throw ApiException.conflict("A role with that name already exists");
                });
        role.setNombre(nombre);
        role.setDescripcion(request.descripcion());
        role.setGlobalRole(request.globalRole());
        role.setPermisos(resolvePermissions(request.permisos()));
        return summary(roles.save(role));
    }

    @Transactional
    public void delete(Long id) {
        CurrentUser.get().requireGlobal(ROLES_GESTIONAR);
        Rol role = findRole(id);
        if (memberships.existsByRolId(role.getId())) {
            throw ApiException.conflict("The role is assigned to organization users and cannot be deleted");
        }
        if (users.existsByRolGlobalId(role.getId())) {
            throw ApiException.conflict("The role is assigned as global role to users and cannot be deleted");
        }
        role.setPermisos(Set.of());
        roles.save(role);
        roles.delete(role);
    }

    private Rol findRole(Long id) {
        return roles.findById(id)
                .orElseThrow(() -> ApiException.notFound("Role not found"));
    }

    private Set<Permission> resolvePermissions(Set<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return Set.of();
        }
        Set<String> requested = new TreeSet<>();
        codigos.forEach(codigo -> {
            String value = codigo == null ? "" : codigo.trim();
            if (!value.isEmpty()) {
                requested.add(value);
            }
        });
        if (requested.isEmpty()) {
            return Set.of();
        }
        Set<Permission> found = new LinkedHashSet<>(permissions.findByCodigoIn(requested));
        if (found.size() != requested.size()) {
            Set<String> known = found.stream().map(Permission::getCodigo).collect(java.util.stream.Collectors.toSet());
            requested.removeAll(known);
            throw ApiException.bad("Unknown permissions: " + String.join(", ", requested));
        }
        return found;
    }

    private RoleDtos.Summary summary(Rol role) {
        Set<String> codigos = new TreeSet<>();
        role.getPermisos().forEach(permission -> codigos.add(permission.getCodigo()));
        return new RoleDtos.Summary(role.getId(), role.getNombre(), role.getDescripcion(),
                role.isGlobalRole(), codigos);
    }
}
