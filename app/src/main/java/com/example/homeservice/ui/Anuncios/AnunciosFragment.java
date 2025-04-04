package com.example.homeservice.ui.Anuncios;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homeservice.databinding.FragmentHomeBinding;

public class AnunciosFragment extends Fragment {

    private FragmentHomeBinding binding;
    private RecyclerView recyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Configurar RecyclerView con GridLayout de 2 columnas
        recyclerView = binding.recyclerViewAnuncios;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Aquí puedes configurar tu adaptador con los datos de los anuncios
        // recyclerView.setAdapter(new AnuncioAdapter(listaAnuncios));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
