package com.sisge.ms.dto.personal;
public final class PersonalAdminDtos {
 private PersonalAdminDtos(){}
 public record Validation(Long importacionId,int personasNuevas,int personasExistentes,int designacionesNuevas,int designacionesExistentes,int maestrosInexistentes,int filasInvalidas,int adicionalesInvalidos,int duplicadosPotenciales){}
 public record Consolidation(Long importacionId,int personasCreadas,int personasActualizadas,int designacionesCreadas,int designacionesActualizadas,int adicionalesCreados,int errores){}
}
