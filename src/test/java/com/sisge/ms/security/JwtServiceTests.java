package com.sisge.ms.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTests {
    @Test
    void includesGlobalAndOrganizationPermissionsInToken() {
        JwtService jwt = new JwtService("unit-test-jwt-secret-must-be-at-least-32-characters", 3600);
        CurrentUser user = new CurrentUser(
                1L,
                "admin@example.com",
                Set.of("PERSONAL_APROBAR", "PERSONAL_VER"),
                Map.of(10L, Set.of("PERSONAL_IMPORTAR", "PERSONAL_REVISAR")),
                true);

        String token = jwt.create(user);
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertEquals("admin@example.com", jwt.subject(token));
        assertTrue(payload.contains("\"permissions\":[\"PERSONAL_APROBAR\",\"PERSONAL_IMPORTAR\",\"PERSONAL_REVISAR\",\"PERSONAL_VER\"]"));
    }
}
