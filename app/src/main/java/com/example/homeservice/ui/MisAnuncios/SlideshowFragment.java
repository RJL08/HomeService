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
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeservice.MyApp;
import com.example.homeservice.interfaz.OnAnuncioLongClickListener;
import com.example.homeservice.interfaz.OnFavoriteToggleListener;
import com.example.homeservice.ui.Anuncios.DetalleAnuncioActivity;
import com.example.homeservice.R;
import com.example.homeservice.adapter.AnuncioAdapter;
import com.example.homeservice.interfaz.OnAnuncioClickListener;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Anuncio;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SlideshowFragment extends Fragment
        implements OnAnuncioClickListener, OnFavoriteToggleListener, OnAnuncioLongClickListener {

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private AnuncioAdapter adapter;
    private final List<Anuncio> listaAnuncios = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewMisAnuncios);
        progressBar  = root.findViewById(R.id.progressBar);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 1) Creamos el adaptador pasándole 'this' para ambos listeners:
        adapter = new AnuncioAdapter(listaAnuncios, this, this, this);
        recyclerView.setAdapter(adapter);



        MyApp.getKeyReady().observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                cargarMisAnuncios();
            } else {
                Toast.makeText(requireContext(), "No se pudo inicializar clave de descifrado", Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });
        return root;
    }

    @Override
    public void onAnuncioLongClick(Anuncio anuncio) {
        new MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_HomeService_Dialog)
                .setTitle("Eliminar anuncio")
                .setMessage("¿Estás seguro de que quieres borrar este anuncio?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí, borrar", (d, w) -> {
                    String adId = anuncio.getId();
                    FirebaseFirestore.getInstance()
                            .collection("anuncios")
                            .document(adId)
                            .delete()
                            .addOnSuccessListener(v -> {
                                // lo quitamos de la lista y refrescamos
                                listaAnuncios.remove(anuncio);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(getContext(),
                                        "Anuncio eliminado",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Error al eliminar: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show()
                            );
                })
                .show();
    }


    private void cargarMisAnuncios() {
        progressBar.setVisibility(View.VISIBLE);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No has iniciado sesión", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = user.getUid();

        new FirestoreHelper().leerAnunciosPorUsuario(userId,
                anuncios -> {
                    listaAnuncios.clear();
                    listaAnuncios.addAll(anuncios);
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                },
                error -> {
                    Toast.makeText(getContext(), "Error al cargar tus anuncios", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
        );
    }

    // ------------------------------------
    // OnAnuncioClickListener: abre detalle
    // ------------------------------------
    @Override
    public void onAnuncioClick(Anuncio anuncio) {
        Intent intent = new Intent(requireContext(), DetalleAnuncioActivity.class);
        intent.putExtra("anuncio", anuncio);
        startActivity(intent);
    }

    // --------------------------------------------------------
    // OnFavoriteToggleListener: añade/quita de favoritos en Firestore
    // --------------------------------------------------------

    @Override
    public void onFavoriteAdded(Anuncio anuncio) {
        String userId    = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String anuncioId = anuncio.getId();  // asegúrate de tener getId() en tu modelo
        new FirestoreHelper().agregarAFavoritos(userId, anuncioId,
                aVoid -> {
                    Toast.makeText(getContext(), "Añadido a favoritos", Toast.LENGTH_SHORT).show();
                },
                e -> {
                    Toast.makeText(getContext(),
                            "Error al agregar a favoritos: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );
    }

    @Override
    public void onFavoriteRemoved(Anuncio anuncio) {
        String userId    = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String anuncioId = anuncio.getId();
        new FirestoreHelper().eliminarDeFavoritos(userId, anuncioId,
                aVoid -> {
                    Toast.makeText(getContext(), "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                },
                e -> {
                    Toast.makeText(getContext(),
                            "Error al eliminar de favoritos: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );
    }
}