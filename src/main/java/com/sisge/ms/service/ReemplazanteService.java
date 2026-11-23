package com.sisge.ms.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sisge.ms.dto.reemplazante.ReemplazanteDtos;
import com.sisge.ms.entity.ReemplazanteHistorico;
import com.sisge.ms.exception.ApiException;
import com.sisge.ms.repository.ReemplazanteHistoricoRepository;
import com.sisge.ms.security.CurrentUser;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class ReemplazanteService {
    private static final int MAX_REPORT_ROWS = 10_000;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReemplazanteHistoricoRepository repository;

    public ReemplazanteService(ReemplazanteHistoricoRepository repository) {
        this.repository = repository;
    }

    public record Search(Long organizacionId, String dni, String cuil, String reemplazante, String titular,
            String lugarTrabajo, String hospital, String udeo, Long importacionId, LocalDate fechaCarga) {}

    public Page<ReemplazanteDtos.View> list(Search search, int page, int size) {
        authorizeSearch(search.organizacionId());
        return repository.findAll(specification(search),
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100)))
                .map(ReemplazanteDtos::from);
    }

    public ReemplazanteDtos.View get(Long id) {
        ReemplazanteHistorico historical = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Historical replacement not found"));
        if (historical.getOrganizacion() == null) CurrentUser.get().requireGlobal("REEMPLAZANTES_VER");
        else CurrentUser.get().requireForOrganization(historical.getOrganizacion().getId(), "REEMPLAZANTES_VER");
        return ReemplazanteDtos.from(historical);
    }

    public byte[] pdf(Search search) {
        authorizeSearch(search.organizacionId());
        List<ReemplazanteHistorico> rows = repository.findAll(specification(search),
                PageRequest.of(0, MAX_REPORT_ROWS, Sort.by("id").ascending())).getContent();
        return generatePdf(rows, rows.size() == MAX_REPORT_ROWS);
    }

    private void authorizeSearch(Long organizacionId) {
        CurrentUser user = CurrentUser.get();
        if (!user.global() && organizacionId == null) throw ApiException.bad("organizacionId is required");
        if (organizacionId != null) user.requireForOrganization(organizacionId, "REEMPLAZANTES_VER");
        else user.requireGlobal("REEMPLAZANTES_VER");
    }

    private Specification<ReemplazanteHistorico> specification(Search search) {
        Specification<ReemplazanteHistorico> specification = (root, query, builder) -> builder.conjunction();
        if (search.organizacionId() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("organizacion").get("id"), search.organizacionId()));
        if (search.dni() != null && !search.dni().isBlank()) {
            String normalizedDni = search.dni().replaceAll("\\D", "");
            if (!normalizedDni.isEmpty()) specification = specification.and((root, query, builder) ->
                    builder.like(root.get("dni"), "%" + normalizedDni + "%"));
        }
        if (search.cuil() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("cuil"), search.cuil()));
        if (search.reemplazante() != null) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("reemplazante")), "%" + search.reemplazante().toLowerCase() + "%"));
        if (search.titular() != null) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("titular")), "%" + search.titular().toLowerCase() + "%"));
        if (search.lugarTrabajo() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("lugarTrabajo"), search.lugarTrabajo()));
        if (search.hospital() != null && !search.hospital().isBlank()) specification = specification.and((root, query, builder) ->
                builder.like(builder.lower(root.get("hospitalNombre")), "%" + search.hospital().toLowerCase() + "%"));
        if (search.udeo() != null && !search.udeo().isBlank()) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("udeo"), search.udeo()));
        if (search.importacionId() != null) specification = specification.and((root, query, builder) ->
                builder.equal(root.get("importacion").get("id"), search.importacionId()));
        if (search.fechaCarga() != null) {
            Instant from = search.fechaCarga().atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = search.fechaCarga().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            specification = specification.and((root, query, builder) ->
                    builder.between(root.get("fechaCierre"), from, to));
        }
        return specification;
    }

    private byte[] generatePdf(List<ReemplazanteHistorico> rows, boolean truncated) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

            document.add(new Paragraph("Reporte de reemplazantes", titleFont));
            document.add(new Paragraph("Generado: "
                    + Instant.now().atZone(ZoneOffset.UTC).format(TIMESTAMP_FORMAT)
                    + " UTC   |   Registros: " + rows.size(), metaFont));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[] {4f, 2f, 2f, 5f});
            table.setWidthPercentage(100);
            addCell(table, "Reemplazante", headerFont, true);
            addCell(table, "Desde", headerFont, true);
            addCell(table, "Hasta", headerFont, true);
            addCell(table, "Hospital", headerFont, true);
            for (ReemplazanteHistorico row : rows) {
                addCell(table, row.getReemplazante(), bodyFont, false);
                addCell(table, format(row.getPeriodoDesde()), bodyFont, false);
                addCell(table, format(row.getPeriodoHasta()), bodyFont, false);
                addCell(table, hospital(row), bodyFont, false);
            }
            document.add(table);

            if (truncated) {
                document.add(new Paragraph("Reporte limitado a " + MAX_REPORT_ROWS + " registros.", metaFont));
            }
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el reporte PDF", exception);
        }
    }

    private void addCell(PdfPTable table, String value, Font font, boolean header) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setPadding(4f);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (header) cell.setBackgroundColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private String format(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private String hospital(ReemplazanteHistorico row) {
        String name = row.getHospitalNombre() == null ? "" : row.getHospitalNombre();
        return row.getUdeo() == null || row.getUdeo().isBlank() ? name : name + " (" + row.getUdeo() + ")";
    }
}
