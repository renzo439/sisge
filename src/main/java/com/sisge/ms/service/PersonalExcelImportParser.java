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
            Map.entry("organizacion", "Organizacion"), Map.entry("udo presupuestaria", "UDO Presupuestaria"), Map.entry("dependencia", "Dependencia"), Map.entry("sector", "Sector"), Map.entry("escalafon", "Escalafon"),
            Map.entry("categoria", "Categoria"), Map.entry("situacion revista", "Situacion Revista"),
            Map.entry("agrupamiento", "Agrupamiento"), Map.entry("funcion", "Funcion"), Map.entry("titulo", "Titulo"),
            Map.entry("adicional", "Adicional"), Map.entry("adicional m h", "Adicional M H"), Map.entry("adicional por jefatura", "Adicional por Jefatura"), Map.entry("tipo afectacion", "Tipo Afectacion"), Map.entry("antiguedad", "Antiguedad"), Map.entry("afectado", "Afectado"),
            Map.entry("fecha inicio", "Fecha Inicio"));
    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("apellido y nombre", "nombre completo"),
            Map.entry("apellido y nombres", "nombre completo"),
            Map.entry("apellidos y nombres", "nombre completo"),
            Map.entry("apell y nomb", "nombre completo"),
            Map.entry("apell nomb", "nombre completo"),
            Map.entry("ape y nom", "nombre completo"),
            Map.entry("ape nom", "nombre completo"),
            Map.entry("nombre y apellido", "nombre completo"),
            Map.entry("nombres y apellidos", "nombre completo"),
            Map.entry("dni cuil", "dni cuil"),
            Map.entry("f de inicio", "fecha inicio"),
            Map.entry("fecha de inicio", "fecha inicio"),
            Map.entry("f nac", "fecha nacimiento"),
            Map.entry("fecha nac", "fecha nacimiento"),
            Map.entry("udo presu", "udo presupuestaria"),
            Map.entry("situacion de revista", "situacion revista"));
    public List<ImportacionPersonalFila> parse(InputStream input) {
        try (Workbook book = new XSSFWorkbook(input)) {
            Sheet sheet = book.getNumberOfSheets() == 0 ? null : book.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) throw ApiException.bad("The workbook is empty");
            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> columns = columns(header);
            Map<Integer, String> sourceHeaders = sourceHeaders(header);
            required(columns);
            List<ImportacionPersonalFila> result = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row excelRow = sheet.getRow(i);
                if (excelRow == null || blank(excelRow)) continue;
                Map<String, String> raw = new LinkedHashMap<>();
                for (Map.Entry<Integer,String> source : sourceHeaders.entrySet()) raw.put(source.getValue(), value(excelRow, source.getKey()));
                Map<String, String> known = new HashMap<>();
                for (String field : FIELDS.keySet()) known.put(FIELDS.get(field), value(excelRow, columns.get(field)));
                String fullName = value(excelRow, columns.get("nombre completo"));
                Name name = name(known.get("Nombres"), known.get("Apellido"), fullName);
                Identity identity = identity(known.get("DNI"), known.get("CUIL"), value(excelRow, columns.get("dni cuil")));
                ImportacionPersonalFila row = new ImportacionPersonalFila();
                row.setFilaOrigen(i + 1);
                row.setValoresCrudos(json(raw));
                List<String> errors = new ArrayList<>();
                if (identity.dni().isBlank()) errors.add("dni:REQUIRED");
                if (name.nombres().isBlank()) errors.add("nombres:REQUIRED");
                if (name.apellido().isBlank()) errors.add("apellido:REQUIRED");
                if (date(known.get("Fecha Inicio")) == null) errors.add("fechaInicio:INVALID_DATE");
                row.setIdentity(identity.dni(), identity.cuil(), name.nombres(), name.apellido(), known.get("Sexo"), date(known.get("Fecha Nacimiento")));
                row.setWorkValues(date(known.get("Fecha Inicio")), integer(known.get("Antiguedad")), affected(known.get("Afectado")), known.get("Adicional M H"), known.get("Adicional por Jefatura"));
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
            String field = field(normalize(text(cell)));
            if (field != null) result.put(field, cell.getColumnIndex());
        }
        return result;
    }
    private void required(Map<String, Integer> columns) {
        if (!columns.containsKey("dni") && !columns.containsKey("dni cuil")) throw ApiException.bad("Missing required header: DNI");
        if (!columns.containsKey("nombres") && !columns.containsKey("nombre completo")) throw ApiException.bad("Missing required header: Nombres");
        if (!columns.containsKey("fecha inicio")) throw ApiException.bad("Missing required header: Fecha Inicio");
    }
    private String field(String header) {
        if (FIELDS.containsKey(header)) return header;
        String alias = HEADER_ALIASES.get(header);
        if (alias != null) return alias;
        return header.contains("ape") && header.contains("nom") ? "nombre completo" : null;
    }
    private Map<Integer, String> sourceHeaders(Row header) {
        Map<Integer, String> result = new LinkedHashMap<>();
        Map<String,Integer> occurrences = new HashMap<>();
        for (Cell cell : header) {
            String name=text(cell); if(name.isBlank()) name="COLUMNA_"+(cell.getColumnIndex()+1);
            int occurrence=occurrences.merge(name,1,Integer::sum);
            result.put(cell.getColumnIndex(), occurrence == 1 ? name : name+" ["+occurrence+"]");
        }
        return result;
    }
    private String value(Row row, Integer column) { return column == null ? "" : text(row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)); }
    private boolean blank(Row row) { for (Cell cell : row) if (!text(cell).isBlank()) return false; return true; }
    private String text(Cell cell) { return cell == null ? "" : new DataFormatter().formatCellValue(cell).trim(); }
    public static String normalize(String value) { return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " "); }
    private LocalDate date(String value) { try { return value.isBlank() ? null : LocalDate.parse(value); } catch (Exception ignored) { try { return LocalDate.parse(value, DateTimeFormatter.ofPattern("d/M/uuuu")); } catch (Exception ignoredAgain) { return null; } } }
    private String json(Map<String, String> values) {
        return values.entrySet().stream().map(entry -> "\"" + escape(entry.getKey()) + "\":\"" + escape(entry.getValue()) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r"); }
    private String digits(String value){ return value == null ? null : value.replaceAll("\\D",""); }
    private Name name(String nombres, String apellido, String fullName) {
        if (!apellido.isBlank()) return new Name(nombres, apellido);
        String source = nombres.isBlank() ? fullName : nombres;
        int separator = source.indexOf(',');
        if (separator < 0) return new Name("", "");
        return new Name(source.substring(separator + 1).trim(), source.substring(0, separator).trim());
    }
    private Identity identity(String dni, String cuil, String combined) {
        if (!dni.isBlank() || !cuil.isBlank()) return new Identity(digits(dni), digits(cuil));
        String[] parts = combined.trim().split("\\D+");
        if (parts.length >= 2) return new Identity(digits(parts[0]), digits(parts[1]));
        String value = digits(combined);
        return value.length() == 11 ? new Identity(value.substring(2, 10), value) : new Identity(value, "");
    }
    private Integer integer(String value){ try{return value == null || value.isBlank()?null:Integer.valueOf(value.trim());}catch(NumberFormatException e){return null;} }
    private Boolean affected(String value){ String normalized=normalize(value); if(normalized.isBlank()) return null; if(normalized.equals("si")||normalized.equals("si afectado")||normalized.equals("afectado externo")||normalized.equals("afectada externa")) return true; if(normalized.equals("no")) return false; return null; }
    private record Name(String nombres, String apellido) {}
    private record Identity(String dni, String cuil) {}
}
