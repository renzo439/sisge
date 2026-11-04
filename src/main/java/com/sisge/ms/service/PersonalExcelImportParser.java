package com.sisge.ms.service;

import com.sisge.ms.entity.ImportacionPersonalFila;
import com.sisge.ms.exception.ApiException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class PersonalExcelImportParser {
    private static final Map<String, String> FIELDS = Map.ofEntries(
            Map.entry("dni", "DNI"), Map.entry("cuil", "CUIL"), Map.entry("nombres", "Nombres"),
            Map.entry("apellido", "Apellido"), Map.entry("sexo", "Sexo"), Map.entry("fecha nacimiento", "Fecha Nacimiento"),
            Map.entry("organizacion", "Organizacion"), Map.entry("sector", "Sector"), Map.entry("escalafon", "Escalafon"),
            Map.entry("categoria", "Categoria"), Map.entry("situacion revista", "Situacion Revista"),
            Map.entry("agrupamiento", "Agrupamiento"), Map.entry("funcion", "Funcion"), Map.entry("titulo", "Titulo"),
            Map.entry("adicional", "Adicional"), Map.entry("antiguedad", "Antiguedad"), Map.entry("afectado", "Afectado"),
            Map.entry("fecha inicio", "Fecha Inicio"));
    public List<ImportacionPersonalFila> parse(InputStream input) {
        try (Workbook book = new XSSFWorkbook(input)) {
            Sheet sheet = book.getNumberOfSheets() == 0 ? null : book.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) throw ApiException.bad("The workbook is empty");
            Map<String, Integer> columns = columns(sheet.getRow(sheet.getFirstRowNum()));
            for (String required : List.of("dni", "nombres", "apellido", "fecha inicio")) if (!columns.containsKey(required)) throw ApiException.bad("Missing required header: " + FIELDS.get(required));
            List<ImportacionPersonalFila> result = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row excelRow = sheet.getRow(i);
                if (excelRow == null || blank(excelRow)) continue;
                Map<String, String> raw = new LinkedHashMap<>();
                for (String field : FIELDS.keySet()) raw.put(FIELDS.get(field), value(excelRow, columns.get(field)));
                ImportacionPersonalFila row = new ImportacionPersonalFila();
                row.setFilaOrigen(i + 1);
                row.setValoresCrudos(json(raw));
                List<String> errors = new ArrayList<>();
                if (raw.get("DNI").isBlank()) errors.add("dni:REQUIRED");
                if (raw.get("Nombres").isBlank()) errors.add("nombres:REQUIRED");
                if (raw.get("Apellido").isBlank()) errors.add("apellido:REQUIRED");
                if (date(raw.get("Fecha Inicio")) == null) errors.add("fechaInicio:INVALID_DATE");
                row.setErrores(String.join(";", errors));
                result.add(row);
            }
            if (result.isEmpty()) throw ApiException.bad("The workbook has no data rows");
            return result;
        } catch (ApiException exception) { throw exception; }
        catch (Exception exception) { throw ApiException.bad("Unreadable .xlsx workbook"); }
    }
    private Map<String, Integer> columns(Row header) {
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : header) {
            String normalized = normalize(text(cell));
            if (FIELDS.containsKey(normalized)) result.put(normalized, cell.getColumnIndex());
        }
        return result;
    }
    private String value(Row row, Integer column) { return column == null ? "" : text(row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); }
    private boolean blank(Row row) { for (Cell cell : row) if (!text(cell).isBlank()) return false; return true; }
    private String text(Cell cell) { return cell == null ? "" : new DataFormatter().formatCellValue(cell).trim(); }
    private String normalize(String value) { return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " "); }
    private LocalDate date(String value) { try { return value.isBlank() ? null : LocalDate.parse(value); } catch (Exception ignored) { try { return LocalDate.parse(value, DateTimeFormatter.ofPattern("d/M/uuuu")); } catch (Exception ignoredAgain) { return null; } } }
    private String json(Map<String, String> values) {
        return values.entrySet().stream().map(entry -> "\"" + escape(entry.getKey()) + "\":\"" + escape(entry.getValue()) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
}
