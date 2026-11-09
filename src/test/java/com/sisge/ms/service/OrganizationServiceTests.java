package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.sisge.ms.entity.Organizacion;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.security.CurrentUser;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTests {
    @Mock
    private OrganizationRepository organizations;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsOnlyOrganizationsWhereTheUserCanImportPersonnel() {
        var user = new CurrentUser(1L, "operator@example.com", Set.of(),
                Map.of(2L, Set.of("PERSONAL_IMPORTAR"), 3L, Set.of("PERSONAL_VER")), false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var allowed = new Organizacion();
        allowed.setNombre("Hospital Norte");
        when(organizations.findAllById(List.of(2L))).thenReturn(List.of(allowed));

        var result = new OrganizationService(organizations).listForPersonnelImport();

        assertEquals(1, result.size());
        assertEquals("Hospital Norte", result.get(0).nombre());
    }
}
