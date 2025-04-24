package com.example.homeservice.model;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.List;

/**
 * Representa una conversación (chat) entre dos usuarios.
 */
public class Conversation implements Serializable {

    private String id;                  // ID del documento en Firestore
    private List<String> participants;  // IDs de los dos usuarios
    private String lastMessage;         // Último mensaje enviado
    private Timestamp timestamp;        // Cuándo se actualizó por última vez
    private String otherUserName;       // Nombre del otro usuario (opcional)

    // Constructor vacío requerido por Firestore
    public Conversation() {
    }

    // Getters y Setters
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

    public Timestamp getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getOtherUserName() {
        return otherUserName;
    }
    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }
}
