package com.example.homeservice.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homeservice.R;
import com.example.homeservice.model.Anuncio;

import java.util.List;

/**
 * Adaptador para mostrar la lista de anuncios en el RecyclerView del HomeFragment.
 */
public class AnuncioAdapter extends RecyclerView.Adapter<AnuncioAdapter.AnuncioViewHolder> {

    private List<Anuncio> listaAnuncios;
    private OnAnuncioClickListener listener;

    /**
     * Constructor que recibe la lista de anuncios.
     */
    public AnuncioAdapter(List<Anuncio> listaAnuncios, OnAnuncioClickListener listener) {
        this.listaAnuncios = listaAnuncios;
        this.listener = listener;
    }

    /**
     * Crea las vistas para los items del RecyclerView.
     */
    @NonNull
    @Override
    public AnuncioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflamos el layout del ítem (item_anuncio.xml)
        View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anuncio, parent, false);
        return new AnuncioViewHolder(vista);
    }

    /**
     * Vincula los datos de un anuncio con la vista.
     */
    @Override
    public void onBindViewHolder(@NonNull AnuncioViewHolder holder, int position) {
        Anuncio anuncio = listaAnuncios.get(position);
        holder.tvTitulo.setText(anuncio.getTitulo());
        holder.tvOficio.setText(anuncio.getOficio());
        holder.tvLocalizacion.setText(anuncio.getLocalizacion());
        holder.tvDescripcion.setText(anuncio.getDescripcion());

        // Cargar la primera imagen si existe
        if (!anuncio.getListaImagenes().isEmpty()) {
            String urlPrimera = anuncio.getListaImagenes().get(0);
            Glide.with(holder.itemView.getContext())
                    .load(urlPrimera)
                    .placeholder(R.drawable.nophoto) // Tu placeholder
                    .into(holder.ivAnuncio);
        } else {
            // Si no hay imágenes, un placeholder
            holder.ivAnuncio.setImageResource(R.drawable.nophoto);
        }

        // Evento de clic
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAnuncioClick(anuncio);
            }
        });
    }

    /**
     * Devuelve la cantidad total de elementos.
     */
    @Override
    public int getItemCount() {
        return listaAnuncios.size();
    }

    /**
     * ViewHolder que contiene las vistas del layout del ítem.
     */
    public static class AnuncioViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvOficio, tvLocalizacion, tvDescripcion;
        ImageView ivAnuncio;

        public AnuncioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAnuncio = itemView.findViewById(R.id.ivAnuncio);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvOficio = itemView.findViewById(R.id.tvOficio);
            tvLocalizacion = itemView.findViewById(R.id.tvLocalizacion);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
        }
    }
}
