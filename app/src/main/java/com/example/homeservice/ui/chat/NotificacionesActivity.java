package com.example.homeservice.ui.chat;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.adapter.NotificacionesAdapter;
import com.example.homeservice.database.FirestoreHelper;
import com.example.homeservice.model.Conversation;
import com.example.homeservice.seguridad.CommonCrypto;
import com.example.homeservice.utils.KeystoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class NotificacionesActivity extends AppCompatActivity implements NotificacionesAdapter.OnConversacionClickLargo {

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
        // Instanciamos el adapter con el listener de borrado
        adapter = new NotificacionesAdapter(conversationList, this,conv -> {
            // Aquí abrimos el diálogo de confirmación
            new AlertDialog.Builder(this)
                    .setTitle("Eliminar conversación")
                    .setMessage("¿Deseas eliminar esta conversación solo para ti?")
                    .setPositiveButton("Sí", (d,i) -> {
                        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        // Actualizamos el array deletedFor
                        FirebaseFirestore.getInstance()
                                .collection("conversaciones")
                                .document(conv.getId())
                                .update("ocultadoPara", FieldValue.arrayUnion(myUid))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this,
                                            "Conversación oculta",
                                            Toast.LENGTH_SHORT).show();
                                    // refresca la lista (se volverá a filtrar)
                                    cargarConversaciones();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Error al ocultar: "+e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
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
                    // 1) Limpiamos antes de repoblar
                    conversationList.clear();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Conversation conv = doc.toObject(Conversation.class);
                        if (conv == null) continue;
                        conv.setId(doc.getId());

                        List<String> ocultado = conv.getOcultadoPara();
                        if (ocultado != null && ocultado.contains(currentUserId)) {
                            continue;  // no la añadimos
                        }

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


    /** Este método se invoca desde el adapter al hacer long-click. */
    @Override
    public void onConversacionLongClick(Conversation conversacion) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar conversación")
                .setMessage("¿Deseas ocultar esta conversación solo para ti?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    String miUid = FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid();
                    // Soft-delete en Firestore
                    FirebaseFirestore.getInstance()
                            .collection("conversaciones")
                            .document(conversacion.getId())
                            .update("ocultadoPara", FieldValue.arrayUnion(miUid))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this,
                                        "Conversación oculta",
                                        Toast.LENGTH_SHORT).show();
                                cargarConversaciones();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Error al ocultar: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("No", null)
                .show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }


}