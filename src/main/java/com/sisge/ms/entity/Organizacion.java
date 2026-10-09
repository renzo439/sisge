package com.sisge.ms.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="organizaciones") public class Organizacion {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false) private String nombre;
 @Column(columnDefinition="TEXT") private String descripcion; @Column(name="apikey_hash") private String apiKeyHash;
 @Column(name="created_at",nullable=false) private Instant createdAt=Instant.now(); @Column(name="updated_at",nullable=false) private Instant updatedAt=Instant.now();
 public Long getId(){return id;} public String getNombre(){return nombre;} public void setNombre(String n){nombre=n;} public String getDescripcion(){return descripcion;} public void setDescripcion(String d){descripcion=d;}
}
