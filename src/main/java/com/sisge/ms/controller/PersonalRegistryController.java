package com.sisge.ms.controller;

import com.sisge.ms.dto.personal.PersonalDtos;
import com.sisge.ms.service.PersonalRegistryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class PersonalRegistryController {
    private final PersonalRegistryService service;
    public PersonalRegistryController(PersonalRegistryService service) { this.service=service; }
    @PostMapping(value="/organizaciones/{organizacionId}/personal/importaciones", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED) public PersonalDtos.ImportDetail upload(@PathVariable Long organizacionId, @RequestParam("file") MultipartFile file) { return service.upload(organizacionId, file); }
    @GetMapping("/personal/importaciones") public Page<PersonalDtos.ImportSummary> listImports(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return service.listImports(PageRequest.of(Math.max(0,page), Math.min(Math.max(1,size),100), Sort.by("id").descending())); }
    @GetMapping("/personal/importaciones/{id}") public PersonalDtos.ImportDetail get(@PathVariable Long id) { return service.get(id); }
    @PutMapping("/personal/importaciones/{importId}/filas/{rowId}") public PersonalDtos.ImportDetail review(@PathVariable Long importId, @PathVariable Long rowId, @Valid @RequestBody PersonalDtos.RowReview body) { return service.review(importId, rowId, body); }
    @PostMapping("/personal/importaciones/{id}/aprobar") public PersonalDtos.ImportDetail approve(@PathVariable Long id) { return service.approve(id); }
    @GetMapping("/personas") public Page<PersonalDtos.Person> list(@RequestParam(required=false) Long organizacionId, @RequestParam(required=false) String q, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return service.list(organizacionId, q, PageRequest.of(Math.max(0,page), Math.min(Math.max(1,size),100), Sort.by("persona.apellido").ascending())); }
}
