package com.sisge.ms.dto.reemplazante;
import com.sisge.ms.entity.ReemplazanteHistorico;
import java.math.BigDecimal;
import java.time.*;
public final class ReemplazanteDtos {
 private ReemplazanteDtos(){}
 public record View(Long id,Long organizacionId,String hospitalNombre,String udeo,Long importacionId,String archivoOrigen,int filaOrigen,String reemplazante,String cuil,String dni,
  String categoriaReemplazante,String instrumentoLegal,LocalDate periodoDesde,LocalDate periodoHasta,BigDecimal aPagar,
  BigDecimal aDeuda,String titular,String categoriaTitular,LocalDate licenciaDesde,LocalDate licenciaHasta,Integer cantidadDias,
  String tipoLicenciaExcel,String agrupamiento,String funcionTitular,String tipoLicencia,String observaciones,String lugarTrabajo,Instant fechaCierre){}
 public static View from(ReemplazanteHistorico h){return new View(h.getId(),h.getOrganizacion()==null?null:h.getOrganizacion().getId(),h.getHospitalNombre(),h.getUdeo(),h.getImportacion().getId(),h.getArchivoOrigen(),h.getFilaOrigen(),h.getReemplazante(),h.getCuil(),h.getDni(),h.getCategoriaReemplazante(),h.getInstrumentoLegal(),h.getPeriodoDesde(),h.getPeriodoHasta(),h.getAPagar(),h.getADeuda(),h.getTitular(),h.getCategoriaTitular(),h.getLicenciaDesde(),h.getLicenciaHasta(),h.getCantidadDias(),h.getTipoLicenciaExcel(),h.getAgrupamiento(),h.getFuncionTitular(),h.getTipoLicencia(),h.getObservaciones(),h.getLugarTrabajo(),h.getFechaCierre());}
}
