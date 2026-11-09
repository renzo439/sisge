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
    @ManyToOne @JoinColumn(name = "escalafon_id") private Escalafon escalafon;
    @ManyToOne @JoinColumn(name = "categoria_id") private Categoria categoria;
    @ManyToOne @JoinColumn(name = "situacion_revista_id") private SituacionRevista situacionRevista;
    @ManyToOne @JoinColumn(name = "agrupamiento_id") private Agrupamiento agrupamiento;
    @ManyToOne @JoinColumn(name = "funcion_id") private Funcion funcion;
    @ManyToOne @JoinColumn(name = "tipo_afectacion_id") private TipoAfectacion tipoAfectacion;
    @Column(name = "fecha_inicio") private LocalDate fechaInicio;
    @Column(name = "antiguedad_anios") private Integer antiguedadAnios;
    @Column(name = "afectado", nullable = false) private boolean afectado;
    @Column(nullable = false) private boolean vigente = true;
    public Long getId() { return id; }
    public Persona getPersona() { return persona; }
    public Organizacion getOrganizacion() { return organizacion; }
    public Sector getSector() { return sector; }
    public Escalafon getEscalafon() { return escalafon; }
    public Categoria getCategoria() { return categoria; }
    public SituacionRevista getSituacionRevista() { return situacionRevista; }
    public Agrupamiento getAgrupamiento() { return agrupamiento; }
    public Funcion getFuncion() { return funcion; }
    public TipoAfectacion getTipoAfectacion() { return tipoAfectacion; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public Integer getAntiguedadAnios() { return antiguedadAnios; }
    public boolean isAfectado() { return afectado; }
    public void setPersona(Persona value) { persona = value; } public void setOrganizacion(Organizacion value) { organizacion = value; }
    public void setSector(Sector value) { sector = value; } public void setEscalafon(Escalafon value) { escalafon = value; }
    public void setCategoria(Categoria value) { categoria = value; } public void setSituacionRevista(SituacionRevista value) { situacionRevista = value; }
    public void setAgrupamiento(Agrupamiento value) { agrupamiento = value; } public void setFuncion(Funcion value) { funcion = value; }
    public void setTipoAfectacion(TipoAfectacion value) { tipoAfectacion = value; }
    public void setFechaInicio(LocalDate value) { fechaInicio = value; } public void setAntiguedadAnios(Integer value) { antiguedadAnios = value; }
    public void setAfectado(boolean value) { afectado = value; }
}
