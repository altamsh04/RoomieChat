package com.example.roomiechat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

public class HomeActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String roomId, userId, username;
    private TextView roomIdTextView;
    private EditText messageEditText;
    private ImageButton sendButton, exitButton;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();
        roomId = getIntent().getStringExtra("ROOM_ID");
        userId = getIntent().getStringExtra("USER_ID");
        username = getIntent().getStringExtra("USERNAME");

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        exitButton = findViewById(R.id.exitButton);
        roomIdTextView = findViewById(R.id.roomIdTextView);
        roomIdTextView.setText(roomId);

        chatMessageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessageList);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);


        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CollectionReference usersDeleteRef = db.collection("rooms").document(roomId).collection("users");
                usersDeleteRef.document(userId)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                // User deleted successfully
                                finish(); // Finish the activity or do something else
                                startActivity(new Intent(HomeActivity.this, OptionsActivity.class));

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("DeleteUser", "Error deleting user", e);
                                Toast.makeText(HomeActivity.this, "Error deleting user", Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        loadUsersAndChats();
    }

    private void sendMessage() {
        String messageText = messageEditText.getText().toString().trim();
        if (!messageText.isEmpty()) {
            String chatId = UUID.randomUUID().toString();
            Map<String, Object> message = new HashMap<>();
            message.put("timestamp", FieldValue.serverTimestamp());
            message.put("messageType", "text");
            message.put("message", messageText);
            message.put("status", "sent");

            // Get a reference to the room's users collection
            CollectionReference usersRef = db.collection("rooms").document(roomId).collection("users");

            // Send the message to each user in the room
            usersRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (DocumentSnapshot userDoc : task.getResult()) {
                            String userId = userDoc.getId();

                            // Add the message to each user's chats collection
                            usersRef.document(userId).collection("chats").document(chatId)
                                    .set(message)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Message sent successfully
                                            messageEditText.setText("");
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w("SendMessage", "Error sending message", e);
                                            Toast.makeText(HomeActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Log.w("SendMessage", "Error getting users in room", task.getException());
                        Toast.makeText(HomeActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUsersAndChats() {
        db.collection("rooms").document(roomId).collection("users")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String userId = document.getId();
                                String username = document.getString("username");
                                loadUserChats(userId, username);
                            }
                        } else {
                            Log.w("LoadUsers", "Error getting documents.", task.getException());
                        }
                    }
                });
    }
    private void loadUserChats(String userId, final String username) {
        db.collection("rooms").document(roomId).collection("users")
                .document(userId).collection("chats")
                .orderBy("timestamp") // Order messages by timestamp
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("LoadUserChats", "Listen failed.", e);
                            return;
                        }

                        List<ChatMessage> newMessages = new ArrayList<>();

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot document = dc.getDocument();
                            ChatMessage chatMessage = document.toObject(ChatMessage.class);

                            // Set a messageId for the chatMessage
                            chatMessage.setMessageId(document.getId());

                            // Check for duplicates based on a unique identifier (e.g., message ID)
                            boolean isDuplicate = false;
                            for (ChatMessage existingMessage : chatMessageList) {
                                if (existingMessage.getMessageId().equals(chatMessage.getMessageId())) {
                                    isDuplicate = true;
                                    break;
                                }
                            }

                            if (!isDuplicate) {
                                newMessages.add(chatMessage);
                            }
                        }

                        chatMessageList.addAll(newMessages);
                        chatAdapter.notifyDataSetChanged();
                        chatRecyclerView.scrollToPosition(chatMessageList.size() - 1);
                    }
                });
    }



}
