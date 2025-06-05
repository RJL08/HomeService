package com.example.homeservice.interfaz;

import com.example.homeservice.model.Anuncio;

/**
 * Interfaz para manejar los eventos de clic en un anuncio.
 */
public interface OnAnuncioClickListener {
    void onAnuncioClick(Anuncio anuncio);
}