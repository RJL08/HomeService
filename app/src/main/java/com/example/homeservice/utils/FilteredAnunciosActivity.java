package com.example.homeservice.utils;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.adapter.AnuncioAdapter;
import com.example.homeservice.interfaz.OnAnuncioClickListener;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.interfaz.OnFavoriteToggleListener;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.ui.Anuncios.DetalleAnuncioActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilteredAnunciosActivity extends AppCompatActivity implements OnAnuncioClickListener, OnFavoriteToggleListener {

    private RecyclerView recyclerView;
    private AnuncioAdapter adapter;
    private final List<Anuncio> listaAnuncios = new ArrayList<>();
    private String categoriaSeleccionada;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtered_anuncios);

        recyclerView = findViewById(R.id.recyclerViewFilteredAnuncios);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new AnuncioAdapter(listaAnuncios, this,this);

        recyclerView.setAdapter(adapter);

        // Recibe la categoría desde el Intent
        categoriaSeleccionada = getIntent().getStringExtra("categoria");
        if (categoriaSeleccionada == null || categoriaSeleccionada.isEmpty()) {
            Toast.makeText(this, "No se ha seleccionado una categoría", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Consulta a Firestore para obtener los anuncios de esta categoría
        new FirestoreHelper().leerAnunciosDescifrados(
                lista -> {
                    // filtrado en memoria
                    List<Anuncio> filtrados = new ArrayList<>();
                    for (Anuncio a : lista) {
                        if (categoriaSeleccionada.equals(a.getOficio())) {
                            filtrados.add(a);
                        }
                    }
                    // orden por fecha desc
                    Collections.sort(filtrados, (a1, a2) ->
                            Long.compare(a2.getFechaPublicacion(), a1.getFechaPublicacion())
                    );
                    // actualizo la vista
                    listaAnuncios.clear();
                    listaAnuncios.addAll(filtrados);
                    adapter.notifyDataSetChanged();
                },
                error -> {
                    Toast.makeText(this, "Error cargando anuncios", Toast.LENGTH_SHORT).show();
                }
        );


    }

    @Override
    public void onAnuncioClick(Anuncio anuncio) {
        // Lanza la actividad de detalles con el objeto anuncio
        Intent intent = new Intent(this, DetalleAnuncioActivity.class);
        intent.putExtra("anuncio", anuncio);
        startActivity(intent);
    }

    // --------------------------------------------------------
    // OnFavoriteToggleListener: añade/quita favoritos en Firestore
    // --------------------------------------------------------
    @Override
    public void onFavoriteAdded(Anuncio anuncio) {
        String userId    = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String anuncioId = anuncio.getId();  // Asegúrate de que Anuncio tenga getId()
        new FirestoreHelper().agregarAFavoritos(userId, anuncioId,
                aVoid -> Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show(),
                e     -> Toast.makeText(this,
                        "Error al agregar a favoritos: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

    @Override
    public void onFavoriteRemoved(Anuncio anuncio) {
        String userId    = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String anuncioId = anuncio.getId();
        new FirestoreHelper().eliminarDeFavoritos(userId, anuncioId,
                aVoid -> Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show(),
                e     -> Toast.makeText(this,
                        "Error al eliminar de favoritos: " + e.getMessage(),
                        Toast.LENGTH_SHORT
                ).show()
        );
    }

}
