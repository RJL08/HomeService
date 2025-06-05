package com.example.homeservice.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
/**
 * Representa una conversación (chat) entre dos usuarios.
 */
public class Conversation implements Serializable {

    private String id;                  // ID del documento en Firestore
    private List<String> participants;  // IDs de los dos usuarios
    private String lastMessage;         // Último mensaje enviado (cifrado o claro)
    private Timestamp timestamp;        // Cuándo se actualizó por última vez
    private String otherUserName;       // Nombre del otro usuario (opcional)

    private String adId;                // ID del anuncio asociado
    private String adTitle;             // Título del anuncio (no cifrado)

    private List<String> unreadFor;     // IDs de usuarios que todavía no han leído
    @Exclude
    private boolean unread;// Computado en cliente: ¿mi ID está en unreadFor?
    private String lastSender;
    private List<String> ocultadoPara;

    // Constructor vacío requerido por Firestore
    public Conversation() {
        // Inicializamos para evitar NPE
        this.participants = new ArrayList<>();
        this.unreadFor    = new ArrayList<>();
    }

    // ——— Getters y setters ———

    // Getter y setter para ocultadoPara
    public List<String> getOcultadoPara() {
        return ocultadoPara;
    }
    public void setOcultadoPara(List<String> ocultadoPara) {
        this.ocultadoPara = ocultadoPara;
    }

    public String getLastSender() {
        return lastSender;
    }
    public void setLastSender(String lastSender) {
        this.lastSender = lastSender;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public List<String> getParticipants() {
        return participants;
    }
    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessage() {
        return lastMessage;
    }
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }

    public String getAdId() { return adId; }
    public void setAdId(String adId) { this.adId = adId; }

    public String getAdTitle() { return adTitle; }
    public void setAdTitle(String adTitle) {
        this.adTitle = adTitle;
    }

    public List<String> getUnreadFor() {
        return unreadFor;
    }
    public void setUnreadFor(List<String> unreadFor) {
        this.unreadFor = unreadFor;
    }

    /**
     * Marcado en cliente para saber si ocultar el punto rojo.
     * Está excluido de la serialización a Firestore.
     */
    public boolean isUnread() {
        return unread;
    }
    public void setUnread(boolean unread) {
        this.unread = unread;
    }
}