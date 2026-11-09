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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;

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
    @PersistenceContext private EntityManager entityManager;
    @Autowired private AuditoriaService audit;
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
        parsed.forEach(row -> { row.setImportacion(imported); resolveMasters(row, imported.getOrganizacion()); });
        int errors=(int)parsed.stream().filter(row -> row.getErrores()!=null&&!row.getErrores().isBlank()).count();
        imported.setCounts(parsed.size(), parsed.size()-errors, errors);
        imports.save(imported); rows.saveAll(parsed);
        audit(imported, "IMPORTACION");
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
        if (imported.getEstado() != PersonalImportStatus.PENDIENTE && imported.getEstado() != PersonalImportStatus.EN_RELEVAMIENTO) throw ApiException.conflict("Importation cannot be reviewed");
        ImportacionPersonalFila row=rows.findByIdAndImportacionId(rowId, importId).orElseThrow(() -> ApiException.notFound("Import row not found"));
        String oldValue=rowJson(row);
        Organizacion organization=organizations.findById(review.organizacionId()).orElseThrow(() -> ApiException.notFound("Organization not found"));
        if (!java.util.Objects.equals(organization.getId(), imported.getOrganizacion().getId())) throw ApiException.forbidden("Rows cannot be reassigned to another organization");
        Dependencia dependencia=review.dependenciaId()==null ? row.getDependencia() : required(Dependencia.class, review.dependenciaId(), "Dependency not found");
        if (dependencia != null && !java.util.Objects.equals(dependencia.getOrganizacion().getId(), organization.getId())) throw ApiException.bad("Dependency does not belong to organization");
        Sector sector=review.sectorId() == null ? row.getSector() : sectors.findByIdAndOrganizacionId(review.sectorId(), organization.getId()).orElseThrow(() -> ApiException.bad("Sector does not belong to organization"));
        if (sector != null && dependencia != null && sector.getDependencia()!=null && !java.util.Objects.equals(sector.getDependencia().getId(), dependencia.getId())) throw ApiException.bad("Sector does not belong to dependency");
        row.review(normalizeDni(review.dni()), normalizeCuil(review.cuil()), review.nombres().trim(), review.apellido().trim(), review.sexo(), review.fechaNacimiento(), organization, dependencia, sector,
                optional(Escalafon.class, review.escalafonId(), row.getEscalafon()), optional(Categoria.class, review.categoriaId(), row.getCategoria()),
                optional(SituacionRevista.class, review.situacionRevistaId(), row.getSituacionRevista()), optional(Agrupamiento.class, review.agrupamientoId(), row.getAgrupamiento()),
                optional(Funcion.class, review.funcionId(), row.getFuncion()), optional(TipoAfectacion.class, review.tipoAfectacionId(), row.getTipoAfectacion()),
                optional(Titulo.class, review.tituloId(), row.getTitulo()), review.fechaInicio(), review.antiguedadAnios(), review.afectado());
        row.setErrores(validation(row));
        auditReview(imported,row,oldValue);
        return detail(imported, rows.findByImportacionId(importId));
    }
    @Transactional
    public PersonalDtos.ImportDetail approve(Long id) {
        CurrentUser.get().requireGlobal("PERSONAL_APROBAR");
        ImportacionPersonal imported=imports.lockById(id).orElseThrow(() -> ApiException.notFound("Personnel importation not found"));
        if (imported.getEstado() != PersonalImportStatus.PENDIENTE && imported.getEstado() != PersonalImportStatus.EN_RELEVAMIENTO) throw ApiException.conflict("Importation cannot be approved");
        List<ImportacionPersonalFila> staged=rows.findByImportacionId(id);
        if (staged.isEmpty() || staged.stream().anyMatch(row -> !row.isRevisada() || !validation(row).isBlank() || (row.getErrores()!=null && !row.getErrores().isBlank()))) throw ApiException.conflict("Every row must be explicitly reviewed and valid before approval");
        imported.approve();
        audit(imported, "APROBACION");
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
    private void resolveMasters(ImportacionPersonalFila row, Organizacion organization) {
        row.setResolved(organization, null, null,
                master(Escalafon.class, row, "Escalafon", "ESCALAFON_NO_EXISTE"),
                master(Categoria.class, row, "Categoria", "CATEGORIA_NO_EXISTE"),
                master(SituacionRevista.class, row, "Situacion Revista", "SITUACION_REVISTA_NO_EXISTE"),
                master(Agrupamiento.class, row, "Agrupamiento", "AGRUPAMIENTO_NO_EXISTE"),
                master(Funcion.class, row, "Funcion", "FUNCION_NO_EXISTE"),
                master(TipoAfectacion.class, row, "Tipo Afectacion", "TIPO_AFECTACION_NO_EXISTE"),
                master(Titulo.class, row, "Titulo", "TITULO_NO_EXISTE"));
    }
    private <T extends Maestro> T master(Class<T> type, ImportacionPersonalFila row, String rawKey, String error) {
        String value=raw(row.getValoresCrudos(),rawKey); if(value.isBlank()) return null;
        if (entityManager == null) return null;
        List<T> matches=entityManager.createQuery("select x from "+type.getSimpleName()+" x where lower(x.nombre)=:name and x.activo=true",type).setParameter("name",PersonalExcelImportParser.normalize(value)).getResultList();
        if(matches.size()==1) return matches.get(0);
        row.setErrores(append(row.getErrores(), matches.isEmpty()?error:error+"_AMBIGUO")); return null;
    }
    private String raw(String json,String key){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\""+java.util.regex.Pattern.quote(key)+"\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);return m.find()?m.group(1).replace("\\\"","\"").replace("\\\\","\\\\"):"";}
    private String append(String previous,String error){return previous==null||previous.isBlank()?error:previous+";"+error;}
    private void audit(ImportacionPersonal imported,String action){if(audit!=null)audit.record(imported.getOrganizacion(),action,"IMPORTACION_PERSONAL",imported.getId());}
    private void auditReview(ImportacionPersonal imported,ImportacionPersonalFila row,String oldValue){if(audit!=null)audit.recordChange(imported.getOrganizacion(),"RELEVAMIENTO","IMPORTACION_PERSONAL_FILA",row.getId(),oldValue,rowJson(row));}
    private <T> T required(Class<T> type,Long id,String message){if(entityManager==null)throw ApiException.bad(message);T value=entityManager.find(type,id);if(value==null)throw ApiException.bad(message);return value;}
    private <T extends Maestro> T optional(Class<T> type,Long id,T current){if(id==null)return current;T value=required(type,id,type.getSimpleName()+" not found");if(!value.isActivo())throw ApiException.bad(type.getSimpleName()+" is inactive");return value;}
    private String rowJson(ImportacionPersonalFila row){return "{\"dni\":\""+json(row.getDni())+"\",\"cuil\":\""+json(row.getCuil())+"\",\"organizacionId\":"+id(row.getOrganizacion())+",\"dependenciaId\":"+id(row.getDependencia())+",\"sectorId\":"+id(row.getSector())+",\"escalafonId\":"+id(row.getEscalafon())+",\"categoriaId\":"+id(row.getCategoria())+",\"situacionRevistaId\":"+id(row.getSituacionRevista())+",\"agrupamientoId\":"+id(row.getAgrupamiento())+",\"funcionId\":"+id(row.getFuncion())+",\"tipoAfectacionId\":"+id(row.getTipoAfectacion())+",\"tituloId\":"+id(row.getTitulo())+"}";}
    private String json(String value){return value==null?"":value.replace("\\","\\\\").replace("\"","\\\"");} private Object id(Object value){if(value==null)return "null";try{return value.getClass().getMethod("getId").invoke(value);}catch(Exception ignored){return "null";}}
    private PersonalDtos.ImportDetail detail(ImportacionPersonal imported, List<ImportacionPersonalFila> staged) { int errors=(int)staged.stream().filter(r->r.getErrores()!=null&&!r.getErrores().isBlank()).count(); return new PersonalDtos.ImportDetail(imported.getId(), imported.getOrganizacion().getId(), imported.getArchivoOrigen(), imported.getEstado().name(), staged.size(), staged.size()-errors, errors, staged.stream().map(row -> new PersonalDtos.Row(row.getId(), row.getFilaOrigen(), row.getValoresCrudos(), row.getDni(), row.getCuil(), row.getNombres(), row.getApellido(), row.getSexo(), row.getFechaNacimiento(), row.getOrganizacion()==null?null:row.getOrganizacion().getId(), row.getDependencia()==null?null:row.getDependencia().getId(), row.getSector()==null?null:row.getSector().getId(), row.getEscalafon()==null?null:row.getEscalafon().getId(), row.getCategoria()==null?null:row.getCategoria().getId(), row.getSituacionRevista()==null?null:row.getSituacionRevista().getId(), row.getAgrupamiento()==null?null:row.getAgrupamiento().getId(), row.getFuncion()==null?null:row.getFuncion().getId(), row.getTipoAfectacion()==null?null:row.getTipoAfectacion().getId(), row.getTitulo()==null?null:row.getTitulo().getId(), row.getAdicionalMh(), row.getAdicionalJefatura(), row.getFechaInicio(), row.getAntiguedadAnios(), row.getAfectado(), row.isRevisada(), row.getErrores())).toList()); }
    private PersonalDtos.Person person(Designacion value) { Persona p=value.getPersona(); return new PersonalDtos.Person(p.getId(), p.getDni(), p.getCuil(), p.getNombres(), p.getApellido(), p.getSexo(), p.getFechaNacimiento(), value.getOrganizacion().getId(), value.getOrganizacion().getNombre(), value.getSector()==null?null:value.getSector().getId(), value.getSector()==null?null:value.getSector().getNombre(), name(value.getEscalafon()), name(value.getCategoria()), name(value.getSituacionRevista()), name(value.getAgrupamiento()), name(value.getFuncion()), value.getFechaInicio(), value.getAntiguedadAnios(), value.isAfectado()); }
    private String name(Maestro master){return master==null?null:master.getNombre();}
}
