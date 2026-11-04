package com.sisge.ms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "importacion_personal_filas", uniqueConstraints = @UniqueConstraint(columnNames = {"importacion_id", "fila_origen"}))
public class ImportacionPersonalFila {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "importacion_id") private ImportacionPersonal importacion;
    @Column(name = "fila_origen", nullable = false) private int filaOrigen;
    @Column(name = "valores_crudos", columnDefinition = "TEXT", nullable = false) private String valoresCrudos;
    private String dni; private String cuil; private String nombres; private String apellido; private String sexo;
    @Column(name = "fecha_nacimiento") private LocalDate fechaNacimiento;
    @ManyToOne @JoinColumn(name = "organizacion_id") private Organizacion organizacion;
    @ManyToOne @JoinColumn(name = "sector_id") private Sector sector;
    private String escalafon; private String categoria; @Column(name = "situacion_revista") private String situacionRevista;
    private String agrupamiento; private String funcion; @Column(name = "fecha_inicio") private LocalDate fechaInicio;
    @Column(name = "antiguedad_anios") private Integer antiguedadAnios; private Boolean afectado;
    @Column(name = "revisada", nullable = false) private boolean revisada;
    @Column(columnDefinition = "TEXT") private String errores;
    public Long getId() { return id; } public int getFilaOrigen() { return filaOrigen; } public String getValoresCrudos() { return valoresCrudos; }
    public String getDni() { return dni; } public String getCuil() { return cuil; } public String getNombres() { return nombres; } public String getApellido() { return apellido; }
    public String getSexo() { return sexo; } public LocalDate getFechaNacimiento() { return fechaNacimiento; } public Organizacion getOrganizacion() { return organizacion; }
    public Sector getSector() { return sector; } public String getEscalafon() { return escalafon; } public String getCategoria() { return categoria; }
    public String getSituacionRevista() { return situacionRevista; } public String getAgrupamiento() { return agrupamiento; } public String getFuncion() { return funcion; }
    public LocalDate getFechaInicio() { return fechaInicio; } public Integer getAntiguedadAnios() { return antiguedadAnios; } public Boolean getAfectado() { return afectado; }
    public boolean isRevisada() { return revisada; } public String getErrores() { return errores; }
    public void setImportacion(ImportacionPersonal value) { importacion = value; } public void setFilaOrigen(int value) { filaOrigen = value; } public void setValoresCrudos(String value) { valoresCrudos = value; }
    public void setErrores(String value) { errores = value; }
    public void review(String dni, String cuil, String nombres, String apellido, String sexo, LocalDate nacimiento, Organizacion organizacion, Sector sector, String escalafon, String categoria, String situacion, String agrupamiento, String funcion, LocalDate inicio, Integer antiguedad, Boolean afectado) {
        this.dni=dni; this.cuil=cuil; this.nombres=nombres; this.apellido=apellido; this.sexo=sexo; this.fechaNacimiento=nacimiento; this.organizacion=organizacion; this.sector=sector; this.escalafon=escalafon; this.categoria=categoria; this.situacionRevista=situacion; this.agrupamiento=agrupamiento; this.funcion=funcion; this.fechaInicio=inicio; this.antiguedadAnios=antiguedad; this.afectado=afectado; this.revisada=true;
    }
}
