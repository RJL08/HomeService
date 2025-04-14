package com.example.homeservice.ui.Anuncios;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.homeservice.R;


import com.example.homeservice.model.Anuncio;
import com.example.homeservice.adapter.ImagenesAdapter;

import java.util.ArrayList;

public class DetalleAnuncioActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvTitulo, tvDescripcion, tvCiudad, tvCategoria;
    private ImageView ivMapa;


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
        ivMapa = findViewById(R.id.ivMapa);


        // Recuperamos el anuncio enviado por Intent
        Anuncio anuncio = (Anuncio) getIntent().getSerializableExtra("anuncio");

        if (anuncio != null) {
            tvTitulo.setText(anuncio.getTitulo());
            tvDescripcion.setText(anuncio.getDescripcion());
            tvCiudad.setText(anuncio.getLocalizacion());
            tvCategoria.setText(anuncio.getOficio());
            // Cargamos el mapa con la ubicación del anuncio usando LocationIQ
            String apiKey = getString(R.string.locationiq_api_key); // Asegúrate de tener esto en strings.xml
            // Construimos la URL del mapa con la API de LocationIQ y los datos del anuncio (latitud, longitud)
            String urlMapa = "https://maps.locationiq.com/v3/staticmap"
                    + "?key=" + apiKey
                    + "&center=" + anuncio.getLatitud() + "," + anuncio.getLongitud()
                    + "&zoom=15"
                    + "&size=600x300"
                    + "&format=png"
                    + "&markers=icon:large-red-cutout|" + anuncio.getLatitud() + "," + anuncio.getLongitud();

            Glide.with(this).load(urlMapa).into(ivMapa);


            // Adaptador para las imágenes (debes pasar una lista de URLs)
            ArrayList<String> urls = (ArrayList<String>) anuncio.getListaImagenes(); // Asegúrate de tener este método
            ImagenesAdapter adapter = new ImagenesAdapter(urls);
            viewPager.setAdapter(adapter);
        }
    }
}
