package com.example.homeservice.notificaciones;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.homeservice.R;
import com.example.homeservice.ui.chat.ChatActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ServicioFirebase extends FirebaseMessagingService {

    private static final String CANAL_CHAT = "canal_chat";

    /* ══════════════════════════════════════════════════
       1. Se dispara cuando llega un push de FCM
       ══════════════════════════════════════════════════ */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        crearCanalSiHaceFalta();

        String titulo = msg.getData().get("title");
        if (titulo == null) titulo = "Nuevo mensaje";
        String cuerpo = msg.getData().get("body");
        if (cuerpo == null) cuerpo = "Tienes un mensaje";
        String convId = msg.getData().get("conversationId");
        if (convId == null) return;   // sin ID no podemos abrir chat

        /* PendingIntent para abrir la conversación */
        Intent i = new Intent(this, ChatActivity.class)
                .putExtra("conversationId", convId);
        PendingIntent pi = PendingIntent.getActivity(
                this, convId.hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        /* RemoteViews (pequeño y expandido) */
        RemoteViews small = new RemoteViews(getPackageName(),
                R.layout.notification_chat_small);
        small.setTextViewText(R.id.tvTitulo, titulo);
        small.setTextViewText(R.id.tvCuerpo, cuerpo);

        RemoteViews big   = new RemoteViews(getPackageName(),
                R.layout.notification_chat_big);
        big.setTextViewText(R.id.tvTituloGrande, titulo);
        big.setTextViewText(R.id.tvCuerpoGrande, cuerpo);

        /* Builder */
        NotificationCompat.Builder nb =
                new NotificationCompat.Builder(this, CANAL_CHAT)
                        .setSmallIcon(R.drawable.logo)   // tu icono
                        .setCustomContentView(small)
                        .setCustomBigContentView(big)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setAutoCancel(true)
                        .setContentIntent(pi)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(convId.hashCode(), nb.build());
    }

    /* ══════════════════════════════════════════════════
       2. Token nuevo ⇒ lo subimos a Firestore
       ══════════════════════════════════════════════════ */
    @Override public void onNewToken(@NonNull String token) {
        GestorNotificaciones.subirTokenAFirestore(token);
    }

    /* ══════════════════════════════════════════════════ */
    private void crearCanalSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CANAL_CHAT) != null) return;

        AudioAttributes atr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build();

        NotificationChannel canal = new NotificationChannel(
                CANAL_CHAT, "Chats",
                NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription("Notificaciones de mensajes de chat");
        canal.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                atr);
        canal.enableVibration(true);
        nm.createNotificationChannel(canal);
    }
}
