package com.example.homeservice.ui.Anuncios;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.example.homeservice.R;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.adapter.ImagenesAdapter;
import com.example.homeservice.ui.Publicar.PublicarAnuncio;
import com.example.homeservice.ui.chat.ChatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class DetalleAnuncioActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvTitulo, tvDescripcion, tvCiudad, tvCategoria;
    private ImageView ivMapa;
    private Anuncio anuncio;
    private ActivityResultLauncher<Intent> editLauncher;


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
        MaterialButton btnCompartir = findViewById(R.id.btnAccionCompartir);


        // 1) Lee tu Toolbar y conviértelo en action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // 2) Ponle título y habilita la flecha “up”
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Detalle Anuncio");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Título blanco
        toolbar.setTitleTextColor(Color.WHITE);

// Flecha blanca
        Drawable nav = toolbar.getNavigationIcon();
        if (nav != null) {
            nav.setTint(Color.WHITE);
        }

        // Recuperamos el anuncio enviado por Intent
        AtomicReference<Anuncio> anuncio = new AtomicReference<>((Anuncio) getIntent().getSerializableExtra("anuncio"));
        // Si no hay anuncio, no hacemos nada
        if (anuncio.get() != null) {
            tvTitulo.setText(anuncio.get().getTitulo());
            tvDescripcion.setText(anuncio.get().getDescripcion());
            tvCiudad.setText(anuncio.get().getLocalizacion());
            tvCategoria.setText(anuncio.get().getOficio());
            // Cargamos el mapa con la ubicación del anuncio usando LocationIQ
            String apiKey = getString(R.string.locationiq_api_key); // Asegúrate de tener esto en strings.xml
            // Construimos la URL del mapa con la API de LocationIQ y los datos del anuncio (latitud, longitud)
            String urlMapa = "https://maps.locationiq.com/v3/staticmap"
                    + "?key=" + apiKey
                    + "&center=" + anuncio.get().getLatitud() + "," + anuncio.get().getLongitud()
                    + "&zoom=15"
                    + "&size=600x300"
                    + "&format=png"
                    + "&markers=icon:large-red-cutout|" + anuncio.get().getLatitud() + "," + anuncio.get().getLongitud();

            Glide.with(this).load(urlMapa).into(ivMapa);


            // Adaptador para las imágenes (debes pasar una lista de URLs)
            ArrayList<String> urls = (ArrayList<String>) anuncio.get().getListaImagenes(); // Asegúrate de tener este método
            ImagenesAdapter adapter = new ImagenesAdapter(urls);
            viewPager.setAdapter(adapter);

        }

        // Después de cargar los datos del anuncio en DetalleAnuncioActivity:
        Button btnAccion = findViewById(R.id.btnAccionChat);
        btnAccion.setText("CHAT");
        btnAccion.setOnClickListener(v -> {
            String publisherId    = anuncio.get().getUserId();
            String currentUserId  = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String serviceTitle   = anuncio.get().getTitulo();
            String adId           = anuncio.get().getId();

            FirestoreHelper helper = new FirestoreHelper();
            helper.getOrCreateConversation(
                    currentUserId,
                    publisherId,
                    serviceTitle,
                    adId,
                    conversationId -> {
                        Intent i = new Intent(this, ChatActivity.class);
                        i.putExtra("conversationId", conversationId);
                        startActivity(i);
                    },
                    error -> Toast.makeText(
                            this,
                            "Error al iniciar el chat: " + error.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show()
            );
        });

        // Compartir texto plano
        btnCompartir.setOnClickListener(v -> {
            if (anuncio.get() != null) {
                String texto = "Título: " + anuncio.get().getTitulo() + "\n"
                        + "Descripción: " + anuncio.get().getDescripcion() + "\n"
                        + "Localización: " + anuncio.get().getLocalizacion();
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("text/plain");
                share.putExtra(Intent.EXTRA_SUBJECT, "Anuncio HomeService");
                share.putExtra(Intent.EXTRA_TEXT, texto);
                startActivity(Intent.createChooser(share, "Compartir anuncio via"));
            }
        });

        // ——— REGISTRA EL LAUNCHER ———
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Anuncio updated = (Anuncio) result.getData()
                                .getSerializableExtra("anuncio_actualizado");
                        if (updated != null) {
                            // Sustituye tu variable local y vuelve a pintar la UI:
                            anuncio.set(updated);
                            tvTitulo    .setText(updated.getTitulo());
                            tvDescripcion.setText(updated.getDescripcion());
                            tvCiudad    .setText(updated.getLocalizacion());
                            tvCategoria .setText(updated.getOficio());
                            // carrusel:
                            viewPager.setAdapter(
                                    new ImagenesAdapter(new ArrayList<>(updated.getListaImagenes()))
                            );
                        }
                    }
                }
        );

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();  // cierra esta actividad
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detalle_anuncio, menu);
        FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
        Anuncio anuncio = (Anuncio) getIntent().getSerializableExtra("anuncio");
        MenuItem edit = menu.findItem(R.id.action_edit);
        if (me == null || anuncio == null || !me.getUid().equals(anuncio.getUserId())) {
            edit.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            Anuncio anuncio = (Anuncio) getIntent().getSerializableExtra("anuncio");
            Intent i = new Intent(this, PublicarAnuncio.class);
            i.putExtra("modo_editar", true);
            i.putExtra("anuncio", anuncio);
            editLauncher.launch(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
