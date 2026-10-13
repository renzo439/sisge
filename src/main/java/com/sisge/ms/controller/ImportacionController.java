package com.sisge.ms.controller;

import com.sisge.ms.dto.importacion.ImportacionDtos;
import com.sisge.ms.service.ImportacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class ImportacionController {
    private final ImportacionService service;
    public ImportacionController(ImportacionService service) { this.service = service; }
    @PostMapping(value = "/organizaciones/{organizacionId}/importaciones/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacionDtos.Detail upload(@PathVariable Long organizacionId, @RequestParam("file") MultipartFile file) { return service.upload(organizacionId, file); }
    @GetMapping("/importaciones/{id}") public ImportacionDtos.Detail get(@PathVariable Long id) { return service.get(id); }
    @GetMapping("/importaciones") public Page<ImportacionDtos.Summary> list(@RequestParam(required = false) Long organizacionId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return service.list(organizacionId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by("id").descending())); }
    @PutMapping("/importaciones/{id}/reemplazantes/{registroId}") public ImportacionDtos.Detail edit(@PathVariable Long id, @PathVariable Long registroId, @RequestBody @jakarta.validation.Valid ImportacionDtos.RowEdit dto) { return service.edit(id, registroId, dto); }
    @PostMapping("/importaciones/{id}/enviar-revision") public ImportacionDtos.Detail submit(@PathVariable Long id) { return service.submit(id); }
}
