package com.sisge.ms.entity; import jakarta.persistence.*;
@Entity @Table(name="permisos") public class Permission { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String codigo; private String descripcion;
 public Long getId(){return id;} public String getCodigo(){return codigo;} public void setCodigo(String c){codigo=c;} public String getDescripcion(){return descripcion;} public void setDescripcion(String d){descripcion=d;} }
