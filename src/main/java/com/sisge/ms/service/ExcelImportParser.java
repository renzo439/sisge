package com.sisge.ms.service;

import com.sisge.ms.entity.ImportacionReemplazante;
import com.sisge.ms.exception.ApiException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
public class ExcelImportParser {
    private static final List<String> HEADERS = List.of(
            "Reemplazante", "CUIL", "Categoria", "Instr.Legal", "Periodo Desde", "Periodo Hasta",
            "A pagar", "A deuda", "Titular", "Categoria", "Licencia Desde", "Licencia Hasta",
            "Cantidad dias", "Tipo de Licencia");
    private static final List<String> HISTORICAL_HEADERS = List.of("Hospital", "UDEO");

    public record HistoricalRow(String hospital, String udeo, ImportacionReemplazante reemplazante) {
    }

    public List<ImportacionReemplazante> parse(InputStream input) {
        return parseRows(input, false).stream().map(ParsedRow::reemplazante).toList();
    }

    public List<HistoricalRow> parseHistorical(InputStream input) {
        return parseRows(input, true).stream()
                .map(row -> new HistoricalRow(row.hospital(), row.udeo(), row.reemplazante()))
                .toList();
    }

    private List<ParsedRow> parseRows(InputStream input, boolean historical) {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                throw ApiException.bad("The workbook is empty");
            }

            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> columns = columns(header);
            List<String> requiredHeaders = new ArrayList<>(HEADERS);
            if (historical) {
                requiredHeaders.addAll(HISTORICAL_HEADERS);
            }
            for (String requiredHeader : requiredHeaders) {
                if (!columns.containsKey(requiredHeader)) {
                    throw ApiException.bad("Missing required header: " + requiredHeader);
                }
            }
            if (!columns.containsKey("Categoria.1")) {
                throw ApiException.bad("Missing required header: Categoria (second column)");
            }

            List<ParsedRow> parsed = new ArrayList<>();
            for (int rowIndex = header.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || blank(row)) {
                    continue;
                }
                ImportacionReemplazante reemplazante = replacement(row, columns, rowIndex + 1);
                String hospital = historical ? value(row, columns, "Hospital") : null;
                String udeo = historical ? value(row, columns, "UDEO") : null;
                if (historical && (hospital == null || hospital.isBlank() || udeo == null || udeo.isBlank())) {
                    throw ApiException.bad("Hospital and UDEO are required for every historical row");
                }
                parsed.add(new ParsedRow(hospital, udeo, reemplazante));
            }
            if (parsed.isEmpty()) {
                throw ApiException.bad("The workbook has no data rows");
            }
            return parsed;
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw ApiException.bad("Unreadable .xlsx workbook");
        }
    }

    private Map<String, Integer> columns(Row header) {
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            String name = text(cell);
            if ("Categoria".equals(name) && columns.containsKey(name)) {
                name = "Categoria.1";
            }
            columns.put(name, cell.getColumnIndex());
        }
        return columns;
    }

    private ImportacionReemplazante replacement(Row row, Map<String, Integer> columns, int sourceRow) {
        ImportacionReemplazante replacement = new ImportacionReemplazante();
        replacement.setFilaOrigen(sourceRow);
        replacement.setReemplazante(value(row, columns, "Reemplazante"));
        replacement.setCuil(value(row, columns, "CUIL"));
        replacement.setCategoriaReemplazante(value(row, columns, "Categoria"));
        replacement.setInstrumentoLegal(value(row, columns, "Instr.Legal"));
        replacement.setPeriodoDesde(date(row, columns, "Periodo Desde"));
        replacement.setPeriodoHasta(date(row, columns, "Periodo Hasta"));
        replacement.setAPagar(money(row, columns, "A pagar"));
        replacement.setADeuda(money(row, columns, "A deuda"));
        replacement.setTitular(value(row, columns, "Titular"));
        replacement.setCategoriaTitular(value(row, columns, "Categoria.1"));
        replacement.setLicenciaDesde(date(row, columns, "Licencia Desde"));
        replacement.setLicenciaHasta(date(row, columns, "Licencia Hasta"));
        replacement.setCantidadDias(integer(row, columns, "Cantidad dias"));
        replacement.setTipoLicenciaExcel(value(row, columns, "Tipo de Licencia"));
        replacement.setErrores(validate(replacement));
        return replacement;
    }

    public String validate(ImportacionReemplazante replacement) {
        List<String> errors = new ArrayList<>();
        if (replacement.getReemplazante() == null || replacement.getReemplazante().isBlank()) {
            errors.add("reemplazante:REQUIRED");
        }
        if (!validCuil(replacement.getCuil())) {
            errors.add("cuil:INVALID");
        }
        if (replacement.getPeriodoDesde() == null || replacement.getPeriodoHasta() == null) {
            errors.add("periodo:INVALID_DATE");
        } else if (replacement.getPeriodoDesde().isAfter(replacement.getPeriodoHasta())) {
            errors.add("periodo:INVALID_RANGE");
        }
        if (replacement.getLicenciaDesde() != null && replacement.getLicenciaHasta() != null
                && replacement.getLicenciaDesde().isAfter(replacement.getLicenciaHasta())) {
            errors.add("licencia:INVALID_RANGE");
        }
        if (replacement.getCantidadDias() != null && replacement.getCantidadDias() < 0) {
            errors.add("cantidadDias:INVALID");
        }
        return String.join(";", errors);
    }

    private String value(Row row, Map<String, Integer> columns, String header) {
        Cell cell = row.getCell(columns.get(header), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? null : text(cell);
    }

    private String text(Cell cell) {
        return cell == null ? "" : new DataFormatter().formatCellValue(cell).trim();
    }

    private boolean blank(Row row) {
        for (Cell cell : row) {
            if (!text(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private LocalDate date(Row row, Map<String, Integer> columns, String header) {
        Cell cell = row.getCell(columns.get(header), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null || text(cell).isBlank()) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            if (cell.getCellType() == CellType.NUMERIC) {
                return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
            }
            String value = text(cell);
            try {
                return LocalDate.parse(value);
            } catch (Exception ignored) {
                try {
                    return LocalDate.parse(value, DateTimeFormatter.ofPattern("d/M/uuuu"));
                } catch (Exception ignoredAgain) {
                    return LocalDate.parse(value, DateTimeFormatter.ofPattern("d/M/uu"));
                }
            }
        } catch (Exception exception) {
            return null;
        }
    }

    private BigDecimal money(Row row, Map<String, Integer> columns, String header) {
        String value = value(row, columns, header);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String normalized = value.replaceAll("[^0-9,.-]", "");
            int comma = normalized.lastIndexOf(',');
            int dot = normalized.lastIndexOf('.');
            if (comma >= 0 && dot >= 0) {
                normalized = comma > dot ? normalized.replace(".", "").replace(",", ".") : normalized.replace(",", "");
            } else if (comma >= 0) {
                normalized = normalized.replace(",", ".");
            } else if (dot >= 0 && normalized.length() - dot - 1 != 2) {
                normalized = normalized.replace(".", "");
            }
            return new BigDecimal(normalized);
        } catch (Exception exception) {
            return null;
        }
    }

    private Integer integer(Row row, Map<String, Integer> columns, String header) {
        String value = value(row, columns, header);
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value.replace(",", ".")).intValueExact();
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean validCuil(String value) {
        if (value == null) {
            return false;
        }
        String cuil = value.replaceAll("\\D", "");
        if (!cuil.matches("\\d{11}")) {
            return false;
        }
        int[] weights = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int total = 0;
        for (int index = 0; index < 10; index++) {
            total += (cuil.charAt(index) - '0') * weights[index];
        }
        int check = 11 - (total % 11);
        if (check == 11) {
            check = 0;
        } else if (check == 10) {
            check = 9;
        }
        return check == cuil.charAt(10) - '0';
    }

    private record ParsedRow(String hospital, String udeo, ImportacionReemplazante reemplazante) {
    }
}
