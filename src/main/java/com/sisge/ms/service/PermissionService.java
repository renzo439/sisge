package com.sisge.ms.service;

import com.sisge.ms.dto.permission.PermissionDtos;
import com.sisge.ms.entity.Permission;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.PermissionRepository;
import com.sisge.ms.repository.RoleRepository;
import com.sisge.ms.security.CurrentUser;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissionService {
    private static final String PERMISOS_GESTIONAR = "PERMISOS_GESTIONAR";
    private static final String ROLES_GESTIONAR = "ROLES_GESTIONAR";

    private final PermissionRepository permissions;
    private final RoleRepository roles;

    public PermissionService(PermissionRepository permissions, RoleRepository roles) {
        this.permissions = permissions;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<PermissionDtos.Summary> list() {
        requireView();
        return permissions.findAllByOrderByCodigoAsc().stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public PermissionDtos.Summary get(Long id) {
        requireView();
        return summary(findPermission(id));
    }

    @Transactional
    public PermissionDtos.Summary create(PermissionDtos.Create request) {
        CurrentUser.get().requireGlobal(PERMISOS_GESTIONAR);
        String codigo = normalizeCode(request.codigo());
        if (permissions.existsByCodigo(codigo)) {
            throw ApiException.conflict("A permission with that code already exists");
        }
        Permission permission = new Permission();
        permission.setCodigo(codigo);
        permission.setDescripcion(request.descripcion());
        return summary(permissions.save(permission));
    }

    @Transactional
    public PermissionDtos.Summary update(Long id, PermissionDtos.Update request) {
        CurrentUser.get().requireGlobal(PERMISOS_GESTIONAR);
        Permission permission = findPermission(id);
        String codigo = normalizeCode(request.codigo());
        if (!codigo.equals(permission.getCodigo()) && permissions.existsByCodigo(codigo)) {
            throw ApiException.conflict("A permission with that code already exists");
        }
        permission.setCodigo(codigo);
        permission.setDescripcion(request.descripcion());
        return summary(permissions.save(permission));
    }

    @Transactional
    public void delete(Long id) {
        CurrentUser.get().requireGlobal(PERMISOS_GESTIONAR);
        Permission permission = findPermission(id);
        if (roles.existsByPermisosId(permission.getId())) {
            throw ApiException.conflict("The permission is assigned to roles and cannot be deleted");
        }
        permissions.delete(permission);
    }

    private void requireView() {
        CurrentUser user = CurrentUser.get();
        boolean allowed = user.global() && (user.globalPermissions().contains(PERMISOS_GESTIONAR)
                || user.globalPermissions().contains(ROLES_GESTIONAR));
        if (!allowed) {
            throw ApiException.forbidden("Missing global permission: " + PERMISOS_GESTIONAR);
        }
    }

    private Permission findPermission(Long id) {
        return permissions.findById(id)
                .orElseThrow(() -> ApiException.notFound("Permission not found"));
    }

    private String normalizeCode(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private PermissionDtos.Summary summary(Permission permission) {
        return new PermissionDtos.Summary(permission.getId(), permission.getCodigo(), permission.getDescripcion());
    }
}
