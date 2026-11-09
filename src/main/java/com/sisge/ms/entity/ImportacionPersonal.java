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
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PersonalImportStatus estado = PersonalImportStatus.PENDIENTE;
    @Column(name = "fecha_creacion", nullable = false) private Instant fechaCreacion = Instant.now();
    @Column(name = "fecha_aprobacion") private Instant fechaAprobacion;
    @Column(name="fecha_revision") private Instant fechaRevision;
    @Column(name="cantidad_filas", nullable=false) private int cantidadFilas;
    @Column(name="cantidad_filas_validas", nullable=false) private int cantidadFilasValidas;
    @Column(name="cantidad_filas_con_error", nullable=false) private int cantidadFilasConError;
    public Long getId() { return id; } public Organizacion getOrganizacion() { return organizacion; }
    public Usuario getCreadoPor() { return creadoPor; } public String getArchivoOrigen() { return archivoOrigen; }
    public PersonalImportStatus getEstado() { return estado; } public Instant getFechaCreacion() { return fechaCreacion; }
    public void setOrganizacion(Organizacion value) { organizacion = value; } public void setCreadoPor(Usuario value) { creadoPor = value; }
    public int getCantidadFilas(){return cantidadFilas;} public int getCantidadFilasValidas(){return cantidadFilasValidas;} public int getCantidadFilasConError(){return cantidadFilasConError;}
    public void setArchivoOrigen(String value) { archivoOrigen = value; } public void setCounts(int total,int valid,int errors){cantidadFilas=total;cantidadFilasValidas=valid;cantidadFilasConError=errors;}
    public void startReview(){estado=PersonalImportStatus.EN_RELEVAMIENTO; fechaRevision=Instant.now();} public void approve() { estado = PersonalImportStatus.APROBADO; fechaAprobacion = Instant.now(); } public void markConsolidated(){estado=PersonalImportStatus.CONSOLIDADO;}
}
