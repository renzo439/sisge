package com.sisge.ms.security;

import com.sisge.ms.exception.ApiException;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;

public record CurrentUser(
        Long id,
        String email,
        Set<String> globalPermissions,
        Map<Long, Set<String>> organizationPermissions,
        boolean global) {

    public static CurrentUser get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw ApiException.forbidden("Authentication required");
        }
        return user;
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
