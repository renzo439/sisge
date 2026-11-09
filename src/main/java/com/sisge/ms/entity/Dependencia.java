package com.sisge.ms.entity;
import jakarta.persistence.*;
@Entity @Table(name="dependencias", uniqueConstraints=@UniqueConstraint(columnNames={"organizacion_id","nombre"}))
public class Dependencia { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(optional=false) @JoinColumn(name="organizacion_id") private Organizacion organizacion; @Column(nullable=false) private String nombre; @Column(nullable=false) private boolean activo=true;
 public Long getId(){return id;} public Organizacion getOrganizacion(){return organizacion;} public String getNombre(){return nombre;} public boolean isActivo(){return activo;} public void setOrganizacion(Organizacion v){organizacion=v;} public void setNombre(String v){nombre=v;} public void setActivo(boolean v){activo=v;} }
