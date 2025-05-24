package com.example.homeservice.ui.Anuncios;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
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
    private TextView tvTitulo, tvDescripcion, tvCiudad, tvCategoria, tvUserName;
    private ImageView ivMapa, ivUserPhoto;
    private Anuncio anuncio;
    private ActivityResultLauncher<Intent> editLauncher;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detalle_anuncio);

        // ——— 1) Enlazar todas las vistas ———
        viewPager     = findViewById(R.id.viewPagerImagenes);
        ivUserPhoto   = findViewById(R.id.ivUserPhoto);
        tvUserName    = findViewById(R.id.tvUserName);
        tvTitulo      = findViewById(R.id.tvTituloDetalle);
        tvDescripcion = findViewById(R.id.tvDescripcionDetalle);
        tvCiudad      = findViewById(R.id.tvCiudadDetalle);
        tvCategoria   = findViewById(R.id.tvOficioDetalle);
        ivMapa        = findViewById(R.id.ivMapa);
        MaterialButton btnCompartir = findViewById(R.id.btnAccionCompartir);
        Button btnChat             = findViewById(R.id.btnAccionChat);

        // ——— 2) Toolbar como ActionBar ———
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Detalle Anuncio");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setTitleTextColor(Color.WHITE);
        Drawable nav = toolbar.getNavigationIcon();
        if (nav != null) nav.setTint(Color.WHITE);

        // ——— 3) Recuperar el Anuncio del Intent ———
        anuncio = (Anuncio) getIntent().getSerializableExtra("anuncio");
        if (anuncio == null) {
            Toast.makeText(this, "No se ha recibido el anuncio", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ——— 4) Rellenar datos fijos del anuncio ———
        tvTitulo     .setText(anuncio.getTitulo());
        tvDescripcion.setText(anuncio.getDescripcion());
        tvCiudad     .setText(anuncio.getLocalizacion());
        tvCategoria  .setText(anuncio.getOficio());

        // Mapa estático LocationIQ
        String apiKey = getString(R.string.locationiq_api_key);
        String urlMapa = "https://maps.locationiq.com/v3/staticmap"
                + "?key=" + apiKey
                + "&center=" + anuncio.getLatitud() + "," + anuncio.getLongitud()
                + "&zoom=15"
                + "&size=600x300"
                + "&format=png"
                + "&markers=icon:large-red-cutout|"
                + anuncio.getLatitud() + "," + anuncio.getLongitud();
        Glide.with(this).load(urlMapa).into(ivMapa);

        // Carrusel de imágenes
        ArrayList<String> urls = new ArrayList<>(anuncio.getListaImagenes());
        ImagenesAdapter adapter = new ImagenesAdapter(urls);
        viewPager.setAdapter(adapter);

        // ——— 5) Cargar NOMBRE y FOTO del publicador ———
        String publisId = anuncio.getUserId();
        if (publisId != null) {
            new FirestoreHelper().leerUsuarioDescifrado(
                    publisId,
                    usuario -> {
                        if (usuario != null) {
                            tvUserName.setText(usuario.getNombre());
                            Glide.with(this)
                                    .load(usuario.getFotoPerfil())
                                    .placeholder(R.drawable.nophoto)
                                    .circleCrop()
                                    .into(ivUserPhoto);
                        }
                    },
                    error -> Log.w("DetalleAnuncio", "no pude cargar usuario", error)
            );
        }

        // ——— 6) Botón CHAT ———
        btnChat.setText("CHAT");
        btnChat.setOnClickListener(v -> {
            String publisherId   = anuncio.getUserId();
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String serviceTitle  = anuncio.getTitulo();
            String adId          = anuncio.getId();
            new FirestoreHelper().getOrCreateConversation(
                    currentUserId, publisherId, serviceTitle, adId,
                    conversationId -> {
                        Intent i = new Intent(this, ChatActivity.class);
                        i.putExtra("conversationId", conversationId);
                        startActivity(i);
                    },
                    err -> Toast.makeText(
                            this,
                            "Error al iniciar el chat: " + err.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show()
            );
        });

        // ——— 7) Botón COMPARTIR ———
        btnCompartir.setOnClickListener(v -> {
            String texto = "Título: " + anuncio.getTitulo() + "\n"
                    + "Descripción: " + anuncio.getDescripcion() + "\n"
                    + "Localización: " + anuncio.getLocalizacion();
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, "Anuncio HomeService");
            share.putExtra(Intent.EXTRA_TEXT, texto);
            startActivity(Intent.createChooser(share, "Compartir anuncio via"));
        });

        // ——— 8) Launcher para editar (si es tu anuncio) ———
        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Anuncio updated = (Anuncio) result.getData()
                                .getSerializableExtra("anuncio_actualizado");
                        if (updated != null) {
                            anuncio = updated;
                            tvTitulo     .setText(updated.getTitulo());
                            tvDescripcion.setText(updated.getDescripcion());
                            tvCiudad     .setText(updated.getLocalizacion());
                            tvCategoria  .setText(updated.getOficio());
                            viewPager.setAdapter(
                                    new ImagenesAdapter(
                                            new ArrayList<>(updated.getListaImagenes())
                                    )
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
