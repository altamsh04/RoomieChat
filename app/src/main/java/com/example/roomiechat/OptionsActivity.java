package com.example.roomiechat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class OptionsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private Switch roomPrivacySwitch;
    private String roomPrivacy = "Private";

    private FirebaseStorage storage;
    private StorageReference storageReference;

    private Uri imageUri;
    private String uidForProfile;

    private ImageView imageViewProfile;
    private FirestoreHelper firestoreHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        db = FirebaseFirestore.getInstance();
        firestoreHelper = new FirestoreHelper();
        uidForProfile = Utils.getUidFromSharedPreferences(this);

        imageViewProfile = findViewById(R.id.profile);

        fetchProfileData();

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

        imageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(OptionsActivity.this, ProfileActivity.class));
            }
        });
    }

    private void fetchProfileData() {
        if (uidForProfile != null) {
            DocumentReference userRef = db.collection("users").document(uidForProfile);
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String profileImageUrl = document.getString("profileImageUrl");
                            if (profileImageUrl != null) {
                                Glide.with(OptionsActivity.this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.profile) // Add a placeholder image
                                        .into(imageViewProfile);
                            } else {
                                imageViewProfile.setImageResource(R.drawable.profile); // Set placeholder if no URL found
                            }
                        } else {
                            Toast.makeText(OptionsActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(OptionsActivity.this, "Failed to fetch profile data", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void showCreateRoomPopup() {
        int roomNumber = new Random().nextInt(90000) + 10000;

        LayoutInflater inflater = getLayoutInflater();
        View popupView = inflater.inflate(R.layout.popup_create_room, null);

        TextView roomNumberTextView = popupView.findViewById(R.id.roomNumberTextView);
        roomNumberTextView.setText(String.valueOf(roomNumber));

        roomPrivacySwitch = popupView.findViewById(R.id.roomPrivacySwitch);
        roomPrivacySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateSwitchText(isChecked);
            }
        });

        final EditText usernameEditText = popupView.findViewById(R.id.roomNameEditText);

        AlertDialog.Builder createBuilder = new AlertDialog.Builder(this);
        createBuilder.setView(popupView)
                .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String roomName = usernameEditText.getText().toString().trim();

                        if (!roomName.isEmpty()) {
                            getUsernameFromDatabase(new Utils.UsernameCallback() {
                                @Override
                                public void onCallback(String username) {
                                    if (username != null) {
                                        Log.d("USERNAME", "Fetched Username: " + username);
                                        displayUsername(username);
                                    } else {
                                        Log.d("USERNAME", "Username not found");
                                    }
                                }
                            });

                            checkAndCreateRoom(roomNumber, roomName);
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

    private void getUsernameFromDatabase(Utils.UsernameCallback callback) {
        String uid = Utils.getUidFromSharedPreferences(OptionsActivity.this);
        Log.d("USER_ID", uid);
        if (uid != null) {
            Utils.fetchUsernameFromFirestore(uid, callback);
        } else {
            callback.onCallback(null);
        }
    }

    private void displayUsername(String username) {
        Log.d("USERNAME", username);
    }

    private void updateSwitchText(boolean isPrivate) {
        if (isPrivate) {
            roomPrivacySwitch.setText("Private");
            roomPrivacySwitch.setTextColor(Color.parseColor("#FF1010"));
            roomPrivacy = "Private";
        } else {
            roomPrivacySwitch.setText("Public");
            roomPrivacySwitch.setTextColor(Color.parseColor("#23BC00"));
            roomPrivacy = "Public";
        }
    }

    private void showJoinRoomPopup() {
        getUsernameFromDatabase(new Utils.UsernameCallback() {
            @Override
            public void onCallback(final String username) {
                if (username != null) {
                    LayoutInflater inflater = getLayoutInflater();
                    View popupView = inflater.inflate(R.layout.popup_join_room, null);

                    final EditText roomCodeEditText = popupView.findViewById(R.id.roomCodeEditText);
                    final Button joinRandomRoom = popupView.findViewById(R.id.joinRandomRoom);
                    joinRandomRoom.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            joinRandomPublicRoom(username);
                        }
                    });

                    AlertDialog.Builder joinBuilder = new AlertDialog.Builder(OptionsActivity.this);
                    joinBuilder.setView(popupView)
                            .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    String roomCode = roomCodeEditText.getText().toString().trim();
                                    if (!roomCode.isEmpty()) {
                                        checkAndJoinRoom(roomCode, username);
                                    } else {
                                        roomCodeEditText.setError("Room Code is required");
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });

                    joinBuilder.create().show();
                } else {
                    Log.d("USERNAME", "Username not found");
                }
            }
        });
    }

    private void joinRandomPublicRoom(final String username) {
        firestoreHelper.fetchPublicRoomIds(new FirestoreHelper.FirestoreCallback() {
            @Override
            public void onCallback(List<String> roomIds) {
                if (!roomIds.isEmpty()) {
                    // Get a random room ID from the list
                    String randomRoomId = roomIds.get(new Random().nextInt(roomIds.size()));
                    // Add the user to the randomly selected room
                    Log.d("RANDOM_ROOM_ID", randomRoomId);
                    addUserToRoom(randomRoomId);
                } else {
                    Toast.makeText(OptionsActivity.this, "No public rooms available to join.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkAndCreateRoom(final int roomNumber, final String roomName) {
        final String roomNumberStr = String.valueOf(roomNumber);

        getUsernameFromDatabase(new Utils.UsernameCallback() {
            @Override
            public void onCallback(final String username) {
                if (username != null) {
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
                                            createRoom(roomNumberStr, roomName);
                                        }
                                    } else {
                                        Log.w("CheckRoom", "Error checking document", task.getException());
                                    }
                                }
                            });
                } else {
                    Log.d("USERNAME", "Username not found");
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
                                String roomPrivacy = document.getString("roomPrivacy");
                                if ("Private".equals(roomPrivacy)) {
                                    Toast.makeText(OptionsActivity.this, "This room is private. You cannot join.", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Room exists and is not private, add user to the room
                                    addUserToRoom(roomCode);
                                }
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

    private void createRoom(final String roomNumberStr, final String roomName) {
        Map<String, Object> room = new HashMap<>();
        room.put("adminId", Utils.getUidFromSharedPreferences(OptionsActivity.this));
        room.put("roomId", roomNumberStr);
        room.put("roomName", roomName);
        room.put("roomPrivacy", roomPrivacy);
        room.put("createdAt", FieldValue.serverTimestamp());

        db.collection("rooms").document(roomNumberStr)
                .set(room)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        addUserToRoom(roomNumberStr);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("CreateRoom", "Error adding document", e);
                    }
                });
    }

    private void addUserToRoom(final String roomNumberStr) {
        getUsernameFromDatabase(new Utils.UsernameCallback() {
            @Override
            public void onCallback(String username) {
                if (username != null) {
                    String uniqueUserId = UUID.randomUUID().toString();

                    Map<String, Object> user = new HashMap<>();
                    user.put("username", username);
                    user.put("joinedAt", FieldValue.serverTimestamp());

                    db.collection("rooms").document(roomNumberStr)
                            .collection("users")
                            .document(uniqueUserId)
                            .set(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    joinRoom(roomNumberStr, uniqueUserId, username);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w("AddUserToRoom", "Error adding user document", e);
                                }
                            });
                } else {
                    Log.d("USERNAME", "Failed to fetch username");
                }
            }
        });
    }

    private void joinRoom(String roomNumberStr, String uniqueUserId, String username) {
        Intent intent = new Intent(OptionsActivity.this, HomeActivity.class);
        intent.putExtra("ROOM_ID", roomNumberStr);
        intent.putExtra("USER_ID", uniqueUserId);
        intent.putExtra("USERNAME", username);
        startActivity(intent);
    }
}
