package com.example.roomiechat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchFriendsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText searchInput;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<User> userList;
    private TextView homeTextView, notFollowAnyoneYet;
    private String currentUserId;
    private boolean fetchMyFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friends);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Retrieve the fetchMyFriends flag from the intent extras
        fetchMyFriends = getIntent().getBooleanExtra("FETCH_MY_FRIENDS", false);

        searchInput = findViewById(R.id.searchInput);
        recyclerView = findViewById(R.id.recyclerView);
        notFollowAnyoneYet = findViewById(R.id.notFollowAnyoneYet);

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(userAdapter);

        if (fetchMyFriends) {
            fetchAllFriends();
        } else {
            fetchAllUsers();
        }

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (fetchMyFriends) {
                    searchFriends(s.toString());
                } else {
                    searchUsers(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });

        homeTextView = findViewById(R.id.homeTextView);
        homeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void fetchAllUsers() {
        db.collection("users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    userList.clear();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        User user = document.toObject(User.class);
                        if (!user.getId().equals(currentUserId)) {
                            userList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void fetchAllFriends() {
        db.collection("users").document(currentUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                Map<String, Object> friendsMap = (Map<String, Object>) task.getResult().get("friends");
                if (friendsMap != null && !friendsMap.isEmpty()) {
                    List<String> friendIds = new ArrayList<>(friendsMap.keySet());
                    db.collection("users")
                            .whereIn("id", friendIds)
                            .get()
                            .addOnCompleteListener(friendTask -> {
                                if (friendTask.isSuccessful()) {
                                    userList.clear();
                                    for (QueryDocumentSnapshot document : friendTask.getResult()) {
                                        User user = document.toObject(User.class);
                                        userList.add(user);
                                    }
                                    userAdapter.notifyDataSetChanged();
                                }
                            });
                } else {
                    notFollowAnyoneYet.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void searchUsers(String query) {
        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            userList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                if (!user.getId().equals(currentUserId)) {
                                    userList.add(user);
                                }
                            }
                            userAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void searchFriends(String query) {
        db.collection("users").document(currentUserId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                Map<String, Object> friendsMap = (Map<String, Object>) task.getResult().get("friends");
                if (friendsMap != null && !friendsMap.isEmpty()) {
                    List<String> friendIds = new ArrayList<>(friendsMap.keySet());
                    db.collection("users")
                            .whereIn("id", friendIds)
                            .whereGreaterThanOrEqualTo("username", query)
                            .whereLessThanOrEqualTo("username", query + "\uf8ff")
                            .get()
                            .addOnCompleteListener(friendTask -> {
                                if (friendTask.isSuccessful()) {
                                    userList.clear();
                                    for (QueryDocumentSnapshot document : friendTask.getResult()) {
                                        User user = document.toObject(User.class);
                                        userList.add(user);
                                    }
                                    userAdapter.notifyDataSetChanged();
                                }
                            });
                } else {
                    notFollowAnyoneYet.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
