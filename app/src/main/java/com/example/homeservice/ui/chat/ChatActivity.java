package com.example.homeservice.ui.chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homeservice.R;
import com.example.homeservice.adapter.ChatAdapter;
import com.example.homeservice.model.ChatMessage;
import com.example.homeservice.seguridad.CommonCrypto;
import com.example.homeservice.utils.KeystoreManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.Collections;
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
    private KeystoreManager keystore;// ← agrega esto
    // sound pool para sonidos cortos en ves de media Player
    private SoundPool soundPool;
    private int sendSoundId;
    private String myUid;

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
        //leemos las preferencias de los sonidods
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        recyclerView = findViewById(R.id.recyclerViewChat);
        etMensaje    = findViewById(R.id.etMensaje);
        btnEnviar    = findViewById(R.id.btnEnviar);
        db           = FirebaseFirestore.getInstance();

        // 2.1 Inicializa SoundPool con un solo sonido
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .build();
        sendSoundId = soundPool.load(this, R.raw.send,1);

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                mensajes = new ArrayList<>();
        adapter  = new ChatAdapter(mensajes, myUid);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        recyclerView.setAdapter(adapter);


        conversationId = getIntent().getStringExtra("conversationId");
        if (conversationId == null) {
            Toast.makeText(this, "No se encontró la conversación", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        cargarMensajes();
        btnEnviar.setOnClickListener(v -> {
            // 1) Solo reproducir si “sonidos_activados” == true
            if (prefs.getBoolean("sonidos_activados", true)) {
                soundPool.play(
                        sendSoundId,
                        /*leftVolume=*/ 1f,
                        /*rightVolume=*/ 1f,
                        /*priority=*/ 0,
                        /*loop=*/ 0,
                        /*rate=*/ 1f
                );
            }
            enviarMensaje();
        });

        // Maneja el intent inicial:
        handleConversationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Si ya estaba abierta en el back-stack, hacemos que procese de nuevo:
        setIntent(intent);
        handleConversationIntent(intent);
    }

    private void handleConversationIntent(Intent intent) {
        String convId = intent.getStringExtra("conversationId");
        if (convId != null) {
            // Carga tus mensajes aquí...
            cargarMensajes();
        }
    }


    private void cargarMensajes() {

        db.collection("conversaciones")
                .document(conversationId)
                .collection("mensajes")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Toast.makeText(this,"Error al leer mensajes",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mensajes.clear();
                    if (value != null) {
                        for (DocumentSnapshot d : value.getDocuments()) {

                            ChatMessage m = d.toObject(ChatMessage.class);
                            if (m == null) continue;

                            /* —— descifrar texto —— */
                            try {
                                m.setTexto( CommonCrypto.decrypt( m.getTexto() ) );
                            } catch (Exception e) {
                                Log.w("ChatActivity","Texto ilegible → se muestra tal cual",e);
                            }

                            mensajes.add(m);
                        }

                        adapter.notifyDataSetChanged();
                        if (!mensajes.isEmpty())
                            recyclerView.scrollToPosition(mensajes.size() - 1);
                    }
                });
    }


    private void enviarMensaje() {

        // 0) Texto plano
        String textoPlano = etMensaje.getText().toString().trim();
        if (textoPlano.isEmpty()) return;

        /* ───────── C I F R A R ───────── */
        String textoCifradoTemp;              // variable temporal

        try {
            textoCifradoTemp = CommonCrypto.encrypt(textoPlano);
        } catch (Exception e) {
            Log.w("ChatActivity","No se pudo cifrar, se envía en claro",e);
            textoCifradoTemp = textoPlano;
        }

        final String textoCifrado = textoCifradoTemp;   // ← ahora sí es final
        final String miUid        = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final ChatMessage mensaje = new ChatMessage(miUid, textoCifrado, new Date().getTime());

        /* ───────── S U B I R  M E N S A J E ───────── */
        final DocumentReference refConversacion =
                db.collection("conversaciones").document(conversationId);

        refConversacion.collection("mensajes")
                .add(mensaje)
                .addOnSuccessListener(r -> {
                    etMensaje.setText("");

                    // —— actualizar metadatos de la conversación ——
                    refConversacion.get().addOnSuccessListener(snapshot -> {

                        List<String> participantes =
                                (List<String>) snapshot.get("participants");
                        if (participantes == null) participantes = Collections.emptyList();

                        List<String> noLeidos = new ArrayList<>();
                        for (String uid : participantes)
                            if (!uid.equals(miUid)) noLeidos.add(uid);

                        Map<String,Object> meta = new HashMap<>();
                        meta.put("lastMessage", textoCifrado);
                        meta.put("timestamp",   FieldValue.serverTimestamp());
                        meta.put("lastSender",  miUid);
                        meta.put("unreadFor",   noLeidos);

                        refConversacion.update(meta)
                                .addOnSuccessListener(v ->
                                        Log.d("ChatActivity","Metadatos actualizados"))
                                .addOnFailureListener(e ->
                                        Log.e("ChatActivity","Error meta",e));
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,"Error enviando mensaje",Toast.LENGTH_SHORT).show());
    }




    @Override
    protected void onResume() {
        super.onResume();

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("conversaciones")
                .document(conversationId)
                .update("unreadFor", FieldValue.arrayRemove(myUid));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Libera recursos
        soundPool.release();
        soundPool = null;
    }

}