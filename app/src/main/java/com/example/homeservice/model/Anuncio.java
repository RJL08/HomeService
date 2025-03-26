package com.example.homeservice.model;


    /**
     * Representa los datos de un anuncio en la base de datos (Firestore).
     */
    public class Anuncio {

        private String titulo;          // Título del anuncio
        private String descripcion;     // Descripción detallada
        private String oficio;          // Tipo de servicio: "Pintor", "Electricista", etc.
        private String localizacion;    // Nombre de la ciudad o ubicación textual
        private String userId;          // ID del usuario que publicó el anuncio
        private long fechaPublicacion;  // Timestamp de cuándo se creó el anuncio

        /**
         * Constructor vacío para Firestore (des-serialización).
         */
        public Anuncio() {
        }

        /**
         * Constructor principal para inicializar todos los campos.
         */
        public Anuncio(String titulo, String descripcion, String oficio,
                       String localizacion, String userId, long fechaPublicacion) {
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.oficio = oficio;
            this.localizacion = localizacion;
            this.userId = userId;
            this.fechaPublicacion = fechaPublicacion;
        }

        // Getters y setters

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
    }


