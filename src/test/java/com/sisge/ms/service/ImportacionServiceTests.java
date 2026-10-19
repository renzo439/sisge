package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sisge.ms.dto.importacion.ImportacionDtos;
import com.sisge.ms.entity.ImportacionReemplazante;
import com.sisge.ms.entity.Organizacion;
import com.sisge.ms.entity.Usuario;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.model.ImportStatus;
import com.sisge.ms.repository.ImportacionReemplazanteRepository;
import com.sisge.ms.repository.ImportacionRepository;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.repository.ReemplazanteHistoricoRepository;
import com.sisge.ms.security.CurrentUser;
import com.sisge.ms.security.UserAuthenticationService;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ImportacionServiceTests {
    @Mock
    private ImportacionRepository imports;
    @Mock
    private ImportacionReemplazanteRepository rows;
    @Mock
    private ReemplazanteHistoricoRepository history;
    @Mock
    private OrganizationRepository organizations;
    @Mock
    private UserAuthenticationService authentication;
    @Mock
    private ExcelImportParser parser;
    @Mock
    private MultipartFile file;
    @Mock
    private Usuario administrator;
    @Mock
    private JdbcTemplate jdbc;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void migratesValidReviewedWorkbookDirectlyToHistory() throws Exception {
        CurrentUser user = new CurrentUser(1L, "admin@example.com",
                Set.of("IMPORTACIONES_APROBAR", "IMPORTACIONES_CERRAR"), Map.of(), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        Organizacion organization = new Organizacion();
        organization.setNombre("Organización");
        ImportacionReemplazante row = new ImportacionReemplazante();
        row.setFilaOrigen(2);
        row.setErrores("");

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("historico.xlsx");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(parser.parse(any())).thenReturn(List.of(row));
        when(organizations.findById(7L)).thenReturn(Optional.of(organization));
        when(authentication.entity("admin@example.com")).thenReturn(administrator);

        ImportacionService service = new ImportacionService(imports, rows, history, organizations, authentication, parser, jdbc);

        ImportacionDtos.Detail result = service.migrateHistorical(7L, file);

        assertEquals(ImportStatus.CERRADA, result.estado());
        assertEquals(1, result.cantidadFilas());
        assertEquals(0, result.cantidadFilasConError());
        verify(imports).save(any());
        verify(rows).saveAll(List.of(row));
        verify(history).saveAll(any());
        verify(authentication, org.mockito.Mockito.times(2)).entity(eq("admin@example.com"));
    }

    @Test
    void createsOrganizationAndClosesImportationForConsolidatedHistoricalWorkbook() throws Exception {
        CurrentUser user = new CurrentUser(1L, "admin@example.com",
                Set.of("IMPORTACIONES_APROBAR", "IMPORTACIONES_CERRAR"), Map.of(), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        ImportacionReemplazante row = new ImportacionReemplazante();
        row.setFilaOrigen(2);
        row.setErrores("");
        ExcelImportParser.HistoricalRow historicalRow =
                new ExcelImportParser.HistoricalRow("Calilegua", "6118", row);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("historico-consolidado.xlsx");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(parser.parseHistorical(any())).thenReturn(List.of(historicalRow));
        when(organizations.findByUdeo("6118")).thenReturn(Optional.empty());
        when(organizations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authentication.entity("admin@example.com")).thenReturn(administrator);

        ImportacionService service = new ImportacionService(imports, rows, history, organizations, authentication, parser, jdbc);

        List<ImportacionDtos.Summary> results = service.migrateConsolidatedHistorical(file);

        assertEquals(1, results.size());
        assertEquals(ImportStatus.CERRADA, results.get(0).estado());
        verify(organizations).save(any());
        verify(jdbc, org.mockito.Mockito.times(2)).batchUpdate(any(String.class), any(List.class), eq(500), any());
    }

    @Test
    void reportsBlockingRowErrorsForConsolidatedHistoricalWorkbook() throws Exception {
        CurrentUser user = new CurrentUser(1L, "admin@example.com",
                Set.of("IMPORTACIONES_APROBAR", "IMPORTACIONES_CERRAR"), Map.of(), true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        ImportacionReemplazante row = new ImportacionReemplazante();
        row.setFilaOrigen(42);
        row.setErrores("periodo:INVALID_RANGE;cuil:INVALID");
        ExcelImportParser.HistoricalRow historicalRow =
                new ExcelImportParser.HistoricalRow("Calilegua", "6118", row);

        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("historico-consolidado.xlsx");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(parser.parseHistorical(any())).thenReturn(List.of(historicalRow));
        when(organizations.findByUdeo("6118")).thenReturn(Optional.empty());
        when(organizations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authentication.entity("admin@example.com")).thenReturn(administrator);

        ImportacionService service = new ImportacionService(imports, rows, history, organizations, authentication, parser, jdbc);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.migrateConsolidatedHistorical(file));

        assertEquals(List.of(Map.of("fila", 42, "errores", List.of("periodo:INVALID_RANGE", "cuil:INVALID"))),
                exception.details());
    }
}
