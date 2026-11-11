package com.sisge.ms.controller;

import com.sisge.ms.dto.permission.PermissionDtos;
import com.sisge.ms.service.PermissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/permisos")
public class PermissionController {
    private final PermissionService service;

    public PermissionController(PermissionService service) {
        this.service = service;
    }

    @GetMapping
    public List<PermissionDtos.Summary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public PermissionDtos.Summary get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionDtos.Summary create(@Valid @RequestBody PermissionDtos.Create request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PermissionDtos.Summary update(@PathVariable Long id, @Valid @RequestBody PermissionDtos.Update request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
