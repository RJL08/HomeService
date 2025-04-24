package com.example.homeservice.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.model.Conversation;
import com.example.homeservice.ui.chat.ChatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class NotificacionesAdapter extends RecyclerView.Adapter<NotificacionesAdapter.ConvViewHolder> {

    private final List<Conversation> conversations;
    private final Context context;

    public NotificacionesAdapter(List<Conversation> conversations, Context context) {
        this.conversations = conversations;
        this.context = context;
    }

    @NonNull
    @Override
    public ConvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conversacion, parent, false);
        return new ConvViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConvViewHolder holder, int position) {
        Conversation conv = conversations.get(position);
        // Asigna el nombre del otro usuario, último mensaje y fecha
        holder.tvOtherUserName.setText(conv.getOtherUserName() != null ? conv.getOtherUserName() : "Chat");
        holder.tvLastMessage.setText(conv.getLastMessage() != null ? conv.getLastMessage() : "Sin mensajes");
        String dateText = "";
        if (conv.getTimestamp() != null) {
            dateText = DateFormat.getDateTimeInstance().format(new Date(conv.getTimestamp().toDate().getTime()));
        }
        holder.tvTimestamp.setText(dateText);

        // Al pulsar, se abre la actividad de chat
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("conversationId", conv.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public static class ConvViewHolder extends RecyclerView.ViewHolder {
        TextView tvOtherUserName, tvLastMessage, tvTimestamp;
        public ConvViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOtherUserName = itemView.findViewById(R.id.tvOtherUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}