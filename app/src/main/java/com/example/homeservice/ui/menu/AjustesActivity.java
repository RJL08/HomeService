package com.example.homeservice.ui.menu;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.homeservice.R;

/**
 * clase que se crea para poder acceder a las preferencias del usuario desde el menú y asi poder modificar el modo oscuro,
 * tambien modificar el tema de notificaciones y sonidos
 */
public class AjustesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.contenedor_ajustes, new AjustesFragment())
                .commit();
    }
}
