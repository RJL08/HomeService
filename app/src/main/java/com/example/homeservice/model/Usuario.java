package com.example.homeservice.model;

/**
 * Representa los datos de un usuario en la base de datos (Firestore).
 * Usado para guardar información adicional de cada cuenta.
 */
public class Usuario {

    private String nombre;
    private String apellidos;
    private String correo;
    private String telefono;
    private String localizacion; // Nombre de la ciudad o ubicación textual

    /**
     * Constructor vacío necesario para Firestore (des-serialización).
     */
    public Usuario() {
    }

    /**
     * Constructor principal para inicializar todos los campos del usuario.
     * @param nombre Nombre del usuario
     * @param apellidos Apellidos del usuario
     * @param correo Correo electrónico
     * @param telefono Número de teléfono
     * @param localizacion Nombre de la ciudad o ubicación
     */
    public Usuario(String nombre, String apellidos, String correo,
                   String telefono, String localizacion) {
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.correo = correo;
        this.telefono = telefono;
        this.localizacion = localizacion;
    }

    // Getters y Setters

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getLocalizacion() {
        return localizacion;
    }

    public void setLocalizacion(String localizacion) {
        this.localizacion = localizacion;
    }
}
