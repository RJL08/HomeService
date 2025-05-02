package com.example.homeservice.ui.chat;

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
import com.example.homeservice.model.Conversation;
import com.example.homeservice.seguridad.CommonCrypto;
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
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("conversaciones")
                .whereArrayContains("participants", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar conversaciones", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    conversationList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Conversation conv = doc.toObject(Conversation.class);
                        if (conv == null) continue;
                        conv.setId(doc.getId());

                        // ¿Un mensaje pendiente?
                        boolean pendiente = conv.getUnreadFor() != null
                                && conv.getUnreadFor().contains(currentUserId);
                        conv.setUnread(pendiente);

                        // Descifrar último mensaje
                        String ultimo = conv.getLastMessage();
                        if (ultimo != null) {
                            try {
                                conv.setLastMessage(CommonCrypto.decrypt(ultimo));
                            } catch (Exception e) {
                                Log.w("Notificaciones", "No se pudo descifrar lastMessage", e);
                            }
                        }

                        // … resto de tu lógica (título del anuncio, etc.) …
                        conversationList.add(conv);
                    }
                    adapter.notifyDataSetChanged();
                });
    }





    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }


}