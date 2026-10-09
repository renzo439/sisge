package com.sisge.ms.controller;

import com.sisge.ms.dto.importacion.ImportacionDtos;
import com.sisge.ms.model.ImportStatus;
import com.sisge.ms.service.ImportacionService;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/importaciones")
public class AdminImportacionController {
    private final ImportacionService service;
    public AdminImportacionController(ImportacionService service) { this.service = service; }
    @GetMapping public Page<ImportacionDtos.Summary> queue(@RequestParam(required = false) Long organizacionId, @RequestParam(required = false) ImportStatus estado, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta, @RequestParam(required = false) String usuario, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.adminList(organizacionId, estado, desde, hasta, usuario, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100))); }
    @PostMapping("/{id}/observar") public ImportacionDtos.Detail observe(@PathVariable Long id, @RequestBody @jakarta.validation.Valid ImportacionDtos.Reason reason) { return service.observe(id, reason.motivo()); }
    @PostMapping("/{id}/rechazar") public ImportacionDtos.Detail reject(@PathVariable Long id, @RequestBody @jakarta.validation.Valid ImportacionDtos.Reason reason) { return service.reject(id, reason.motivo()); }
    @PostMapping({"/{id}/aprobar", "/{id}/cerrar"}) public ImportacionDtos.Detail close(@PathVariable Long id) { return service.approveAndClose(id); }
}
