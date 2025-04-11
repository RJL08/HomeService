package com.example.homeservice.ui.Anuncios;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.homeservice.R;


import com.example.homeservice.model.Anuncio;
import com.example.homeservice.adapter.ImagenesAdapter;

import java.util.ArrayList;

public class DetalleAnuncioActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvTitulo, tvDescripcion, tvCiudad, tvCategoria;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_anuncio);

        // Enlazamos vistas
        viewPager = findViewById(R.id.viewPagerImagenes);
        tvTitulo = findViewById(R.id.tvTituloDetalle);
        tvDescripcion = findViewById(R.id.tvDescripcionDetalle);
        tvCiudad = findViewById(R.id.tvCiudadDetalle);
        tvCategoria = findViewById(R.id.tvOficioDetalle);


        // Recuperamos el anuncio enviado por Intent
        Anuncio anuncio = (Anuncio) getIntent().getSerializableExtra("anuncio");

        if (anuncio != null) {
            tvTitulo.setText(anuncio.getTitulo());
            tvDescripcion.setText(anuncio.getDescripcion());
            tvCiudad.setText(anuncio.getLocalizacion());
            tvCategoria.setText(anuncio.getOficio());

            // Adaptador para las imágenes (debes pasar una lista de URLs)
            ArrayList<String> urls = (ArrayList<String>) anuncio.getListaImagenes(); // Asegúrate de tener este método
            ImagenesAdapter adapter = new ImagenesAdapter(urls);
            viewPager.setAdapter(adapter);
        }
    }
}
