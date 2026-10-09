package com.sisge.ms.entity; import jakarta.persistence.*;
@Entity @Table(name="permisos") public class Permission { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false,unique=true) private String codigo; private String descripcion; public String getCodigo(){return codigo;} }
