package com.example.homeservice.ui.chat;

import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private NotificacionesAdapter adapter;
    private final List<Conversation> conversationList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        recyclerView = findViewById(R.id.recyclerViewNotificaciones);
        progressBar = findViewById(R.id.progressBarNotificaciones);

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

        new FirestoreHelper().getDb().collection("conversaciones")
                .whereArrayContains("participants", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    if (error != null) {
                        Toast.makeText(this, "Error al cargar conversaciones", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    conversationList.clear();
                    if (querySnapshot != null) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Conversation conv = doc.toObject(Conversation.class);
                            if (conv != null) {
                                conv.setId(doc.getId());
                                conversationList.add(conv);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}