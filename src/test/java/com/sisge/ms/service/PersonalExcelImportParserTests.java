package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.*;
import com.sisge.ms.exception.ApiException;
import java.io.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class PersonalExcelImportParserTests {
    private final PersonalExcelImportParser parser = new PersonalExcelImportParser();
    @Test
    void stagesRawValuesAndReportsInvalidStartDate() throws Exception {
        try (var workbook = workbook(); var output = new ByteArrayOutputStream()) {
            workbook.write(output);
            var row = parser.parse(new ByteArrayInputStream(output.toByteArray())).get(0);
            assertEquals(2, row.getFilaOrigen());
            assertTrue(row.getValoresCrudos().contains("\"Titulo\":\"Licenciada\""));
            assertEquals("fechaInicio:INVALID_DATE", row.getErrores());
        }
    }
    @Test
    void rejectsWorkbookWithoutIdentityHeaders() throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            workbook.createSheet().createRow(0).createCell(0).setCellValue("CUIL");
            workbook.write(output);
            assertThrows(ApiException.class, () -> parser.parse(new ByteArrayInputStream(output.toByteArray())));
        }
    }
    @Test
    void acceptsCensusHeadersWithCombinedIdentityAndName() throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            var header = sheet.createRow(0);
            String[] headers = {"DNI / CUIL", "NOMBRES", "F. DE INICIO"};
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            var row = sheet.createRow(1);
            String[] values = {"11664837 20116648378", "CRUZ, ALBERTO", "27/07/2020"};
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
            workbook.write(output);

            var parsed = parser.parse(new ByteArrayInputStream(output.toByteArray())).get(0);

            assertEquals("11664837", parsed.getDni());
            assertEquals("20116648378", parsed.getCuil());
            assertEquals("ALBERTO", parsed.getNombres());
            assertEquals("CRUZ", parsed.getApellido());
            assertTrue(parsed.getErrores().isBlank());
        }
    }
    @Test
        void retainsUnknownSourceColumnsInRawValues() throws Exception {
            try (var workbook = workbook(); var output = new ByteArrayOutputStream()) {
                var sheet=workbook.getSheetAt(0);
                sheet.getRow(0).createCell(6).setCellValue("Columna ajena");
                sheet.getRow(1).createCell(6).setCellValue("valor original");
                workbook.write(output);
                var row=parser.parse(new ByteArrayInputStream(output.toByteArray())).get(0);
                assertTrue(row.getValoresCrudos().contains("\"Columna ajena\":\"valor original\""));
        }
    }
    private XSSFWorkbook workbook() {
        var workbook = new XSSFWorkbook(); var sheet = workbook.createSheet(); var header = sheet.createRow(0);
        String[] headers = {"DNI", "Nombres", "Apellido", "Fecha Inicio", "Titulo", "CUIL"};
        for (int i=0; i<headers.length; i++) header.createCell(i).setCellValue(headers[i]);
        var row = sheet.createRow(1);
        String[] values = {"12345678", "Ana", "Pérez", "not-a-date", "Licenciada", "20-12345678-6"};
        for (int i=0; i<values.length; i++) row.createCell(i).setCellValue(values[i]);
        return workbook;
    }
}
