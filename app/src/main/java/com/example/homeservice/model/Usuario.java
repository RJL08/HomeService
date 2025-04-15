package com.example.homeservice.model;



/**
 * Representa los datos de un usuario en la base de datos de Firestore.
 */
public class Usuario {

    private String id;             // UID proporcionado por Firebase
    private String nombre;         // Nombre del usuario
    private String apellidos;      // Apellidos del usuario
    private String correo;         // Correo electrónico
    private String localizacion;   // Ciudad o ubicación textual
    private String fotoPerfil;     // URL o ruta de la imagen de perfil

    // Campos para coordenadas
    private Double lat;            // Latitud
    private Double lon;            // Longitud

    /**
     * Constructor vacío requerido por Firestore (des-serialización).
     */
    public Usuario() {
    }

    /**
     * Constructor principal para crear un Usuario sin lat/lon específicos.
     *
     * @param id            UID de Firebase
     * @param nombre        Nombre
     * @param apellidos     Apellidos
     * @param correo        Correo
     * @param localizacion  Nombre de la ciudad o ubicación textual
     * @param fotoPerfil    URL de la foto de perfil
     */
    public Usuario(String id,
                   String nombre,
                   String apellidos,
                   String correo,
                   String localizacion,
                   String fotoPerfil,
                   Double lat,
                   Double lon) {
        this.id = id;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.correo = correo;
        this.localizacion = localizacion;
        this.fotoPerfil = fotoPerfil;
        this.lat = lat;
        this.lon = lon;
    }


    // Getters y setters de todos los campos:

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

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }
}
