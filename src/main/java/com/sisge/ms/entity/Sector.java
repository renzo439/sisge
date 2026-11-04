package com.sisge.ms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sectores", uniqueConstraints = @UniqueConstraint(columnNames = {"organizacion_id", "nombre"}))
public class Sector {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "organizacion_id") private Organizacion organizacion;
    @Column(nullable = false) private String nombre;
    public Long getId() { return id; }
    public Organizacion getOrganizacion() { return organizacion; }
    public String getNombre() { return nombre; }
    public void setOrganizacion(Organizacion value) { organizacion = value; }
    public void setNombre(String value) { nombre = value; }
}
