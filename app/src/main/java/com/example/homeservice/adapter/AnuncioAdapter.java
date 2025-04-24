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
import com.example.homeservice.interfaz.OnAnuncioClickListener;
import com.example.homeservice.interfaz.OnFavoriteToggleListener;
import com.example.homeservice.model.Anuncio;

import java.util.List;

/**
 * Adaptador para mostrar la lista de anuncios en el RecyclerView del HomeFragment.
 */
public class AnuncioAdapter extends RecyclerView.Adapter<AnuncioAdapter.AnuncioViewHolder> {

    private List<Anuncio> listaAnuncios;
    private OnAnuncioClickListener listener;
    private OnFavoriteToggleListener favoriteListener;

    /**
     * Constructor que recibe la lista de anuncios y los listeners.
     */
    public AnuncioAdapter(List<Anuncio> listaAnuncios,
                          OnAnuncioClickListener listener,
                          OnFavoriteToggleListener favoriteListener) {
        this.listaAnuncios = listaAnuncios;
        this.listener = listener;
        this.favoriteListener = favoriteListener;
    }

    @NonNull
    @Override
    public AnuncioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View vista = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anuncio, parent, false);
        return new AnuncioViewHolder(vista);
    }

    @Override
    public void onBindViewHolder(@NonNull AnuncioViewHolder holder, int position) {
        Anuncio anuncio = listaAnuncios.get(position);

        holder.tvTitulo.setText(anuncio.getTitulo());
        holder.tvOficio.setText(anuncio.getOficio());
        holder.tvLocalizacion.setText(anuncio.getLocalizacion());
        holder.tvDescripcion.setText(anuncio.getDescripcion());

        // Carga de imagen principal
        if (!anuncio.getListaImagenes().isEmpty()) {
            String urlPrimera = anuncio.getListaImagenes().get(0);
            Glide.with(holder.itemView.getContext())
                    .load(urlPrimera)
                    .placeholder(R.drawable.nophoto)
                    .into(holder.ivAnuncio);
        } else {
            holder.ivAnuncio.setImageResource(R.drawable.nophoto);
        }

        // Configura el corazón según estado favorito
        if (anuncio.isFavorite()) {
            holder.ivFavorite.setImageResource(R.drawable.favoritos);
        } else {
            holder.ivFavorite.setImageResource(R.drawable.favorite);
        }

        // Toggle favorito
        holder.ivFavorite.setOnClickListener(v -> {
            if (anuncio.isFavorite()) {
                anuncio.setFavorite(false);
                holder.ivFavorite.setImageResource(R.drawable.favorite);
                if (favoriteListener != null) favoriteListener.onFavoriteRemoved(anuncio);
            } else {
                anuncio.setFavorite(true);
                holder.ivFavorite.setImageResource(R.drawable.favoritos);
                if (favoriteListener != null) favoriteListener.onFavoriteAdded(anuncio);
            }
        });

        // Clic en todo el item para abrir detalle
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAnuncioClick(anuncio);
        });
    }

    @Override
    public int getItemCount() {
        return listaAnuncios.size();
    }

    static class AnuncioViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvOficio, tvLocalizacion, tvDescripcion;
        ImageView ivAnuncio, ivFavorite;

        public AnuncioViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAnuncio = itemView.findViewById(R.id.ivAnuncio);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvOficio = itemView.findViewById(R.id.tvOficio);
            tvLocalizacion = itemView.findViewById(R.id.tvLocalizacion);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
        }
    }
}
