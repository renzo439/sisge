package com.sisge.ms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "personas")
public class Persona {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(unique = true, length = 20) private String cuil;
    @Column(unique = true, length = 20) private String dni;
    @Column(nullable = false) private String nombres;
    @Column(nullable = false) private String apellido;
    private String sexo;
    @Column(name = "fecha_nacimiento") private LocalDate fechaNacimiento;
    public Long getId() { return id; }
    public String getCuil() { return cuil; }
    public String getDni() { return dni; }
    public String getNombres() { return nombres; }
    public String getApellido() { return apellido; }
    public String getSexo() { return sexo; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setCuil(String value) { cuil = value; }
    public void setDni(String value) { dni = value; }
    public void setNombres(String value) { nombres = value; }
    public void setApellido(String value) { apellido = value; }
    public void setSexo(String value) { sexo = value; }
    public void setFechaNacimiento(LocalDate value) { fechaNacimiento = value; }
}
