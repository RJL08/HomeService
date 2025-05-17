package com.example.homeservice.notificaciones;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.example.homeservice.R;
import com.example.homeservice.ui.chat.ChatActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class ServicioFirebase extends FirebaseMessagingService {

    private static final String CANAL_CHAT = "canal_chat";
    private Uri sonidoUri;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage msg) {
        crearCanalSiHaceFalta();

        // 1) Extraer datos del mensaje
        Map<String, String> datos       = msg.getData();
        String             titulo       = datos.get("title");
        String             convId       = datos.get("conversationId");
        if (convId == null) return;

        // 2) Valores por defecto para el título
        if (titulo == null) titulo = "Nuevo mensaje";

        // 3) Fijamos el cuerpo al mensaje por defecto (ignoramos el cifrado)
        String cuerpoPlano = "Tienes un mensaje nuevo";

        // 4) Disparar notificación individual y resumen
        mostrarNotificacionIndividual(titulo, cuerpoPlano, convId);
        mostrarNotificacionResumen    (titulo,               convId);
    }

    /** Construye y lanza la notificación individual. */
    private void mostrarNotificacionIndividual(String title, String body, String convId) {
        RemoteViews small = new RemoteViews(getPackageName(), R.layout.notification_chat_small);
        small.setTextViewText(R.id.tvTitulo, title);
        small.setTextViewText(R.id.tvCuerpo, body);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CANAL_CHAT)
                .setSmallIcon(R.drawable.logo)
                .setCustomContentView(small)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setContentIntent(buildConversationIntent(convId))
                .setAutoCancel(true)
                .setGroup(convId)
                .setGroupSummary(false)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(sonidoUri);

        NotificationManager nm = getSystemService(NotificationManager.class);
        int notificationId = (int) System.currentTimeMillis();
        nm.notify(notificationId, nb.build());
    }

    /** Construye y lanza la notificación de resumen para la conversación. */
    private void mostrarNotificacionResumen(String title, String convId) {
        NotificationCompat.Builder summary = new NotificationCompat.Builder(this, CANAL_CHAT)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(title)
                .setContentText("Tienes nuevos mensajes en esta conversación")
                .setContentIntent(buildConversationIntent(convId))
                .setGroup(convId)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);


        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.notify(convId.hashCode() + 1, summary.build());
    }

    /** Crea el PendingIntent para abrir ChatActivity con el conversationId. */
    private PendingIntent buildConversationIntent(String convId) {
        Intent intent = new Intent(this, ChatActivity.class)
                .putExtra("conversationId", convId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
                this,
                convId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @Override
    public void onNewToken(@NonNull String token) {
        GestorNotificaciones.subirTokenAFirestore(token);
    }

    /** Crea el canal de notificaciones en Android O+. */
    private void crearCanalSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CANAL_CHAT) != null) return;

        AudioAttributes atr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .build();

        sonidoUri = Uri.parse(
                ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                        + getPackageName() + "/" + R.raw.notificaciones
        );

        NotificationChannel canal = new NotificationChannel(
                CANAL_CHAT,
                "Chats",
                NotificationManager.IMPORTANCE_HIGH
        );
        canal.setDescription("Notificaciones de mensajes de chat");
        canal.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                atr
        );
        canal.enableVibration(true);
        canal.setSound(sonidoUri, atr);
        nm.createNotificationChannel(canal);
    }
}
