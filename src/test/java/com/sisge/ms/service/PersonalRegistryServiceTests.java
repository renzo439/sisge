package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.sisge.ms.dto.personal.PersonalDtos;
import com.sisge.ms.entity.*;
import com.sisge.ms.model.PersonalImportStatus;
import com.sisge.ms.repository.*;
import com.sisge.ms.security.CurrentUser;
import com.sisge.ms.security.UserAuthenticationService;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PersonalRegistryServiceTests {
    @Mock ImportacionPersonalRepository imports;
    @Mock ImportacionPersonalFilaRepository rows;
    @Mock PersonaRepository people;
    @Mock DesignacionRepository assignments;
    @Mock OrganizationRepository organizations;
    @Mock SectorRepository sectors;
    @Mock UserAuthenticationService authentication;
    @Mock PersonalExcelImportParser parser;
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }
    @Test
    void approvalLeavesOfficialRecordsForControlledConsolidation() {
        var user = new CurrentUser(1L, "admin@example.com", Set.of("PERSONAL_APROBAR"), Map.of(), true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var organization = new Organizacion(); organization.setNombre("Hospital");
        var imported = new ImportacionPersonal(); imported.setOrganizacion(organization); imported.setArchivoOrigen("personal.xlsx");
        var row = new ImportacionPersonalFila(); row.setFilaOrigen(2); row.setValoresCrudos("{}");
        row.review("12345678", "20123456786", "Ana", "Pérez", "F", LocalDate.of(1990, 1, 1), organization, null, "Profesional", "A", "Planta", "Salud", "Médica", LocalDate.of(2026, 1, 1), 5, true);
        when(imports.lockById(9L)).thenReturn(Optional.of(imported));
        when(rows.findByImportacionId(9L)).thenReturn(List.of(row));
        var service = new PersonalRegistryService(imports, rows, people, assignments, organizations, sectors, authentication, parser);
        PersonalDtos.ImportDetail result = service.approve(9L);
        assertEquals(PersonalImportStatus.APROBADO.name(), result.estado());
        verifyNoInteractions(people, assignments);
    }
    @Test
    void listsImportsWithOrganizationNameForGlobalReviewers() {
        var user = new CurrentUser(1L, "admin@example.com", Set.of("PERSONAL_REVISAR"), Map.of(), true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var organization = new Organizacion();
        organization.setNombre("Hospital Norte");
        var imported = new ImportacionPersonal();
        imported.setOrganizacion(organization);
        imported.setArchivoOrigen("censo.xlsx");
        imported.setCounts(10, 8, 2);
        var pageable = PageRequest.of(0, 20);
        when(imports.findAll(pageable)).thenReturn(new PageImpl<>(List.of(imported), pageable, 1));
        var service = new PersonalRegistryService(imports, rows, people, assignments, organizations, sectors, authentication, parser);

        var result = service.listImports(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Hospital Norte", result.getContent().get(0).organizacionNombre());
    }
}
