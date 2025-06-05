package com.example.homeservice.interfaz;

import com.example.homeservice.model.Anuncio;

/**
 * Interfaz para manejar los eventos de clic largo en un anuncio.
 */
public interface OnAnuncioLongClickListener {
    void onAnuncioLongClick(Anuncio anuncio);
}