package com.example.homeservice.ui.MisAnuncios;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.homeservice.ui.Anuncios.DetalleAnuncioActivity;
import com.example.homeservice.R;
import com.example.homeservice.adapter.AnuncioAdapter;
import com.example.homeservice.adapter.OnAnuncioClickListener;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Anuncio;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class SlideshowFragment extends Fragment implements OnAnuncioClickListener {

    private ProgressBar progressBar;
    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private AnuncioAdapter adapter;
    private final List<Anuncio> listaAnuncios = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewMisAnuncios);
        progressBar = root.findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AnuncioAdapter(listaAnuncios, this);
        recyclerView.setAdapter(adapter);

        cargarMisAnuncios();

        return root;
    }

    private void cargarMisAnuncios() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No has iniciado sesión", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = user.getUid();

        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.leerAnunciosPorUsuario(userId,
                anuncios -> {
                    listaAnuncios.clear();
                    listaAnuncios.addAll(anuncios);
                    adapter.notifyDataSetChanged();
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                },
                error -> {
                    Toast.makeText(getContext(), "Error al cargar tus anuncios", Toast.LENGTH_SHORT).show();
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
        );
    }

    @Override
    public void onAnuncioClick(Anuncio anuncio) {
        // Al pulsar un anuncio, lanzamos la actividad de detalle
        Intent intent = new Intent(getContext(), DetalleAnuncioActivity.class);
        intent.putExtra("anuncio", anuncio);  // Recuerda que tu clase Anuncio debe implementar Serializable.
        startActivity(intent);
    }
}