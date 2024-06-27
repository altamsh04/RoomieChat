package com.example.roomiechat;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Utils {

    public static String getUidFromSharedPreferences(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("RoomieChatPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("uid", null); // Returns the UID or null if not found
    }

    public static void fetchUsernameFromFirestore(String uid, final UsernameCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            callback.onCallback(username);
                        } else {
                            Log.d("FETCH_USERNAME", "No such document");
                            callback.onCallback(null);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.d("FETCH_USERNAME", "Failed with: ", e);
                        callback.onCallback(null);
                    }
                });
    }

    public static void fetchUserDetailsFromFirestore(String uid, final UserDetailsCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String email = documentSnapshot.getString("email");
                            String bio = documentSnapshot.getString("bio");
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                            callback.onCallback(username, email, bio, profileImageUrl);
                        } else {
                            Log.d("FETCH_USER_DETAILS", "No such document");
                            callback.onCallback(null, null, null, null);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.d("FETCH_USER_DETAILS", "Failed with: ", e);
                        callback.onCallback(null, null, null, null);
                    }
                });
    }

    public interface UsernameCallback {
        void onCallback(String username);
    }

    public interface UserDetailsCallback {
        void onCallback(String username, String email, String bio, String profileImage);
    }
}
