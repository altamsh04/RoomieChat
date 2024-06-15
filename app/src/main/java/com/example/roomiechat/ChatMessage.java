package com.example.roomiechat;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String message;
    private String messageId;

    private String messageType;
    private String status;
    private Timestamp timestamp; // Use Timestamp instead of Long
    private String chatId; // Add a new field for chatId


    public ChatMessage() {
        // Default constructor required for calls to DataSnapshot.getValue(ChatMessage.class)
    }

    public ChatMessage(String message, String messageType, String status, Timestamp timestamp, String username) {
        this.message = message;
        this.messageType = messageType;
        this.status = status;
        this.timestamp = timestamp;
        this.chatId = chatId; // Initialize chatId in the constructor
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }


    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

}
