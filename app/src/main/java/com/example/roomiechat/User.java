package com.example.roomiechat;

import java.util.Map;

public class User {
    private String id;
    private String bio;
    private String email;
    private String profileImageUrl;
    private String username;
    private Map<String, Boolean> friends; // Map to store followed user IDs

    public User() {
        // Empty constructor needed for Firestore serialization
    }

    public User(String id, String bio, String email, String profileImageUrl, String username, Map<String, Boolean> friends) {
        this.id = id;
        this.bio = bio;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.username = username;
        this.friends = friends;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, Boolean> getFriends() {
        return friends;
    }

    public void setFriends(Map<String, Boolean> friends) {
        this.friends = friends;
    }
}
