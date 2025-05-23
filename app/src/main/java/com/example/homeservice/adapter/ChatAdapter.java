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
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_ENVIADO  = 0;
    private static final int TIPO_RECIBIDO = 1;

    private final List<ChatMessage> mensajes;
    private final String myUid;

    public ChatAdapter(List<ChatMessage> mensajes, String myUid) {
        this.mensajes = mensajes;
        this.myUid = myUid;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = mensajes.get(position);
        // Si el senderId coincide con mi UID → layout “enviado”
        return msg.getSenderId().equals(myUid)
                ? TIPO_ENVIADO
                : TIPO_RECIBIDO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_ENVIADO) {
            View v = inf.inflate(R.layout.item_chat_sent, parent, false);
            return new SentVH(v);
        } else {
            View v = inf.inflate(R.layout.item_chat_received, parent, false);
            return new RecVH(v);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        ChatMessage m = mensajes.get(position);
        if (holder instanceof SentVH) {
            ((SentVH) holder).bind(m);
        } else {
            ((RecVH) holder).bind(m);
        }
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    // ViewHolder para mensajes enviados
    static class SentVH extends RecyclerView.ViewHolder {
        private final TextView tvTexto;
        private final TextView tvTimestamp;

        SentVH(@NonNull View itemView) {
            super(itemView);
            tvTexto     = itemView.findViewById(R.id.tvMensajeTexto);
            tvTimestamp = itemView.findViewById(R.id.tvMensajeTimestamp);
        }

        void bind(ChatMessage m) {
            tvTexto.setText(m.getTexto());
            String hora = DateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(new Date(m.getTimestamp()));
            tvTimestamp.setText(hora);
        }
    }

    // ViewHolder para mensajes recibidos
    static class RecVH extends RecyclerView.ViewHolder {
        private final TextView tvTexto;
        private final TextView tvTimestamp;

        RecVH(@NonNull View itemView) {
            super(itemView);
            tvTexto     = itemView.findViewById(R.id.tvMensajeTexto);
            tvTimestamp = itemView.findViewById(R.id.tvMensajeTimestamp);
        }

        void bind(ChatMessage m) {
            tvTexto.setText(m.getTexto());
            String hora = DateFormat.getTimeInstance(DateFormat.SHORT)
                    .format(new Date(m.getTimestamp()));
            tvTimestamp.setText(hora);
        }
    }
}
