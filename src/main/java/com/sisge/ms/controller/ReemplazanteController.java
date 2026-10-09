package com.sisge.ms.controller;

import com.sisge.ms.dto.reemplazante.ReemplazanteDtos;
import com.sisge.ms.entity.ReemplazanteHistorico;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.ReemplazanteHistoricoRepository;
import com.sisge.ms.security.CurrentUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reemplazantes")
public class ReemplazanteController {
    private final ReemplazanteHistoricoRepository repository;

    public ReemplazanteController(ReemplazanteHistoricoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public Page<ReemplazanteDtos.View> list(@RequestParam(required = false) Long organizacionId,
            @RequestParam(required = false) String cuil, @RequestParam(required = false) String reemplazante,
            @RequestParam(required = false) String titular, @RequestParam(required = false) String lugarTrabajo,
            @RequestParam(required = false) Long importacionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaCarga,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        CurrentUser user = CurrentUser.get();
        if (!user.global() && organizacionId == null) throw ApiException.bad("organizacionId is required");
        if (organizacionId != null) user.requireForOrganization(organizacionId, "REEMPLAZANTES_VER");
        else user.requireGlobal("REEMPLAZANTES_VER");

        Specification<ReemplazanteHistorico> specification = (root, query, builder) -> builder.conjunction();
        if (organizacionId != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("organizacion").get("id"), organizacionId));
        if (cuil != null) specification = specification.and((root, query, builder) -> builder.equal(root.get("cuil"), cuil));
        if (reemplazante != null) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("reemplazante")), "%" + reemplazante.toLowerCase() + "%"));
        if (titular != null) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("titular")), "%" + titular.toLowerCase() + "%"));
        if (lugarTrabajo != null) specification = specification.and((root, query, builder) -> builder.equal(root.get("lugarTrabajo"), lugarTrabajo));
        if (importacionId != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("importacion").get("id"), importacionId));
        if (fechaCarga != null) {
            Instant from = fechaCarga.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = fechaCarga.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            specification = specification.and((root, query, builder) -> builder.between(root.get("fechaCierre"), from, to));
        }
        return repository.findAll(specification, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100)))
                .map(ReemplazanteDtos::from);
    }

    @GetMapping("/{id}")
    public ReemplazanteDtos.View get(@PathVariable Long id) {
        ReemplazanteHistorico historical = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Historical replacement not found"));
        CurrentUser.get().requireForOrganization(historical.getOrganizacion().getId(), "REEMPLAZANTES_VER");
        return ReemplazanteDtos.from(historical);
    }
}
