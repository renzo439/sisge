package com.sisge.ms.service;

import com.sisge.ms.dto.importacion.ImportacionDtos;
import com.sisge.ms.entity.Importacion;
import com.sisge.ms.entity.ImportacionReemplazante;
import com.sisge.ms.entity.Organizacion;
import com.sisge.ms.entity.ReemplazanteHistorico;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.model.ImportStatus;
import com.sisge.ms.repository.ImportacionReemplazanteRepository;
import com.sisge.ms.repository.ImportacionRepository;
import com.sisge.ms.repository.OrganizationRepository;
import com.sisge.ms.repository.ReemplazanteHistoricoRepository;
import com.sisge.ms.security.CurrentUser;
import com.sisge.ms.security.UserAuthenticationService;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportacionService {
    private final ImportacionRepository imports;
    private final ImportacionReemplazanteRepository rows;
    private final ReemplazanteHistoricoRepository history;
    private final OrganizationRepository organizations;
    private final UserAuthenticationService authentication;
    private final ExcelImportParser parser;

    public ImportacionService(ImportacionRepository imports, ImportacionReemplazanteRepository rows,
            ReemplazanteHistoricoRepository history, OrganizationRepository organizations,
            UserAuthenticationService authentication, ExcelImportParser parser) {
        this.imports = imports;
        this.rows = rows;
        this.history = history;
        this.organizations = organizations;
        this.authentication = authentication;
        this.parser = parser;
    }

    @Transactional
    public ImportacionDtos.Detail upload(Long organizationId, MultipartFile file) {
        CurrentUser user = CurrentUser.get();
        user.requireForOrganization(organizationId, "REEMPLAZANTES_IMPORTAR");
        List<ImportacionReemplazante> parsed = parseUpload(file);
        Importacion importacion = new Importacion();
        importacion.setOrganizacion(organizations.findById(organizationId)
                .orElseThrow(() -> ApiException.notFound("Organization not found")));
        importacion.setCreadoPor(authentication.entity(user.email()));
        importacion.setArchivoOrigen(file.getOriginalFilename());
        importacion.setCantidadFilas(parsed.size());
        parsed.forEach(row -> row.setImportacion(importacion));
        imports.save(importacion);
        rows.saveAll(parsed);
        recount(importacion, parsed);
        return detail(importacion, parsed);
    }

    @Transactional
    public ImportacionDtos.Detail migrateHistorical(Long organizationId, MultipartFile file) {
        CurrentUser user = CurrentUser.get();
        user.requireGlobal("IMPORTACIONES_APROBAR");
        user.requireGlobal("IMPORTACIONES_CERRAR");
        List<ImportacionReemplazante> parsed = parseUpload(file);
        return closeHistorical(organizations.findById(organizationId)
                .orElseThrow(() -> ApiException.notFound("Organization not found")), user, file.getOriginalFilename(), parsed);
    }

    @Transactional
    public List<ImportacionDtos.Detail> migrateConsolidatedHistorical(MultipartFile file) {
        CurrentUser user = CurrentUser.get();
        user.requireGlobal("IMPORTACIONES_APROBAR");
        user.requireGlobal("IMPORTACIONES_CERRAR");
        List<ExcelImportParser.HistoricalRow> parsed = parseHistoricalUpload(file);
        Map<String, List<ExcelImportParser.HistoricalRow>> byUdeo = parsed.stream()
                .collect(java.util.stream.Collectors.groupingBy(ExcelImportParser.HistoricalRow::udeo,
                        LinkedHashMap::new, java.util.stream.Collectors.toList()));
        return byUdeo.values().stream().map(hospitalRows -> {
            String hospital = hospitalRows.get(0).hospital();
            if (hospitalRows.stream().anyMatch(row -> !hospital.equals(row.hospital()))) {
                throw ApiException.conflict("UDEO is associated with more than one hospital name: "
                        + hospitalRows.get(0).udeo());
            }
            Organizacion organization = findOrCreateOrganization(hospitalRows.get(0).udeo(), hospital);
            return closeHistorical(organization, user, file.getOriginalFilename(),
                    hospitalRows.stream().map(ExcelImportParser.HistoricalRow::reemplazante).toList());
        }).toList();
    }

    @Transactional(readOnly = true)
    public ImportacionDtos.Detail get(Long id) {
        Importacion importacion = find(id);
        requireAccess(importacion, CurrentUser.get());
        return detail(importacion, rows.findByImportacionId(id));
    }

    @Transactional(readOnly = true)
    public Page<ImportacionDtos.Summary> list(Long organizationId, Pageable pageable) {
        CurrentUser user = CurrentUser.get();
        if (user.global()) {
            user.requireGlobal("IMPORTACIONES_VER_TODAS");
            return imports.findAll(pageable).map(this::summary);
        }
        if (organizationId == null) throw ApiException.bad("organizacionId is required");
        user.requireForOrganization(organizationId, "IMPORTACIONES_VER_PROPIAS");
        return imports.findByOrganizacionId(organizationId, pageable).map(this::summary);
    }

    @Transactional(readOnly = true)
    public Page<ImportacionDtos.Summary> adminList(Long organizationId, ImportStatus status,
            Instant from, Instant to, String email, Pageable pageable) {
        CurrentUser.get().requireGlobal("IMPORTACIONES_VER_TODAS");
        Specification<Importacion> specification = (root, query, builder) -> builder.conjunction();
        if (organizationId != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("organizacion").get("id"), organizationId));
        if (status != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("estado"), status));
        if (from != null) specification = specification.and((root, query, builder) ->
                builder.greaterThanOrEqualTo(root.get("fechaCreacion"), from));
        if (to != null) specification = specification.and((root, query, builder) ->
                builder.lessThanOrEqualTo(root.get("fechaCreacion"), to));
        if (email != null && !email.isBlank()) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("creadoPor").get("email"), email));
        return imports.findAll(specification, pageable).map(this::summary);
    }

    @Transactional
    public ImportacionDtos.Detail edit(Long id, Long rowId, ImportacionDtos.RowEdit edit) {
        Importacion importacion = find(id);
        CurrentUser user = CurrentUser.get();
        requireAccess(importacion, user);
        user.requireForOrganization(importacion.getOrganizacion().getId(), "IMPORTACIONES_EDITAR_PROPIAS");
        requireCreator(importacion, user);
        if (importacion.getEstado() != ImportStatus.EN_PROCESO && importacion.getEstado() != ImportStatus.OBSERVADA) {
            throw ApiException.conflict("Importation is not editable");
        }
        ImportacionReemplazante row = rows.findByIdAndImportacionId(rowId, id)
                .orElseThrow(() -> ApiException.notFound("Import row not found"));
        row.edit(edit.agrupamiento(), edit.funcionTitular(), edit.tipoLicencia(), edit.observaciones(), edit.lugarTrabajo());
        row.setErrores(parser.validate(row));
        List<ImportacionReemplazante> importRows = rows.findByImportacionId(id);
        recount(importacion, importRows);
        return detail(importacion, importRows);
    }

    @Transactional
    public ImportacionDtos.Detail submit(Long id) {
        Importacion importacion = find(id);
        CurrentUser user = CurrentUser.get();
        requireAccess(importacion, user);
        user.requireForOrganization(importacion.getOrganizacion().getId(), "IMPORTACIONES_ENVIAR_REVISION");
        requireCreator(importacion, user);
        if (importacion.getEstado() != ImportStatus.EN_PROCESO && importacion.getEstado() != ImportStatus.OBSERVADA) {
            throw ApiException.conflict("Importation cannot be submitted");
        }
        List<ImportacionReemplazante> importRows = rows.findByImportacionId(id);
        importRows.forEach(row -> row.setErrores(parser.validate(row)));
        recount(importacion, importRows);
        if (importacion.getCantidadFilasConError() > 0) throw ApiException.conflict("Importation has blocking row errors");
        importacion.submit();
        return detail(importacion, importRows);
    }

    @Transactional
    public ImportacionDtos.Detail observe(Long id, String reason) {
        Importacion importacion = find(id);
        requireAdmin("IMPORTACIONES_OBSERVAR");
        if (importacion.getEstado() != ImportStatus.PENDIENTE_REVISION) throw ApiException.conflict("Only pending importations can be observed");
        importacion.observe(authentication.entity(CurrentUser.get().email()), reason);
        return detail(importacion, rows.findByImportacionId(id));
    }

    @Transactional
    public ImportacionDtos.Detail reject(Long id, String reason) {
        Importacion importacion = find(id);
        requireAdmin("IMPORTACIONES_RECHAZAR");
        if (importacion.getEstado() != ImportStatus.PENDIENTE_REVISION) throw ApiException.conflict("Only pending importations can be rejected");
        importacion.reject(authentication.entity(CurrentUser.get().email()), reason);
        return detail(importacion, rows.findByImportacionId(id));
    }

    @Transactional
    public ImportacionDtos.Detail approveAndClose(Long id) {
        CurrentUser user = CurrentUser.get();
        user.requireGlobal("IMPORTACIONES_APROBAR");
        user.requireGlobal("IMPORTACIONES_CERRAR");
        Importacion importacion = imports.lockById(id).orElseThrow(() -> ApiException.notFound("Importation not found"));
        if (importacion.getCreadoPor().getId().equals(user.id())) throw ApiException.forbidden("An importation creator cannot approve it");
        if (importacion.getEstado() != ImportStatus.PENDIENTE_REVISION) throw ApiException.conflict("Only pending importations can be approved and closed");
        List<ImportacionReemplazante> importRows = rows.findByImportacionId(id);
        importRows.forEach(row -> row.setErrores(parser.validate(row)));
        recount(importacion, importRows);
        if (importacion.getCantidadFilasConError() > 0) throw ApiException.conflict("Importation has blocking row errors");
        if (history.existsByImportacionId(id)) throw ApiException.conflict("Importation has already been closed");
        history.saveAll(importRows.stream().map(ReemplazanteHistorico::from).toList());
        importacion.close(authentication.entity(user.email()));
        return detail(importacion, importRows);
    }

    private Importacion find(Long id) {
        return imports.findById(id).orElseThrow(() -> ApiException.notFound("Importation not found"));
    }

    private List<ImportacionReemplazante> parseUpload(MultipartFile file) {
        validateUploadFile(file);
        try {
            return parser.parse(file.getInputStream());
        } catch (IOException exception) {
            throw ApiException.bad("Cannot read upload");
        }
    }

    private List<ExcelImportParser.HistoricalRow> parseHistoricalUpload(MultipartFile file) {
        validateUploadFile(file);
        try {
            return parser.parseHistorical(file.getInputStream());
        } catch (IOException exception) {
            throw ApiException.bad("Cannot read upload");
        }
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.bad("File is empty");
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw ApiException.bad("Only .xlsx files are accepted");
        }
    }

    private Organizacion findOrCreateOrganization(String udeo, String hospital) {
        return organizations.findByUdeo(udeo).map(organization -> {
            if (!organization.getNombre().equals(hospital)) {
                throw ApiException.conflict("Hospital name does not match UDEO " + udeo);
            }
            return organization;
        }).orElseGet(() -> {
            Organizacion organization = new Organizacion();
            organization.setUdeo(udeo);
            organization.setNombre(hospital);
            return organizations.save(organization);
        });
    }

    private ImportacionDtos.Detail closeHistorical(Organizacion organization, CurrentUser user, String filename,
            List<ImportacionReemplazante> importRows) {
        Importacion importacion = new Importacion();
        importacion.setOrganizacion(organization);
        importacion.setCreadoPor(authentication.entity(user.email()));
        importacion.setArchivoOrigen(filename);
        importacion.setCantidadFilas(importRows.size());
        importRows.forEach(row -> row.setImportacion(importacion));
        imports.save(importacion);
        rows.saveAll(importRows);
        recount(importacion, importRows);
        if (importacion.getCantidadFilasConError() > 0) {
            throw ApiException.conflict("Historical importation has blocking row errors");
        }
        history.saveAll(importRows.stream().map(ReemplazanteHistorico::from).toList());
        importacion.close(authentication.entity(user.email()));
        return detail(importacion, importRows);
    }

    private void requireAccess(Importacion importacion, CurrentUser user) {
        if (user.global()) user.requireGlobal("IMPORTACIONES_VER_TODAS");
        else user.requireForOrganization(importacion.getOrganizacion().getId(), "IMPORTACIONES_VER_PROPIAS");
    }

    private void requireCreator(Importacion importacion, CurrentUser user) {
        if (!user.global() && !importacion.getCreadoPor().getId().equals(user.id())) {
            throw ApiException.forbidden("Only the importation creator may perform this action");
        }
    }

    private void requireAdmin(String permission) {
        CurrentUser.get().requireGlobal(permission);
    }

    private void recount(Importacion importacion, List<ImportacionReemplazante> importRows) {
        importacion.recount((int) importRows.stream().filter(row -> row.getErrores() == null || row.getErrores().isBlank()).count(),
                (int) importRows.stream().filter(row -> row.getErrores() != null && !row.getErrores().isBlank()).count());
    }

    private ImportacionDtos.Summary summary(Importacion importacion) {
        return new ImportacionDtos.Summary(importacion.getId(), importacion.getOrganizacion().getId(),
                importacion.getOrganizacion().getNombre(), importacion.getArchivoOrigen(), importacion.getEstado(),
                importacion.getCreadoPor().getEmail(), importacion.getFechaCreacion(), importacion.getFechaEnvioRevision(),
                importacion.getCantidadFilas(), importacion.getCantidadFilasConError());
    }

    private ImportacionDtos.Detail detail(Importacion importacion, List<ImportacionReemplazante> importRows) {
        return new ImportacionDtos.Detail(importacion.getId(), importacion.getOrganizacion().getId(), importacion.getArchivoOrigen(),
                importacion.getEstado(), importacion.getCantidadFilas(), importacion.getCantidadFilasValidas(),
                importacion.getCantidadFilasConError(), importacion.getMotivoObservacion(), importacion.getMotivoRechazo(),
                importacion.getFechaCreacion(), importacion.getFechaEnvioRevision(), importacion.getFechaRevision(), importacion.getFechaCierre(),
                importRows.stream().map(row -> new ImportacionDtos.Row(row.getId(), row.getFilaOrigen(), row.getReemplazante(),
                        row.getCuil(), row.getCategoriaReemplazante(), row.getInstrumentoLegal(), row.getPeriodoDesde(), row.getPeriodoHasta(),
                        row.getAPagar(), row.getADeuda(), row.getTitular(), row.getCategoriaTitular(), row.getLicenciaDesde(),
                        row.getLicenciaHasta(), row.getCantidadDias(), row.getTipoLicenciaExcel(), row.getAgrupamiento(),
                        row.getFuncionTitular(), row.getTipoLicencia(), row.getObservaciones(), row.getLugarTrabajo(), row.getErrores())).toList());
    }
}
