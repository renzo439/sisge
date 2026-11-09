package com.sisge.ms.entity;
import jakarta.persistence.*;
@Entity @Table(name="adicionales") public class Adicional { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String codigo; @Column(nullable=false) private String nombre; private String descripcion; @Column(nullable=false) private boolean activo=true;
 public Long getId(){return id;} public String getCodigo(){return codigo;} public String getNombre(){return nombre;} public boolean isActivo(){return activo;} }
