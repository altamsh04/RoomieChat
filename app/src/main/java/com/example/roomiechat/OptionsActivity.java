package com.example.roomiechat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class OptionsActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        db = FirebaseFirestore.getInstance();

        Button createRoomButton = findViewById(R.id.createRoom);
        createRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateRoomPopup();
            }
        });

        Button joinRoomButton = findViewById(R.id.joinRoom);
        joinRoomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showJoinRoomPopup();
            }
        });
    }

    private void showCreateRoomPopup() {
        int roomNumber = new Random().nextInt(90000) + 10000;

        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popup_create_room, null);

        TextView roomNumberTextView = popupView.findViewById(R.id.roomNumberTextView);
        roomNumberTextView.setText(String.valueOf(roomNumber));

        final EditText usernameEditText = popupView.findViewById(R.id.usernameEditText);

        AlertDialog.Builder createBuilder = new AlertDialog.Builder(this);
        createBuilder.setView(popupView)
                .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String username = usernameEditText.getText().toString().trim();
                        if (!username.isEmpty()) {
                            checkAndCreateRoom(roomNumber, username);
                        } else {
                            usernameEditText.setError("Username is required");
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        createBuilder.create().show();
    }

    private void showJoinRoomPopup() {
        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popup_join_room, null);

        final EditText roomCodeEditText = popupView.findViewById(R.id.roomCodeEditText);
        final EditText usernameEditText = popupView.findViewById(R.id.usernameEditText);

        AlertDialog.Builder joinBuilder = new AlertDialog.Builder(this);
        joinBuilder.setView(popupView)
                .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String roomCode = roomCodeEditText.getText().toString().trim();
                        String username = usernameEditText.getText().toString().trim();
                        if (!roomCode.isEmpty() && !username.isEmpty()) {
                            checkAndJoinRoom(roomCode, username);
                        } else {
                            if (roomCode.isEmpty()) {
                                roomCodeEditText.setError("Room Code is required");
                            }
                            if (username.isEmpty()) {
                                usernameEditText.setError("Username is required");
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        joinBuilder.create().show();
    }

    private void checkAndCreateRoom(final int roomNumber, final String username) {
        final String roomNumberStr = String.valueOf(roomNumber);

        db.collection("rooms").document(roomNumberStr).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Room already exists, proceed to join
                                String userId = UUID.randomUUID().toString();
                                joinRoom(roomNumberStr, userId, username);
                            } else {
                                // Room does not exist, create a new one
                                createRoom(roomNumberStr, username);
                            }
                        } else {
                            Log.w("CheckRoom", "Error checking document", task.getException());
                        }
                    }
                });
    }

    private void checkAndJoinRoom(final String roomCode, final String username) {
        db.collection("rooms").document(roomCode).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Room exists, add user to the room
                                addUserToRoom(roomCode, username);
                            } else {
                                // Room does not exist, show a toast message
                                Toast.makeText(OptionsActivity.this, "Invalid Room Code", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w("CheckRoom", "Error checking document", task.getException());
                        }
                    }
                });
    }

    private void createRoom(final String roomNumberStr, final String username) {
        Map<String, Object> room = new HashMap<>();
        room.put("roomId", roomNumberStr);
        room.put("createdAt", FieldValue.serverTimestamp());

        db.collection("rooms").document(roomNumberStr)
                .set(room)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        addUserToRoom(roomNumberStr, username);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("CreateRoom", "Error adding document", e);
                    }
                });
    }

    private void addUserToRoom(String roomNumberStr, String username) {
        String userId = UUID.randomUUID().toString();

        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("joinedAt", FieldValue.serverTimestamp());

        db.collection("rooms").document(roomNumberStr)
                .collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        joinRoom(roomNumberStr, userId, username);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("AddUserToRoom", "Error adding user document", e);
                    }
                });
    }

    private void joinRoom(String roomNumberStr, String userId, String username) {
        Intent intent = new Intent(OptionsActivity.this, HomeActivity.class);
        intent.putExtra("ROOM_ID", roomNumberStr);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
    }
}
