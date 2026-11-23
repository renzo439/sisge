package com.sisge.ms.security;

import com.sisge.ms.dto.organizacion.OrganizationDtos;
import com.sisge.ms.entity.Organizacion;
import com.sisge.ms.entity.UserOrganization;
import com.sisge.ms.entity.Usuario;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.repository.UserOrganizationRepository;
import com.sisge.ms.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserAuthenticationService {
    private static final Comparator<Organizacion> ORGANIZATION_ORDER = Comparator
            .comparing(Organizacion::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(Organizacion::getId, Comparator.nullsLast(Long::compareTo));

    private static final Comparator<UserOrganization> MEMBERSHIP_ORDER = Comparator
            .comparing(UserOrganization::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(membership -> membership.getOrganizacion().getNombre(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(membership -> membership.getOrganizacion().getId(),
                    Comparator.nullsLast(Long::compareTo));

    private final UserRepository users;
    private final UserOrganizationRepository memberships;
    private final OrganizationRepository organizations;

    public UserAuthenticationService(UserRepository users, UserOrganizationRepository memberships,
            OrganizationRepository organizations) {
        this.users = users;
        this.memberships = memberships;
        this.organizations = organizations;
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

        List<UserOrganization> activeMemberships = memberships.findByUsuarioIdAndEstado(user.getId(), "ACTIVO");
        Long organizacionId = global ? null : activeMemberships.stream()
                .sorted(MEMBERSHIP_ORDER)
                .map(membership -> membership.getOrganizacion().getId())
                .findFirst()
                .orElse(null);
        Map<Long, Set<String>> organizationPermissions = new HashMap<>();
        activeMemberships.forEach(membership -> {
            Set<String> permissions = new HashSet<>();
            membership.getRol().getPermisos().forEach(permission -> permissions.add(permission.getCodigo()));
            organizationPermissions.put(membership.getOrganizacion().getId(), Set.copyOf(permissions));
        });

        List<Organizacion> visible = global
                ? organizations.findAll()
                : activeMemberships.stream().map(UserOrganization::getOrganizacion).toList();
        List<OrganizationDtos.Summary> organizationList = new ArrayList<>();
        visible.stream().sorted(ORGANIZATION_ORDER)
                .forEach(organization -> organizationList.add(new OrganizationDtos.Summary(
                        organization.getId(), organization.getNombre(), organization.getUdeo())));

        return new CurrentUser(
                user.getId(), user.getEmail(), user.getUsername(), user.getNombre(), user.getApellido(),
                user.getRolGlobal() == null ? null : user.getRolGlobal().getNombre(),
                List.copyOf(organizationList), Set.copyOf(globalPermissions),
                Map.copyOf(organizationPermissions), global, organizacionId);
    }

    public Usuario entity(String identifier) {
        String value = identifier == null ? "" : identifier.trim().toLowerCase(Locale.ROOT);
        return users.findByEmailOrUsername(value, value)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }
}
