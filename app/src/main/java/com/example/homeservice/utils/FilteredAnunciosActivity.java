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
import com.example.homeservice.adapter.OnAnuncioClickListener;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.ui.Anuncios.DetalleAnuncioActivity;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class FilteredAnunciosActivity extends AppCompatActivity implements OnAnuncioClickListener {

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

        adapter = new AnuncioAdapter(listaAnuncios, this);

        recyclerView.setAdapter(adapter);

        // Recibe la categoría desde el Intent
        categoriaSeleccionada = getIntent().getStringExtra("categoria");
        if (categoriaSeleccionada == null || categoriaSeleccionada.isEmpty()) {
            Toast.makeText(this, "No se ha seleccionado una categoría", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Consulta a Firestore para obtener los anuncios de esta categoría
        new FirestoreHelper().getDb().collection("anuncios")
                .whereEqualTo("oficio", categoriaSeleccionada)
                .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(FilteredAnunciosActivity.this, "Error al cargar anuncios", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    listaAnuncios.clear();
                    if (querySnapshot != null) {
                        querySnapshot.getDocuments().forEach(doc -> {
                            Anuncio anuncio = doc.toObject(Anuncio.class);
                            if (anuncio != null) {
                                listaAnuncios.add(anuncio);
                            }
                        });
                        adapter.notifyDataSetChanged();
                    }
                });


    }

    @Override
    public void onAnuncioClick(Anuncio anuncio) {
        // Lanza la actividad de detalles con el objeto anuncio
        Intent intent = new Intent(this, DetalleAnuncioActivity.class);
        intent.putExtra("anuncio", anuncio);
        startActivity(intent);
    }

}
