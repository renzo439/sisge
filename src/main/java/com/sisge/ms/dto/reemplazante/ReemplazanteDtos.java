package com.sisge.ms.dto.reemplazante;
import com.sisge.ms.entity.ReemplazanteHistorico;
import java.time.*;
public final class ReemplazanteDtos {
 private ReemplazanteDtos(){}
 public record View(Long id,Long organizacionId,String organizacionNombre,Long importacionId,String archivoOrigen,int filaOrigen,
  String reemplazante,String cuil,String categoriaReemplazante,String instrumentoLegal,LocalDate periodoDesde,LocalDate periodoHasta,
  String titular,String categoriaTitular,LocalDate licenciaDesde,LocalDate licenciaHasta,Integer cantidadDias,
  String tipoLicenciaExcel,String agrupamiento,String funcionTitular,String tipoLicencia,String observaciones,String lugarTrabajo){}
 public static View from(ReemplazanteHistorico h){return new View(h.getId(),h.getOrganizacion().getId(),h.getOrganizacion().getNombre(),h.getImportacion().getId(),h.getArchivoOrigen(),h.getFilaOrigen(),h.getReemplazante(),h.getCuil(),h.getCategoriaReemplazante(),h.getInstrumentoLegal(),h.getPeriodoDesde(),h.getPeriodoHasta(),h.getTitular(),h.getCategoriaTitular(),h.getLicenciaDesde(),h.getLicenciaHasta(),h.getCantidadDias(),h.getTipoLicenciaExcel(),h.getAgrupamiento(),h.getFuncionTitular(),h.getTipoLicencia(),h.getObservaciones(),h.getLugarTrabajo());}
}
