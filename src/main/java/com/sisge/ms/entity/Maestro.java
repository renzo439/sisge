package com.sisge.ms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class Maestro {
    @jakarta.persistence.Id @jakarta.persistence.GeneratedValue(strategy=jakarta.persistence.GenerationType.IDENTITY) private Long id;
    @Column(length=100) private String codigo;
    @Column(nullable=false, length=255) private String nombre;
    @Column(nullable=false) private boolean activo=true;
    public Long getId(){ return id; } public String getCodigo(){ return codigo; } public String getNombre(){ return nombre; }
    public boolean isActivo(){ return activo; } public void setCodigo(String value){ codigo=value; } public void setNombre(String value){ nombre=value; }
    public void setActivo(boolean value){ activo=value; }
}
