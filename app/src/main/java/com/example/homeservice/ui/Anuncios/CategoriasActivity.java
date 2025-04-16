package com.example.homeservice.ui.Anuncios;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.adapter.CategoriasAdapter;
import com.example.homeservice.utils.FilteredAnunciosActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CategoriasActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CategoriasAdapter adapter;
    private List<String> categorias;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categorias);

        recyclerView = findViewById(R.id.recyclerViewCategorias);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Cargamos las categorías. Puedes definirlas en res/values/strings.xml o de forma directa.
        categorias = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.categorias_array)));

        adapter = new CategoriasAdapter(categorias, new CategoriasAdapter.OnCategoriaClickListener() {
            @Override
            public void onCategoriaClick(String categoria) {
                // Lanza la actividad para mostrar anuncios filtrados
                Intent intent = new Intent(CategoriasActivity.this, FilteredAnunciosActivity.class);
                intent.putExtra("categoria", categoria);
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
    }
}