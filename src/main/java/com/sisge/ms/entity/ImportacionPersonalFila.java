package com.sisge.ms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "importacion_personal_filas", uniqueConstraints = @UniqueConstraint(columnNames = {"importacion_id", "fila_origen"}))
public class ImportacionPersonalFila {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "importacion_id") private ImportacionPersonal importacion;
    @Column(name = "fila_origen", nullable = false) private int filaOrigen;
    @Column(name = "valores_crudos", columnDefinition = "TEXT", nullable = false) private String valoresCrudos;
    private String dni; private String cuil; private String nombres; private String apellido; private String sexo;
    @Column(name = "fecha_nacimiento") private LocalDate fechaNacimiento;
    @ManyToOne @JoinColumn(name = "organizacion_id") private Organizacion organizacion;
    @ManyToOne @JoinColumn(name = "dependencia_id") private Dependencia dependencia;
    @ManyToOne @JoinColumn(name = "sector_id") private Sector sector;
    @ManyToOne @JoinColumn(name="escalafon_id") private Escalafon escalafon;
    @ManyToOne @JoinColumn(name="categoria_id") private Categoria categoria;
    @ManyToOne @JoinColumn(name="situacion_revista_id") private SituacionRevista situacionRevista;
    @ManyToOne @JoinColumn(name="agrupamiento_id") private Agrupamiento agrupamiento;
    @ManyToOne @JoinColumn(name="funcion_id") private Funcion funcion;
    @ManyToOne @JoinColumn(name="tipo_afectacion_id") private TipoAfectacion tipoAfectacion;
    @ManyToOne @JoinColumn(name="titulo_id") private Titulo titulo;
    @Column(name="adicional_mh") private String adicionalMh;
    @Column(name="adicional_jefatura") private String adicionalJefatura;
    @Column(name="estado_relevamiento", nullable=false) private String estadoRelevamiento="PENDIENTE";
    @Column(name = "fecha_inicio") private LocalDate fechaInicio;
    @Column(name = "antiguedad_anios") private Integer antiguedadAnios; private Boolean afectado;
    @Column(name = "revisada", nullable = false) private boolean revisada;
    @Column(columnDefinition = "TEXT") private String errores;
    public Long getId() { return id; } public int getFilaOrigen() { return filaOrigen; } public String getValoresCrudos() { return valoresCrudos; }
    public String getDni() { return dni; } public String getCuil() { return cuil; } public String getNombres() { return nombres; } public String getApellido() { return apellido; }
    public String getSexo() { return sexo; } public LocalDate getFechaNacimiento() { return fechaNacimiento; } public Organizacion getOrganizacion() { return organizacion; }
    public Dependencia getDependencia(){return dependencia;} public Sector getSector() { return sector; } public Escalafon getEscalafon() { return escalafon; } public Categoria getCategoria() { return categoria; }
    public SituacionRevista getSituacionRevista() { return situacionRevista; } public Agrupamiento getAgrupamiento() { return agrupamiento; } public Funcion getFuncion() { return funcion; }
    public TipoAfectacion getTipoAfectacion(){return tipoAfectacion;} public Titulo getTitulo(){return titulo;} public String getAdicionalMh(){return adicionalMh;} public String getAdicionalJefatura(){return adicionalJefatura;}
    public LocalDate getFechaInicio() { return fechaInicio; } public Integer getAntiguedadAnios() { return antiguedadAnios; } public Boolean getAfectado() { return afectado; }
    public boolean isRevisada() { return revisada; } public String getErrores() { return errores; }
    public void setImportacion(ImportacionPersonal value) { importacion = value; } public void setFilaOrigen(int value) { filaOrigen = value; } public void setValoresCrudos(String value) { valoresCrudos = value; }
    public void setErrores(String value) { errores = value; }
    public void review(String dni, String cuil, String nombres, String apellido, String sexo, LocalDate nacimiento, Organizacion organizacion, Dependencia dependencia, Sector sector, Escalafon escalafon, Categoria categoria, SituacionRevista situacion, Agrupamiento agrupamiento, Funcion funcion, TipoAfectacion tipoAfectacion, Titulo titulo, LocalDate inicio, Integer antiguedad, Boolean afectado) {
        this.dni=dni; this.cuil=cuil; this.nombres=nombres; this.apellido=apellido; this.sexo=sexo; this.fechaNacimiento=nacimiento; this.organizacion=organizacion; this.dependencia=dependencia; this.sector=sector; this.escalafon=escalafon; this.categoria=categoria; this.situacionRevista=situacion; this.agrupamiento=agrupamiento; this.funcion=funcion; this.tipoAfectacion=tipoAfectacion; this.titulo=titulo; this.fechaInicio=inicio; this.antiguedadAnios=antiguedad; this.afectado=afectado; this.revisada=true; this.estadoRelevamiento="COMPLETADO";
    }
    /** Compatibility helper for previously staged text rows; new code always supplies master IDs. */
    public void review(String dni,String cuil,String nombres,String apellido,String sexo,LocalDate nacimiento,Organizacion organizacion,Sector sector,String ignoredEscalafon,String ignoredCategoria,String ignoredSituacion,String ignoredAgrupamiento,String ignoredFuncion,LocalDate inicio,Integer antiguedad,Boolean afectado) {
        review(dni,cuil,nombres,apellido,sexo,nacimiento,organizacion,null,sector,null,null,null,null,null,null,null,inicio,antiguedad,afectado);
    }
    public void setIdentity(String dni,String cuil,String nombres,String apellido,String sexo,LocalDate nacimiento){this.dni=dni;this.cuil=cuil;this.nombres=nombres;this.apellido=apellido;this.sexo=sexo;this.fechaNacimiento=nacimiento;}
    public void setWorkValues(LocalDate inicio,Integer antiguedad,Boolean afectado,String mh,String jefatura){fechaInicio=inicio;antiguedadAnios=antiguedad;this.afectado=afectado;adicionalMh=mh;adicionalJefatura=jefatura;}
    public void setResolved(Organizacion org, Dependencia dep, Sector sec, Escalafon esc, Categoria cat, SituacionRevista sit, Agrupamiento agr, Funcion fun, TipoAfectacion tipo, Titulo title){organizacion=org;dependencia=dep;sector=sec;escalafon=esc;categoria=cat;situacionRevista=sit;agrupamiento=agr;funcion=fun;tipoAfectacion=tipo;titulo=title;}
}
