package com.example.homeservice.ui.Favoritos;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.adapter.FavoritosAdapter;
import com.example.homeservice.interfaz.OnAnuncioClickListener;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.interfaz.OnFavoriteToggleListener;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.ui.Anuncios.DetalleAnuncioActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.ArrayList;
import java.util.List;



public class GalleryFragment extends Fragment implements OnAnuncioClickListener {

    private RecyclerView recyclerView;
    private FavoritosAdapter adapter;
    private final List<Anuncio> favoritosList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_gallery, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewFavoritos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FavoritosAdapter(favoritosList, this, new OnFavoriteToggleListener() {
            @Override
            public void onFavoriteAdded(Anuncio anuncio) {
                // Lógica para agregar a favoritos en Firebase
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String anuncioId = anuncio.getId(); // Asegúrate de que el anuncio tenga un ID
                new FirestoreHelper().agregarAFavoritos(userId, anuncioId,
                        aVoid -> Toast.makeText(getContext(), "Añadido a favoritos", Toast.LENGTH_SHORT).show(),
                        e -> Toast.makeText(getContext(), "Error al agregar a favoritos: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onFavoriteRemoved(Anuncio anuncio) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String anuncioId = anuncio.getId();

                new FirestoreHelper().eliminarDeFavoritos(userId, anuncioId,
                        aVoid -> {
                            // 1) eliminar de la lista local
                            int pos = favoritosList.indexOf(anuncio);
                            if (pos != -1) {
                                favoritosList.remove(pos);
                                adapter.notifyItemRemoved(pos);
                            }
                            Toast.makeText(getContext(), "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
                        },
                        e -> Toast.makeText(getContext(), "Error al quitar favorito: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }
        );
        recyclerView.setAdapter(adapter);

        cargarFavoritos();

        return root;
    }

    private void cargarFavoritos() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        new FirestoreHelper().leerFavoritosDeUsuario(userId,
                anuncioIds -> {
                    if (anuncioIds.isEmpty()) {
                        favoritosList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }
                    // ──────────────────────────────────────
                    // Aquí empieza el bloque que hay que pegar
                    FirebaseFirestore.getInstance()
                            .collection("anuncios")
                            .whereIn(FieldPath.documentId(), anuncioIds)   // usa FieldPath para el id
                            .get()
                            .addOnSuccessListener(snap -> {
                                favoritosList.clear();
                                for (DocumentSnapshot doc: snap.getDocuments()) {
                                    Anuncio a = doc.toObject(Anuncio.class);
                                    a.setId(doc.getId());       // asigna el id de Firestore
                                    a.setFavorite(true);        // marca como favorito
                                    favoritosList.add(a);
                                }
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(),
                                        "Error cargando favoritos", Toast.LENGTH_SHORT).show();
                            });
                    // Aquí termina el bloque
                    // ──────────────────────────────────────

                },
                e -> Toast.makeText(getContext(),
                        "Error obteniendo favoritos: "+e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onAnuncioClick(Anuncio anuncio) {
        Intent intent = new Intent(requireContext(), DetalleAnuncioActivity.class);
        intent.putExtra("anuncio", anuncio);
        startActivity(intent);
    }
}
