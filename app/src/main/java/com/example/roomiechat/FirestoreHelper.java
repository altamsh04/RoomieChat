package com.example.roomiechat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FirestoreHelper {
    private static final String TAG = "FirestoreHelper";
    private FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public void fetchPublicRoomIds(final FirestoreCallback callback) {
        db.collection("rooms")
                .whereEqualTo("roomPrivacy", "Public")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> roomIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                roomIds.add(document.getId());
                            }
                            callback.onCallback(roomIds);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public interface FirestoreCallback {
        void onCallback(List<String> roomIds);
    }
}
