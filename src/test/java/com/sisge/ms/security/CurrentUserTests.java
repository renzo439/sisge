package com.sisge.ms.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sisge.ms.exception.ApiException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CurrentUserTests {
    @Test
    void organizationPermissionCannotComeFromAnotherOrganization() {
        CurrentUser user = new CurrentUser(1L, "operator@example.com", Set.of(),
                Map.of(10L, Set.of("REEMPLAZANTES_VER")), false);
        assertDoesNotThrow(() -> user.requireForOrganization(10L, "REEMPLAZANTES_VER"));
        assertThrows(ApiException.class, () -> user.requireForOrganization(20L, "REEMPLAZANTES_VER"));
    }

    @Test
    void globalRolesUseOnlyGlobalPermissions() {
        CurrentUser user = new CurrentUser(1L, "admin@example.com", Set.of("IMPORTACIONES_VER_TODAS"), Map.of(), true);
        assertDoesNotThrow(() -> user.requireForOrganization(20L, "IMPORTACIONES_VER_TODAS"));
    }

    @Test
    void permissionsCombineGlobalAndOrganizationPermissions() {
        CurrentUser user = new CurrentUser(1L, "user@example.com", Set.of("PERSONAL_VER"),
                Map.of(10L, Set.of("REEMPLAZANTES_VER"), 20L, Set.of("IMPORTACIONES_VER_PROPIAS")), false);
        assertEquals(Set.of("PERSONAL_VER", "REEMPLAZANTES_VER", "IMPORTACIONES_VER_PROPIAS"),
                user.permissions());
    }
}
