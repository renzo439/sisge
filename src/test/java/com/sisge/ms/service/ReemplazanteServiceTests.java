package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sisge.ms.entity.ReemplazanteHistorico;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.ReemplazanteHistoricoRepository;
import com.sisge.ms.security.CurrentUser;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class ReemplazanteServiceTests {
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void generatesPdfWithReplacementDatesAndHospital() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(1L, "admin@example.com", Set.of("REEMPLAZANTES_VER"), Map.of(), true),
                null, List.of()));
        ReemplazanteHistoricoRepository repository = mock(ReemplazanteHistoricoRepository.class);
        ReemplazanteHistorico row = mock(ReemplazanteHistorico.class);
        when(row.getReemplazante()).thenReturn("Ana Perez");
        when(row.getPeriodoDesde()).thenReturn(LocalDate.of(2026, 1, 5));
        when(row.getPeriodoHasta()).thenReturn(LocalDate.of(2026, 2, 4));
        when(row.getHospitalNombre()).thenReturn("Hospital Norte");
        when(row.getUdeo()).thenReturn("HN");
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row)));
        var service = new ReemplazanteService(repository);

        byte[] pdf = service.pdf(new ReemplazanteService.Search(2L, null, null, "Ana", null, null,
                null, null, null, null));

        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
        assertTrue(pdf.length > 500);
    }

    @Test
    void requiresOrganizationForNonGlobalUsers() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(3L, "operador@example.com", Set.of(), Map.of(3L, Set.of("REEMPLAZANTES_VER")), false),
                null, List.of()));
        var service = new ReemplazanteService(mock(ReemplazanteHistoricoRepository.class));

        ApiException error = assertThrows(ApiException.class, () -> service.pdf(
                new ReemplazanteService.Search(null, null, null, null, null, null, null, null, null, null)));

        assertEquals(400, error.status().value());
    }
}
