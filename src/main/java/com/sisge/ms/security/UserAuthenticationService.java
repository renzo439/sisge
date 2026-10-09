package com.sisge.ms.security;

import com.sisge.ms.entity.Usuario;
import com.sisge.ms.repository.UserOrganizationRepository;
import com.sisge.ms.repository.UserRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAuthenticationService {
    private final UserRepository users;
    private final UserOrganizationRepository memberships;

    public UserAuthenticationService(UserRepository users, UserOrganizationRepository memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    public CurrentUser load(String email) {
        Usuario user = users.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));
        if (!"ACTIVO".equals(user.getEstado())) {
            throw new UsernameNotFoundException(email);
        }

        boolean global = user.getRolGlobal() != null && user.getRolGlobal().isGlobalRole();
        Set<String> globalPermissions = new HashSet<>();
        if (global) {
            user.getRolGlobal().getPermisos().forEach(permission -> globalPermissions.add(permission.getCodigo()));
        }

        Map<Long, Set<String>> organizationPermissions = new HashMap<>();
        memberships.findByUsuarioIdAndEstado(user.getId(), "ACTIVO").forEach(membership -> {
            Set<String> permissions = new HashSet<>();
            membership.getRol().getPermisos().forEach(permission -> permissions.add(permission.getCodigo()));
            organizationPermissions.put(membership.getOrganizacion().getId(), Set.copyOf(permissions));
        });

        return new CurrentUser(
                user.getId(), user.getEmail(), Set.copyOf(globalPermissions),
                Map.copyOf(organizationPermissions), global);
    }

    public Usuario entity(String email) {
        return users.findByEmail(email).orElseThrow();
    }
}
