package com.example.homeservice.ui.Anuncios;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeservice.adapter.OnAnuncioClickListener;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.databinding.FragmentHomeBinding;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.adapter.AnuncioAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements OnAnuncioClickListener {

    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;
    private AnuncioAdapter adapter;
    private List<Anuncio> listaAnuncios;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();



        recyclerView = binding.recyclerViewAnuncios;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        listaAnuncios = new ArrayList<>();
        adapter = new AnuncioAdapter(listaAnuncios, this);
        recyclerView.setAdapter(adapter);

        cargarAnuncios();

        return root;
    }

    @Override
    public void onAnuncioClick(Anuncio anuncio) {
        Intent intent = new Intent(getContext(), DetalleAnuncioActivity.class);
        intent.putExtra("anuncio", anuncio); // Asegúrate de que Anuncio implemente Serializable
        startActivity(intent);
    }


    private void cargarAnuncios() {
        FirestoreHelper firestoreHelper = new FirestoreHelper();

        firestoreHelper.leerAnuncios(
                lista -> {
                    listaAnuncios.clear();
                    listaAnuncios.addAll(lista);
                    adapter.notifyDataSetChanged();
                },
                error -> {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
