package com.sisge.ms.controller;

import com.sisge.ms.dto.organizacion.OrganizationDtos;
import com.sisge.ms.service.OrganizationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizaciones")
public class OrganizationController {
    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrganizationDtos.Summary> list() {
        return service.listForPersonnelImport();
    }
}
