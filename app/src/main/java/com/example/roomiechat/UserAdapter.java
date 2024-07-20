package com.example.roomiechat;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;
    private FirebaseFirestore db;
    private String currentUserId;
    private Map<String, Object> currentUserFriends;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchCurrentUserFriends();
    }

    private void fetchCurrentUserFriends() {
        db.collection("users").document(currentUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentUserFriends = (Map<String, Object>) documentSnapshot.get("friends");
                notifyDataSetChanged(); // Refresh the RecyclerView to reflect the follow status
            }
        });
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // Set user data
        holder.username.setText(user.getUsername());
        holder.bio.setText(user.getBio());
        Glide.with(context).load(user.getProfileImageUrl()).into(holder.profileImage);

        // Check if the current user is already following this user
        if (currentUserFriends != null && currentUserFriends.containsKey(user.getId())) {
            setFollowingState(holder);
        } else {
            setUnfollowingState(holder);
        }

        // Button click listener
        holder.followButton.setOnClickListener(v -> {
            String followedUserId = user.getId();
            if (currentUserFriends != null && currentUserFriends.containsKey(followedUserId)) {
                unfollowUser(currentUserId, followedUserId, holder);
            } else {
                followUser(currentUserId, followedUserId, holder);
            }
        });
    }

    private void followUser(String currentUserId, String followedUserId, UserViewHolder holder) {
        DocumentReference currentUserRef = db.collection("users").document(currentUserId);
        DocumentReference followedUserRef = db.collection("users").document(followedUserId);

        currentUserRef.update("friends." + followedUserId, true)
                .addOnSuccessListener(aVoid -> {
                    followedUserRef.update("friends." + currentUserId, true)
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d("FOLLOW_SUCCESS", "Successfully followed user: " + followedUserId);
                                fetchCurrentUserFriends();
                                setFollowingState(holder);
                            })
                            .addOnFailureListener(e -> {
                                Log.d("FOLLOW_ERROR", "Error adding current user to followed user's friend list: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.d("FOLLOW_ERROR", "Error following user: " + followedUserId + ", " + e.getMessage());
                });
    }

    private void unfollowUser(String currentUserId, String followedUserId, UserViewHolder holder) {
        DocumentReference currentUserRef = db.collection("users").document(currentUserId);
        DocumentReference followedUserRef = db.collection("users").document(followedUserId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("friends." + followedUserId, FieldValue.delete());

        currentUserRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    followedUserRef.update("friends." + currentUserId, FieldValue.delete())
                            .addOnSuccessListener(aVoid1 -> {
                                Log.d("UNFOLLOW_SUCCESS", "Successfully unfollowed user: " + followedUserId);
                                fetchCurrentUserFriends();
                                setUnfollowingState(holder);
                            })
                            .addOnFailureListener(e -> {
                                Log.d("UNFOLLOW_ERROR", "Error removing current user from followed user's friend list: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.d("UNFOLLOW_ERROR", "Error unfollowing user: " + followedUserId + ", " + e.getMessage());
                });
    }
    private void setFollowingState(UserViewHolder holder) {
        holder.followButton.setText("Unfollow");
    }

    private void setUnfollowingState(UserViewHolder holder) {
        holder.followButton.setText("Follow");
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage;
        TextView username, bio;
        Button followButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            username = itemView.findViewById(R.id.username);
            bio = itemView.findViewById(R.id.bio);
            followButton = itemView.findViewById(R.id.followButton);
        }
    }
}
