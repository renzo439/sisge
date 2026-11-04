package com.sisge.ms.service;

import com.sisge.ms.dto.personal.PersonalDtos;
import com.sisge.ms.entity.*;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.model.PersonalImportStatus;
import com.sisge.ms.repository.*;
import com.sisge.ms.security.CurrentUser;
import com.sisge.ms.security.UserAuthenticationService;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PersonalRegistryService {
    private final ImportacionPersonalRepository imports;
    private final ImportacionPersonalFilaRepository rows;
    private final PersonaRepository people;
    private final DesignacionRepository assignments;
    private final OrganizationRepository organizations;
    private final SectorRepository sectors;
    private final UserAuthenticationService authentication;
    private final PersonalExcelImportParser parser;
    public PersonalRegistryService(ImportacionPersonalRepository imports, ImportacionPersonalFilaRepository rows, PersonaRepository people,
            DesignacionRepository assignments, OrganizationRepository organizations, SectorRepository sectors,
            UserAuthenticationService authentication, PersonalExcelImportParser parser) {
        this.imports=imports; this.rows=rows; this.people=people; this.assignments=assignments; this.organizations=organizations;
        this.sectors=sectors; this.authentication=authentication; this.parser=parser;
    }
    @Transactional
    public PersonalDtos.ImportDetail upload(Long organizationId, MultipartFile file) {
        CurrentUser user=CurrentUser.get(); user.requireForOrganization(organizationId, "PERSONAL_IMPORTAR");
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) throw ApiException.bad("Only non-empty .xlsx files are accepted");
        List<ImportacionPersonalFila> parsed;
        try { parsed=parser.parse(file.getInputStream()); } catch (IOException exception) { throw ApiException.bad("Cannot read upload"); }
        ImportacionPersonal imported=new ImportacionPersonal();
        imported.setOrganizacion(organizations.findById(organizationId).orElseThrow(() -> ApiException.notFound("Organization not found")));
        imported.setCreadoPor(authentication.entity(user.email())); imported.setArchivoOrigen(file.getOriginalFilename());
        parsed.forEach(row -> row.setImportacion(imported));
        imports.save(imported); rows.saveAll(parsed);
        return detail(imported, parsed);
    }
    @Transactional(readOnly = true)
    public PersonalDtos.ImportDetail get(Long id) {
        ImportacionPersonal imported=find(id); access(imported, "PERSONAL_REVISAR");
        return detail(imported, rows.findByImportacionId(id));
    }
    @Transactional
    public PersonalDtos.ImportDetail review(Long importId, Long rowId, PersonalDtos.RowReview review) {
        ImportacionPersonal imported=find(importId); access(imported, "PERSONAL_REVISAR");
        if (imported.getEstado() != PersonalImportStatus.EN_REVISION) throw ApiException.conflict("Importation is already approved");
        ImportacionPersonalFila row=rows.findByIdAndImportacionId(rowId, importId).orElseThrow(() -> ApiException.notFound("Import row not found"));
        Organizacion organization=organizations.findById(review.organizacionId()).orElseThrow(() -> ApiException.notFound("Organization not found"));
        Sector sector=review.sectorId() == null ? null : sectors.findByIdAndOrganizacionId(review.sectorId(), organization.getId()).orElseThrow(() -> ApiException.bad("Sector does not belong to organization"));
        row.review(normalizeDni(review.dni()), normalizeCuil(review.cuil()), review.nombres().trim(), review.apellido().trim(), review.sexo(), review.fechaNacimiento(), organization, sector, review.escalafon(), review.categoria(), review.situacionRevista(), review.agrupamiento(), review.funcion(), review.fechaInicio(), review.antiguedadAnios(), review.afectado());
        row.setErrores(validation(row));
        return detail(imported, rows.findByImportacionId(importId));
    }
    @Transactional
    public PersonalDtos.ImportDetail approve(Long id) {
        CurrentUser.get().requireGlobal("PERSONAL_APROBAR");
        ImportacionPersonal imported=imports.lockById(id).orElseThrow(() -> ApiException.notFound("Personnel importation not found"));
        if (imported.getEstado() != PersonalImportStatus.EN_REVISION) throw ApiException.conflict("Importation is already approved");
        List<ImportacionPersonalFila> staged=rows.findByImportacionId(id);
        if (staged.isEmpty() || staged.stream().anyMatch(row -> !row.isRevisada() || !validation(row).isBlank())) throw ApiException.conflict("Every row must be explicitly reviewed and valid before approval");
        for (ImportacionPersonalFila row : staged) {
            Persona person=findPerson(row);
            person.setDni(row.getDni()); person.setCuil(blankToNull(row.getCuil())); person.setNombres(row.getNombres()); person.setApellido(row.getApellido()); person.setSexo(row.getSexo()); person.setFechaNacimiento(row.getFechaNacimiento());
            people.save(person);
            Designacion assignment=new Designacion();
            assignment.setPersona(person); assignment.setOrganizacion(row.getOrganizacion()); assignment.setSector(row.getSector());
            assignment.setEscalafon(row.getEscalafon()); assignment.setCategoria(row.getCategoria()); assignment.setSituacionRevista(row.getSituacionRevista());
            assignment.setAgrupamiento(row.getAgrupamiento()); assignment.setFuncion(row.getFuncion()); assignment.setFechaInicio(row.getFechaInicio());
            assignment.setAntiguedadAnios(row.getAntiguedadAnios()); assignment.setAfectado(Boolean.TRUE.equals(row.getAfectado())); assignments.save(assignment);
        }
        imported.approve();
        return detail(imported, staged);
    }
    @Transactional(readOnly = true)
    public Page<PersonalDtos.Person> list(Long organizationId, String query, Pageable pageable) {
        CurrentUser user=CurrentUser.get();
        if (user.global()) user.requireGlobal("PERSONAL_VER"); else { if (organizationId == null) throw ApiException.bad("organizacionId is required"); user.requireForOrganization(organizationId, "PERSONAL_VER"); }
        Specification<Designacion> spec=(root, q, cb) -> cb.isTrue(root.get("vigente"));
        if (organizationId != null) spec=spec.and((root, q, cb) -> cb.equal(root.get("organizacion").get("id"), organizationId));
        if (query != null && !query.isBlank()) { String like="%" + query.trim().toLowerCase() + "%"; spec=spec.and((root, q, cb) -> cb.or(cb.like(cb.lower(root.get("persona").get("dni")), like), cb.like(cb.lower(root.get("persona").get("cuil")), like), cb.like(cb.lower(root.get("persona").get("nombres")), like), cb.like(cb.lower(root.get("persona").get("apellido")), like))); }
        return assignments.findAll(spec, pageable).map(this::person);
    }
    private Persona findPerson(ImportacionPersonalFila row) {
        String cuil=blankToNull(row.getCuil());
        return cuil != null ? people.findByCuil(cuil).orElseGet(Persona::new) : people.findByDni(row.getDni()).orElseGet(Persona::new);
    }
    private ImportacionPersonal find(Long id) { return imports.findById(id).orElseThrow(() -> ApiException.notFound("Personnel importation not found")); }
    private void access(ImportacionPersonal imported, String permission) { CurrentUser user=CurrentUser.get(); if (user.global()) user.requireGlobal(permission); else user.requireForOrganization(imported.getOrganizacion().getId(), permission); }
    private String validation(ImportacionPersonalFila row) { if (!row.isRevisada()) return "revision:REQUIRED"; if (row.getDni()==null || row.getDni().isBlank()) return "dni:REQUIRED"; if (row.getNombres()==null || row.getNombres().isBlank() || row.getApellido()==null || row.getApellido().isBlank()) return "nombre:REQUIRED"; if (row.getOrganizacion()==null || row.getFechaInicio()==null || row.getAfectado()==null) return "designacion:REQUIRED"; return ""; }
    private String normalizeDni(String value) { String dni=value.replaceAll("\\D", ""); if (dni.isBlank()) throw ApiException.bad("DNI is invalid"); return dni; }
    private String normalizeCuil(String value) { String cuil=value == null ? "" : value.replaceAll("\\D", ""); if (!cuil.isEmpty() && !cuil.matches("\\d{11}")) throw ApiException.bad("CUIL is invalid"); return cuil; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private PersonalDtos.ImportDetail detail(ImportacionPersonal imported, List<ImportacionPersonalFila> staged) { return new PersonalDtos.ImportDetail(imported.getId(), imported.getOrganizacion().getId(), imported.getArchivoOrigen(), imported.getEstado().name(), staged.stream().map(row -> new PersonalDtos.Row(row.getId(), row.getFilaOrigen(), row.getValoresCrudos(), row.getDni(), row.getCuil(), row.getNombres(), row.getApellido(), row.getSexo(), row.getFechaNacimiento(), row.getOrganizacion()==null?null:row.getOrganizacion().getId(), row.getSector()==null?null:row.getSector().getId(), row.getEscalafon(), row.getCategoria(), row.getSituacionRevista(), row.getAgrupamiento(), row.getFuncion(), row.getFechaInicio(), row.getAntiguedadAnios(), row.getAfectado(), row.isRevisada(), row.getErrores())).toList()); }
    private PersonalDtos.Person person(Designacion value) { Persona p=value.getPersona(); return new PersonalDtos.Person(p.getId(), p.getDni(), p.getCuil(), p.getNombres(), p.getApellido(), p.getSexo(), p.getFechaNacimiento(), value.getOrganizacion().getId(), value.getOrganizacion().getNombre(), value.getSector()==null?null:value.getSector().getId(), value.getSector()==null?null:value.getSector().getNombre(), value.getEscalafon(), value.getCategoria(), value.getSituacionRevista(), value.getAgrupamiento(), value.getFuncion(), value.getFechaInicio(), value.getAntiguedadAnios(), value.isAfectado()); }
}
