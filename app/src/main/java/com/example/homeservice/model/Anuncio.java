package com.example.homeservice.model;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
     * Representa los datos de un anuncio en la base de datos (Firestore).
     */

public class Anuncio implements Serializable {

    private String id;
    private String titulo;          // Título del anuncio
    private String descripcion;     // Descripción detallada
    private String oficio;          // Tipo de servicio: "Pintor", "Electricista", etc.
    private String localizacion;    // Nombre de la ciudad o ubicación textual
    private String userId;          // ID del usuario que publicó el anuncio
    private long fechaPublicacion;  // Timestamp de cuándo se creó el anuncio
    private double latitud;         // Latitud para la ubicación exacta
    private double longitud;        // Longitud para la ubicación exacta
    private ArrayList<String> listaImagenes = new ArrayList<>();
    private boolean favorite = false;
    // campo para calcular distancia entre dos anuncios
    private transient double distanceKm = Double.MAX_VALUE;

    /**
     * Constructor vacío para Firestore (des-serialización).
     */
    public Anuncio() {
    }

    /**
     * Constructor principal para inicializar todos los campos.
     */

    public Anuncio(String titulo, String descripcion, String oficio,
                   String ciudad, String uid, long fechaPublicacion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.oficio = oficio;
        this.localizacion = ciudad;
        this.userId = uid;
        this.fechaPublicacion = fechaPublicacion;
        this.listaImagenes = new ArrayList<>();
        this.favorite = false;
    }

    // Getters y setters

    public double getDistanceKm() {
        return distanceKm;
    }
    public void setDistanceKm(double d) {
        distanceKm = d;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public ArrayList<String> getListaImagenes() {
        return listaImagenes;
    }

    public void setListaImagenes(List<String> listaImagenes) {
        this.listaImagenes = (ArrayList<String>) listaImagenes;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getOficio() {
        return oficio;
    }

    public void setOficio(String oficio) {
        this.oficio = oficio;
    }

    public String getLocalizacion() {
        return localizacion;
    }

    public void setLocalizacion(String localizacion) {
        this.localizacion = localizacion;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(long fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }
}
