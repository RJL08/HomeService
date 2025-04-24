package com.example.homeservice.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.homeservice.R;
import com.example.homeservice.model.Anuncio;
import com.example.homeservice.interfaz.OnAnuncioClickListener;
import com.example.homeservice.interfaz.OnFavoriteToggleListener;
import java.util.List;

public class FavoritosAdapter extends RecyclerView.Adapter<FavoritosAdapter.FavoritoViewHolder> {

    private List<Anuncio> listaFavoritos;
    private OnAnuncioClickListener listener; // Para abrir detalles
    private OnFavoriteToggleListener favoriteListener; // Para el botón de favorito

    public FavoritosAdapter(List<Anuncio> listaFavoritos,
                            OnAnuncioClickListener listener,
                            OnFavoriteToggleListener favoriteListener) {
        this.listaFavoritos = listaFavoritos;
        this.listener = listener;
        this.favoriteListener = favoriteListener;
    }

    @NonNull
    @Override
    public FavoritoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorito, parent, false);
        return new FavoritoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoritoViewHolder holder, int position) {
        Anuncio anuncio = listaFavoritos.get(position);

        holder.tvTitulo.setText(anuncio.getTitulo());
        holder.tvLocalizacion.setText(anuncio.getLocalizacion());

        // Carga la imagen (primera si existe)
        if (anuncio.getListaImagenes() != null && !anuncio.getListaImagenes().isEmpty()) {
            String urlPrimera = anuncio.getListaImagenes().get(0);
            Glide.with(holder.itemView.getContext())
                    .load(urlPrimera)
                    .placeholder(R.drawable.nophoto)
                    .into(holder.ivFavorito);
        } else {
            holder.ivFavorito.setImageResource(R.drawable.nophoto);
        }

        // Configura el botón de favorito según el estado actual
        if (anuncio.isFavorite()) {
            holder.btnFavorite.setImageResource(R.drawable.favoritos); // Corazón lleno
        } else {
            holder.btnFavorite.setImageResource(R.drawable.favorite); // Corazón vacío
        }

        // Configura el listener para el botón de favorito
        holder.btnFavorite.setOnClickListener(v -> {
            // Evitar que el clic se propague al item completo
            v.setClickable(false);
            if (anuncio.isFavorite()) {
                anuncio.setFavorite(false);
                holder.btnFavorite.setImageResource(R.drawable.favorite);
                if (favoriteListener != null) {
                    favoriteListener.onFavoriteRemoved(anuncio);
                }
            } else {
                anuncio.setFavorite(true);
                holder.btnFavorite.setImageResource(R.drawable.favoritos);
                if (favoriteListener != null) {
                    favoriteListener.onFavoriteAdded(anuncio);
                }
            }
            v.setClickable(true);
        });

        // Listener para el item completo (abre detalles)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAnuncioClick(anuncio);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaFavoritos.size();
    }

    public static class FavoritoViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvLocalizacion;
        ImageView ivFavorito;
        ImageButton btnFavorite;

        public FavoritoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTituloFavorito);
            tvLocalizacion = itemView.findViewById(R.id.tvLocalizacionFavorito);
            ivFavorito = itemView.findViewById(R.id.ivFavorito);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}
