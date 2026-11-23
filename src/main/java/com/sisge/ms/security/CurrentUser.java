package com.sisge.ms.security;

import com.sisge.ms.dto.organizacion.OrganizationDtos;
import com.sisge.ms.exception.ApiException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;

public record CurrentUser(
        Long id,
        String email,
        String username,
        String nombre,
        String apellido,
        String rolGlobal,
        List<OrganizationDtos.Summary> organizaciones,
        Set<String> globalPermissions,
        Map<Long, Set<String>> organizationPermissions,
        boolean global,
        Long organizacionId) {

    public CurrentUser(Long id, String email, Set<String> globalPermissions,
            Map<Long, Set<String>> organizationPermissions, boolean global) {
        this(id, email, null, null, null, null, List.of(), globalPermissions, organizationPermissions, global, null);
    }

    public CurrentUser(Long id, String email, String username, String nombre, String apellido, String rolGlobal,
            List<OrganizationDtos.Summary> organizaciones, Set<String> globalPermissions,
            Map<Long, Set<String>> organizationPermissions, boolean global) {
        this(id, email, username, nombre, apellido, rolGlobal, organizaciones, globalPermissions,
                organizationPermissions, global, null);
    }

    public static CurrentUser get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw ApiException.forbidden("Authentication required");
        }
        return user;
    }

    public Set<String> permissions() {
        Set<String> permissions = new HashSet<>(globalPermissions);
        organizationPermissions.values().forEach(permissions::addAll);
        return Set.copyOf(permissions);
    }

    public void requireGlobal(String permission) {
        if (!global || !globalPermissions.contains(permission)) {
            throw ApiException.forbidden("Missing global permission: " + permission);
        }
    }

    public void requireForOrganization(Long organizationId, String permission) {
        if (global) {
            requireGlobal(permission);
            return;
        }
        if (!organizationPermissions.getOrDefault(organizationId, Set.of()).contains(permission)) {
            throw ApiException.forbidden("Missing organization permission: " + permission);
        }
    }
}
