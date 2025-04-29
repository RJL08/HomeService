package com.example.homeservice.ui.chat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.adapter.NotificacionesAdapter;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Conversation;
import com.example.homeservice.utils.KeystoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private NotificacionesAdapter adapter;
    private final List<Conversation> conversationList = new ArrayList<>();
    private KeystoreManager keystore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        try {
            keystore = new KeystoreManager();
        } catch (Exception e) {
            keystore = null;
            Log.w("Notificaciones", "Keystore no disponible", e);
        }

        recyclerView = findViewById(R.id.recyclerViewNotificaciones);
        progressBar  = findViewById(R.id.progressBarNotificaciones);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificacionesAdapter(conversationList, this);
        recyclerView.setAdapter(adapter);

        cargarConversaciones();
    }

    private void cargarConversaciones() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("conversaciones")
                .whereArrayContains("participants", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar conversaciones", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    conversationList.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Conversation conv = doc.toObject(Conversation.class);
                            if (conv == null) continue;
                            conv.setId(doc.getId());

                            // 1) Leer autor del último mensaje
                            String lastSender = doc.getString("lastSender");

                            // 2) Timestamp y lastRead
                            long messageTs = conv.getTimestamp() != null
                                    ? conv.getTimestamp().toDate().getTime() : 0L;
                            long lastReadTs = getLastReadTimestamp(conv.getId());

                            // 3) Marca unread solo si NO eres tú y hay mensajes nuevos
                            boolean isMe = currentUserId.equals(lastSender);
                            conv.setUnread(!isMe && (messageTs > lastReadTs));

                            // ── 3) Resto de tu lógica (adId, decrypt, carga de título…)
                            String adId = doc.getString("adId");
                            conv.setAdId(adId);
                            conv.setAdTitle("Cargando anuncio…");

                            if (keystore != null && conv.getLastMessage() != null) {
                                try {
                                    conv.setLastMessage(
                                            keystore.decryptData(conv.getLastMessage())
                                    );
                                } catch (Exception e) {
                                    Log.w("Notificaciones", "No se pudo descifrar lastMessage", e);
                                }
                            }

                            conversationList.add(conv);

                            if (adId != null) {
                                FirebaseFirestore.getInstance()
                                        .collection("anuncios")
                                        .document(adId)
                                        .get()
                                        .addOnSuccessListener(adDoc -> {
                                            String title = adDoc.getString("titulo");
                                            conv.setAdTitle(title != null ? title : "—");
                                            int index = conversationList.indexOf(conv);
                                            if (index != -1) adapter.notifyItemChanged(index);
                                        });
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }


    /**
     * Recupera de SharedPreferences el timestamp de la última vez que leímos
     * esta conversación. Si no existe, devuelve 0.
     */
    private long getLastReadTimestamp(String conversationId) {
        SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        return prefs.getLong("lastRead_" + conversationId, 0L);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        for (Conversation conv : conversationList) {
            long messageTs = conv.getTimestamp() != null
                    ? conv.getTimestamp().toDate().getTime()
                    : 0L;
            long lastReadTs = getLastReadTimestamp(conv.getId());

            boolean isMe = currentUserId.equals(conv.getLastSender());   // <── aquí el cambio
            conv.setUnread(!isMe && messageTs > lastReadTs);
        }
        adapter.notifyDataSetChanged();
    }


}