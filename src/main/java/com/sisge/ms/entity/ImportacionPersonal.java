package com.sisge.ms.entity;

import com.sisge.ms.model.PersonalImportStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "importaciones_personal")
public class ImportacionPersonal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "organizacion_id") private Organizacion organizacion;
    @ManyToOne(optional = false) @JoinColumn(name = "creado_por_usuario_id") private Usuario creadoPor;
    @Column(name = "archivo_origen", nullable = false) private String archivoOrigen;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PersonalImportStatus estado = PersonalImportStatus.EN_REVISION;
    @Column(name = "fecha_creacion", nullable = false) private Instant fechaCreacion = Instant.now();
    @Column(name = "fecha_aprobacion") private Instant fechaAprobacion;
    public Long getId() { return id; } public Organizacion getOrganizacion() { return organizacion; }
    public Usuario getCreadoPor() { return creadoPor; } public String getArchivoOrigen() { return archivoOrigen; }
    public PersonalImportStatus getEstado() { return estado; } public Instant getFechaCreacion() { return fechaCreacion; }
    public void setOrganizacion(Organizacion value) { organizacion = value; } public void setCreadoPor(Usuario value) { creadoPor = value; }
    public void setArchivoOrigen(String value) { archivoOrigen = value; } public void approve() { estado = PersonalImportStatus.APROBADA; fechaAprobacion = Instant.now(); }
}
