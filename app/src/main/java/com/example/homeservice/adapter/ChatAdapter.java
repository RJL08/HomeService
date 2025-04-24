package com.example.homeservice.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.model.ChatMessage;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> mensajes;

    public ChatAdapter(List<ChatMessage> mensajes) {
        this.mensajes = mensajes;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage mensaje = mensajes.get(position);
        holder.tvTexto.setText(mensaje.getTexto());
        String fecha = DateFormat.getDateTimeInstance().format(new Date(mensaje.getTimestamp()));
        holder.tvTimestamp.setText(fecha);
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvTexto, tvTimestamp;
        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTexto = itemView.findViewById(R.id.tvMensajeTexto);
            tvTimestamp = itemView.findViewById(R.id.tvMensajeTimestamp);
        }
    }
}
