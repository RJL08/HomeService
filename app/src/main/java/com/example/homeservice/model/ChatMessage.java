package com.example.homeservice.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String senderId;
    private String texto;
    private long timestamp;

    // Constructor vacío requerido por Firestore
    public ChatMessage() {
    }

    public ChatMessage(String senderId, String texto, long timestamp) {
        this.senderId = senderId;
        this.texto = texto;
        this.timestamp = timestamp;
    }

    // Getters y setters
    public String getSenderId() {
        return senderId;
    }
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    public String getTexto() {
        return texto;
    }
    public void setTexto(String texto) {
        this.texto = texto;
    }
    public long getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
