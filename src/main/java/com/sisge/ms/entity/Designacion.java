package com.sisge.ms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "designaciones")
public class Designacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "persona_id") private Persona persona;
    @ManyToOne(optional = false) @JoinColumn(name = "organizacion_id") private Organizacion organizacion;
    @ManyToOne @JoinColumn(name = "sector_id") private Sector sector;
    private String escalafon;
    private String categoria;
    @Column(name = "situacion_revista") private String situacionRevista;
    private String agrupamiento;
    private String funcion;
    @Column(name = "fecha_inicio", nullable = false) private LocalDate fechaInicio;
    @Column(name = "antiguedad_anios") private Integer antiguedadAnios;
    @Column(name = "afectado", nullable = false) private boolean afectado;
    @Column(nullable = false) private boolean vigente = true;
    public Long getId() { return id; }
    public Persona getPersona() { return persona; }
    public Organizacion getOrganizacion() { return organizacion; }
    public Sector getSector() { return sector; }
    public String getEscalafon() { return escalafon; }
    public String getCategoria() { return categoria; }
    public String getSituacionRevista() { return situacionRevista; }
    public String getAgrupamiento() { return agrupamiento; }
    public String getFuncion() { return funcion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public Integer getAntiguedadAnios() { return antiguedadAnios; }
    public boolean isAfectado() { return afectado; }
    public void setPersona(Persona value) { persona = value; } public void setOrganizacion(Organizacion value) { organizacion = value; }
    public void setSector(Sector value) { sector = value; } public void setEscalafon(String value) { escalafon = value; }
    public void setCategoria(String value) { categoria = value; } public void setSituacionRevista(String value) { situacionRevista = value; }
    public void setAgrupamiento(String value) { agrupamiento = value; } public void setFuncion(String value) { funcion = value; }
    public void setFechaInicio(LocalDate value) { fechaInicio = value; } public void setAntiguedadAnios(Integer value) { antiguedadAnios = value; }
    public void setAfectado(boolean value) { afectado = value; }
}
