package com.example.homeservice.ui.Anuncios;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeservice.R;
import com.example.homeservice.interfaz.OnAnuncioClickListener;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.databinding.FragmentHomeBinding;
import com.example.homeservice.interfaz.OnFavoriteToggleListener;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.adapter.AnuncioAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment implements OnAnuncioClickListener {

    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private AnuncioAdapter adapter;
    private List<Anuncio> listaAnuncios;
    private Button btnCategorias;
    private Set<String> favoritosIds = new HashSet<>();


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Configuro el RecyclerView
        recyclerView = binding.recyclerViewAnuncios;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Inicializo la lista y el adapter con el listener de favoritos
        listaAnuncios = new ArrayList<>();
        adapter = new AnuncioAdapter(
                listaAnuncios,
                this,  // OnAnuncioClickListener
                new OnFavoriteToggleListener() {
                    @Override
                    public void onFavoriteAdded(Anuncio anuncio) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        new FirestoreHelper().agregarAFavoritos(uid, anuncio.getId(),
                                aVoid -> Toast.makeText(getContext(),"Añadido a favoritos",Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(getContext(),"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show()
                        );
                    }
                    @Override
                    public void onFavoriteRemoved(Anuncio anuncio) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        new FirestoreHelper().eliminarDeFavoritos(uid, anuncio.getId(),
                                aVoid -> Toast.makeText(getContext(),"Eliminado de favoritos",Toast.LENGTH_SHORT).show(),
                                e -> Toast.makeText(getContext(),"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show()
                        );
                    }
                }
        );
        recyclerView.setAdapter(adapter);

        recyclerView.setAdapter(adapter);

        // Botón de categorías
        btnCategorias = root.findViewById(R.id.btnCategorias);
        btnCategorias.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CategoriasActivity.class));
        });

        // Cargo los anuncios
        cargarAnuncios();

        return root;
    }

    @Override
    public void onAnuncioClick(Anuncio anuncio) {
        // Abre pantalla de detalle
        Intent intent = new Intent(requireContext(), DetalleAnuncioActivity.class);
        intent.putExtra("anuncio", anuncio);
        startActivity(intent);
    }

    private void cargarAnuncios() {
        new FirestoreHelper().leerAnunciosDescifrados(
                lista -> {
                    listaAnuncios.clear();
                    listaAnuncios.addAll(lista);
                    adapter.notifyDataSetChanged();
                    // ahora cargamos qué IDs están en favoritos
                    sincronizarFavoritos();
                },
                error -> Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }
    private void sincronizarFavoritos() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        new FirestoreHelper().leerFavoritosDeUsuario(uid,
                anuncioIds -> {
                    favoritosIds.clear();
                    favoritosIds.addAll(anuncioIds);
                    // marcamos cada anuncio
                    for (Anuncio a : listaAnuncios) {
                        a.setFavorite(favoritosIds.contains(a.getId()));
                    }
                    adapter.notifyDataSetChanged();
                },
                e -> {
                    // opcional: log o toast
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        sincronizarFavoritos();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}