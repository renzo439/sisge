package com.sisge.ms.service;
import com.sisge.ms.entity.*; import com.sisge.ms.repository.*; import com.sisge.ms.security.CurrentUser; import com.sisge.ms.security.UserAuthenticationService; import org.springframework.stereotype.Service;
@Service public class AuditoriaService {
 private final AuditoriaEventoRepository events; private final AuditoriaCambioRepository changes; private final UserAuthenticationService users;
 public AuditoriaService(AuditoriaEventoRepository events,AuditoriaCambioRepository changes,UserAuthenticationService users){this.events=events;this.changes=changes;this.users=users;}
 public void record(Organizacion org,String action,String entity,Long id){ AuditoriaEvento event=new AuditoriaEvento(); event.setUsuario(users.entity(CurrentUser.get().email())); event.setOrganizacion(org); event.setAccion(action); event.setEntidad(entity); event.setEntidadId(id); events.save(event); }
 public void recordChange(Organizacion org,String action,String entity,Long id,String oldValue,String newValue){ AuditoriaEvento event=new AuditoriaEvento(); event.setUsuario(users.entity(CurrentUser.get().email())); event.setOrganizacion(org); event.setAccion(action); event.setEntidad(entity); event.setEntidadId(id); events.save(event); AuditoriaCambio change=new AuditoriaCambio(); change.setEvento(event); change.setValorAnterior(oldValue); change.setValorNuevo(newValue); changes.save(change); }
}
