package com.example.homeservice.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.homeservice.utils.KeystoreManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> mensajes;
    private EditText etMensaje;
    private Button btnEnviar;
    private String conversationId;
    private FirebaseFirestore db;
    private KeystoreManager keystore;   // ← agrega esto

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        try {
            keystore = new KeystoreManager();
        } catch (Exception e) {
            // si falla el keystore, lo dejamos a null y mostraremos en claro
            keystore = null;
            Log.w("ChatActivity", "No se pudo inicializar Keystore", e);
        }

        recyclerView = findViewById(R.id.recyclerViewChat);
        etMensaje    = findViewById(R.id.etMensaje);
        btnEnviar    = findViewById(R.id.btnEnviar);
        db           = FirebaseFirestore.getInstance();

        mensajes = new ArrayList<>();
        adapter  = new ChatAdapter(mensajes);
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

        cargarMensajes();

        btnEnviar.setOnClickListener(v -> enviarMensaje());
    }

    private void cargarMensajes() {
        db.collection("conversaciones")
                .document(conversationId)
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
                            if (msg == null) continue;

                            // ── DESCIFRAR el texto si es posible ──
                            if (keystore != null) {
                                try {
                                    String plain = keystore.decryptData(msg.getTexto());
                                    msg.setTexto(plain);
                                } catch (Exception e) {
                                    Log.w("ChatActivity", "No se pudo descifrar mensaje, lo muestro en claro", e);
                                }
                            }
                            mensajes.add(msg);
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

        // 1) Cifrar el texto si es posible
        String tempEncrypted = texto;
        if (keystore != null) {
            try {
                tempEncrypted = keystore.encryptData(texto);
            } catch (Exception e) {
                Log.w("ChatActivity", "No se pudo cifrar mensaje, se envía en claro", e);
            }
        }
        final String encryptedMessage = tempEncrypted;  // ahora sí es efectivamente final

        // 2) Construir el objeto ChatMessage con el texto cifrado
        final String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        ChatMessage mensaje = new ChatMessage(currentUserId, encryptedMessage, new Date().getTime());

        // 3) Subir a la colección de mensajes
        db.collection("conversaciones")
                .document(conversationId)   // 'conversationId' es un campo, así que está permitido
                .collection("mensajes")
                .add(mensaje)
                .addOnSuccessListener(docRef -> {
                    etMensaje.setText("");

                    // 4) Actualizar el documento padre de la conversación con meta cifrada
                    Map<String,Object> meta = new HashMap<>();
                    meta.put("lastMessage", encryptedMessage);
                    meta.put("timestamp", FieldValue.serverTimestamp());
                    meta.put("lastSender", currentUserId);

                    db.collection("conversaciones")
                            .document(conversationId)
                            .update(meta)
                            .addOnSuccessListener(aVoid ->
                                    Log.d("ChatActivity", "Metadatos de conversación actualizados"))
                            .addOnFailureListener(e ->
                                    Log.e("ChatActivity", "Error actualizando conversación", e));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error enviando mensaje", Toast.LENGTH_SHORT).show());
    }
    @Override
    protected void onResume() {
        super.onResume();
        // ── MARCAR COMO LEÍDO al volver a esta pantalla ──
        if (conversationId != null) {
            getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    .edit()
                    .putLong("lastRead_" + conversationId, System.currentTimeMillis())
                    .apply();
        }
    }

}