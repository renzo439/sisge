package com.sisge.ms.service;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*; import java.time.LocalDate; import org.apache.poi.xssf.usermodel.XSSFWorkbook; import org.junit.jupiter.api.Test; import com.sisge.ms.exception.ApiException;

class ExcelImportParserTests {
 private final ExcelImportParser parser=new ExcelImportParser();
 private static final String[] HEADERS={"Reemplazante","CUIL","Categoria","Instr.Legal","Periodo Desde","Periodo Hasta","A pagar","A deuda","Titular","Categoria.1","Licencia Desde","Licencia Hasta","Cantidad dias","Tipo de Licencia"};
 @Test void parsesValidRowAndPreservesOriginalLicense() throws Exception {
  try(var wb=workbook(true);var out=new ByteArrayOutputStream()){wb.write(out);var rows=parser.parse(new ByteArrayInputStream(out.toByteArray()));assertEquals(1,rows.size());var row=rows.get(0);assertEquals("Licencia Excel",row.getTipoLicenciaExcel());assertEquals(LocalDate.of(2026,1,1),row.getPeriodoDesde());assertTrue(row.getErrores().isBlank());}
 }
 @Test void rejectsMissingRequiredHeader() throws Exception {
  try(var wb=workbook(false);var out=new ByteArrayOutputStream()){wb.write(out);assertThrows(ApiException.class,()->parser.parse(new ByteArrayInputStream(out.toByteArray())));}
 }
 @Test void rejectsCorruptWorkbook(){assertThrows(ApiException.class,()->parser.parse(new ByteArrayInputStream(new byte[]{1,2,3})));}
 private XSSFWorkbook workbook(boolean complete){var wb=new XSSFWorkbook();var sheet=wb.createSheet();var header=sheet.createRow(0);for(int i=0;i<(complete?HEADERS.length:HEADERS.length-1);i++)header.createCell(i).setCellValue(HEADERS[i]);if(complete){var row=sheet.createRow(1);String[] values={"Ana Pérez","20-12345678-6","Cat","IL-1","2026-01-01","2026-01-31","1234.50","0","Titular","Cat Titular","2026-01-01","2026-01-31","31","Licencia Excel"};for(int i=0;i<values.length;i++)row.createCell(i).setCellValue(values[i]);}return wb;}
}
