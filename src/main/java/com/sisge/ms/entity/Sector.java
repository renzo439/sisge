package com.sisge.ms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sectores", uniqueConstraints = @UniqueConstraint(columnNames = {"organizacion_id", "nombre"}))
public class Sector {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "organizacion_id") private Organizacion organizacion;
    @ManyToOne @JoinColumn(name = "dependencia_id") private Dependencia dependencia;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private boolean activo = true;
    public Long getId() { return id; }
    public Organizacion getOrganizacion() { return organizacion; }
    public Dependencia getDependencia() { return dependencia; }
    public String getNombre() { return nombre; }
    public void setOrganizacion(Organizacion value) { organizacion = value; }
    public void setDependencia(Dependencia value) { dependencia = value; }
    public void setNombre(String value) { nombre = value; }
}
