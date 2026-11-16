package com.sisge.ms.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthControllerTests {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsCurrentUserPermissions() {
        CurrentUser user = new CurrentUser(7L, "user@example.com", Set.of("REEMPLAZANTES_VER"),
                Map.of(3L, Set.of("REEMPLAZANTES_VER")), false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        AuthController.Me me = new AuthController(null, null, null).me();

        assertEquals(7L, me.id());
        assertEquals("user@example.com", me.email());
        assertFalse(me.global());
        assertEquals(Set.of("REEMPLAZANTES_VER"), me.globalPermissions());
        assertEquals(Map.of(3L, Set.of("REEMPLAZANTES_VER")), me.organizationPermissions());
    }
}
