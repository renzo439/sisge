package com.sisge.ms.controller;

import com.sisge.ms.dto.organizacion.OrganizationDtos;
import com.sisge.ms.service.AdminOrganizationService;
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
@RequestMapping("/api/v1/admin/organizaciones")
public class AdminOrganizationController {
    private final AdminOrganizationService service;

    public AdminOrganizationController(AdminOrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrganizationDtos.Detail> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public OrganizationDtos.Detail get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationDtos.Detail create(@Valid @RequestBody OrganizationDtos.Create request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public OrganizationDtos.Detail update(@PathVariable Long id, @Valid @RequestBody OrganizationDtos.Update request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
