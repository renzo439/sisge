package com.sisge.ms.controller;

import com.sisge.ms.dto.reemplazante.ReemplazanteDtos;
import com.sisge.ms.service.ReemplazanteService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reemplazantes")
@PreAuthorize("hasAuthority('REEMPLAZANTES_VER')")
public class ReemplazanteController {
    private final ReemplazanteService service;

    public ReemplazanteController(ReemplazanteService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ReemplazanteDtos.View> list(@RequestParam(required = false) Long organizacionId,
            @RequestParam(required = false) String dni, @RequestParam(required = false) String cuil,
            @RequestParam(required = false) String reemplazante,
            @RequestParam(required = false) String titular, @RequestParam(required = false) String lugarTrabajo,
            @RequestParam(required = false) String hospital, @RequestParam(required = false) String udeo,
            @RequestParam(required = false) Long importacionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCarga,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(search(organizacionId, dni, cuil, reemplazante, titular, lugarTrabajo,
                hospital, udeo, importacionId, fechaCarga), page, size);
    }

    @GetMapping(value = "/reporte", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> report(@RequestParam(required = false) Long organizacionId,
            @RequestParam(required = false) String dni, @RequestParam(required = false) String cuil,
            @RequestParam(required = false) String reemplazante,
            @RequestParam(required = false) String titular, @RequestParam(required = false) String lugarTrabajo,
            @RequestParam(required = false) String hospital, @RequestParam(required = false) String udeo,
            @RequestParam(required = false) Long importacionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCarga) {
        byte[] pdf = service.pdf(search(organizacionId, dni, cuil, reemplazante, titular, lugarTrabajo,
                hospital, udeo, importacionId, fechaCarga));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"reporte-reemplazantes-" + LocalDate.now() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @GetMapping("/{id}")
    public ReemplazanteDtos.View get(@PathVariable Long id) {
        return service.get(id);
    }

    private ReemplazanteService.Search search(Long organizacionId, String dni, String cuil, String reemplazante,
            String titular, String lugarTrabajo, String hospital, String udeo, Long importacionId,
            LocalDate fechaCarga) {
        return new ReemplazanteService.Search(organizacionId, dni, cuil, reemplazante, titular, lugarTrabajo,
                hospital, udeo, importacionId, fechaCarga);
    }
}
