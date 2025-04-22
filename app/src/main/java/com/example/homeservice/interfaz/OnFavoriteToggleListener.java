package com.example.homeservice.interfaz;

import com.example.homeservice.model.Anuncio;

/**
 * interfaz para el boton de favorito, de tal manera que podamos añadir o eliminar de favoritos
 */
public interface OnFavoriteToggleListener {
    void onFavoriteAdded(Anuncio anuncio);
    void onFavoriteRemoved(Anuncio anuncio);
}