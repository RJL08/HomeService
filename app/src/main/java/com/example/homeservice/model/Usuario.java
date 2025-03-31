package com.example.homeservice.model;

/**
 * Modelo de datos para representar a un usuario en la aplicación.
 */
public class Usuario {

    private String id;               // ID único del usuario (UID de Firebase)
    private String nombre;          // Nombre del usuario
    private String apellidos;       // Apellidos del usuario
    private String correo;          // Correo electrónico
    private String localizacion;    // Ciudad o ubicación del usuario
    private String fotoPerfil;      // URL o ruta de la imagen de perfil

    /**
     * Constructor vacío requerido por Firestore.
     */
    public Usuario() {
    }

    /**
     * Constructor completo para crear un objeto Usuario.
     *
     * @param id           UID proporcionado por Firebase Authentication.
     * @param nombre       Nombre del usuario.
     * @param apellidos    Apellidos del usuario.
     * @param correo       Correo electrónico del usuario.
     * @param localizacion Ciudad o ubicación del usuario.
     * @param fotoPerfil   Ruta o URL de la imagen de perfil.
     */
    public Usuario(String id, String nombre, String apellidos, String correo, String localizacion, String fotoPerfil) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.correo = correo;
        this.localizacion = localizacion;
        this.fotoPerfil = fotoPerfil;
    }

    // Métodos getter y setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getLocalizacion() {
        return localizacion;
    }

    public void setLocalizacion(String localizacion) {
        this.localizacion = localizacion;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}
