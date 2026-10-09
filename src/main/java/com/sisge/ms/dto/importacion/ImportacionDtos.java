package com.sisge.ms.dto.importacion;
import jakarta.validation.constraints.*; import java.util.*;
import com.sisge.ms.model.ImportStatus;
public final class ImportacionDtos {
 private ImportacionDtos(){}
 public record RowEdit(@Size(max=255) String agrupamiento,@Size(max=255) String funcionTitular,@Size(max=255) String tipoLicencia,@Size(max=4000) String observaciones,@Size(max=255) String lugarTrabajo){}
 public record Reason(@NotBlank(message="Reason is required") @Size(max=2000) String motivo){}
 public record Row(Long id,int filaOrigen,String reemplazante,String cuil,String categoriaReemplazante,String instrumentoLegal,
                   java.time.LocalDate periodoDesde,java.time.LocalDate periodoHasta,java.math.BigDecimal aPagar,
                   java.math.BigDecimal aDeuda,String titular,String categoriaTitular,java.time.LocalDate licenciaDesde,
                   java.time.LocalDate licenciaHasta,Integer cantidadDias,String tipoLicenciaExcel,String agrupamiento,
                   String funcionTitular,String tipoLicencia,String observaciones,String lugarTrabajo,String errores){}
 public record Detail(Long id,Long organizacionId,String archivoOrigen,ImportStatus estado,int cantidadFilas,int cantidadFilasValidas,
                      int cantidadFilasConError,String motivoObservacion,String motivoRechazo,java.time.Instant fechaCreacion,
                      java.time.Instant fechaEnvioRevision,java.time.Instant fechaRevision,java.time.Instant fechaCierre,List<Row> registros){}
 public record Summary(Long id,Long organizacionId,String organizacionNombre,String archivoOrigen,ImportStatus estado,
                       String creadoPorEmail,java.time.Instant fechaCreacion,java.time.Instant fechaEnvioRevision,
                       int cantidadFilas,int cantidadFilasConError){}
}
