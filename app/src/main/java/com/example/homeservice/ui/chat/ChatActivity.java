package com.example.homeservice.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.adapter.ChatAdapter;
import com.example.homeservice.model.ChatMessage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> mensajes;
    private EditText etMensaje;
    private Button btnEnviar;
    private String conversationId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recyclerViewChat);
        etMensaje = findViewById(R.id.etMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        db = FirebaseFirestore.getInstance();

        mensajes = new ArrayList<>();
        adapter = new ChatAdapter(mensajes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        conversationId = getIntent().getStringExtra("conversationId");
        if (conversationId == null) {
            Toast.makeText(this, "No se encontró la conversación", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarMensajes();

        btnEnviar.setOnClickListener(v -> enviarMensaje());
    }

    private void cargarMensajes() {
        db.collection("conversaciones").document(conversationId)
                .collection("mensajes")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error al leer mensajes", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mensajes.clear();
                    if (value != null) {
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            ChatMessage msg = doc.toObject(ChatMessage.class);
                            if (msg != null) {
                                mensajes.add(msg);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        if (!mensajes.isEmpty()) {
                            recyclerView.scrollToPosition(mensajes.size() - 1);
                        }
                    }
                });
    }

    private void enviarMensaje() {
        String texto = etMensaje.getText().toString().trim();
        if (texto.isEmpty()) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ChatMessage mensaje = new ChatMessage(currentUserId, texto, new Date().getTime());

        db.collection("conversaciones").document(conversationId)
                .collection("mensajes")
                .add(mensaje)
                .addOnSuccessListener(documentReference -> etMensaje.setText(""))
                .addOnFailureListener(e -> Toast.makeText(this, "Error enviando mensaje", Toast.LENGTH_SHORT).show());
    }


}
