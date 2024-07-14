package com.example.roomiechat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {

    private TextView textViewUsername, textViewEmail, textViewBio, settingsTextView;
    private ImageView imageViewProfileImage;
    private SharedPreferences loginSharedPreferences, detailsSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        textViewUsername = findViewById(R.id.textViewUsername);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewBio = findViewById(R.id.textViewBio);
        imageViewProfileImage = findViewById(R.id.profileImage);
        settingsTextView = findViewById(R.id.settingsTextView);

        settingsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        fetchUserDetails();

        Button buttonLogout = findViewById(R.id.logoutButton);
        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutUser();
            }
        });

        Button buttonEditProfile = findViewById(R.id.editProfile);
        buttonEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditProfileActivity();
            }
        });

        Button myFriends = findViewById(R.id.myFriends);
        myFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), SearchFriendsActivity.class);
                intent.putExtra("FETCH_MY_FRIENDS", true);
                startActivity(intent);
            }
        });

    }

    private void openEditProfileActivity() {
        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void logoutUser() {
        SharedPreferences loginSharedPreferences = getSharedPreferences("RoomieChatPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = loginSharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        SharedPreferences detailsSharedPreferences = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor detailsEditor = detailsSharedPreferences.edit();
        detailsEditor.clear();  // clear all saved profile details
        detailsEditor.apply();

        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchUserDetails() {
        detailsSharedPreferences = getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE);

        String username = detailsSharedPreferences.getString("username", null);
        String email = detailsSharedPreferences.getString("email", null);
        String bio = detailsSharedPreferences.getString("bio", null);
        String profileImage = detailsSharedPreferences.getString("profileImage", null);

        if (username != null && email != null && bio != null && profileImage != null) {
            setUserData(username, email, bio, profileImage);
            Log.d("FETCH_DATA","CACHED");
        } else {
            String uid = Utils.getUidFromSharedPreferences(ProfileActivity.this);
            if (uid != null) {
                Utils.fetchUserDetailsFromFirestore(uid, new Utils.UserDetailsCallback() {
                    @Override
                    public void onCallback(String username, String email, String bio, String profileImage) {
                        if (username != null && email != null) {
                            saveUserDetailsToPrefs(username, email, bio, profileImage);
                            setUserData(username, email, bio, profileImage);
                            Log.d("FETCH_DATA","FIRESTORE");
                        } else {
                            textViewUsername.setText("Username not found");
                            textViewEmail.setText("Email not found");
                            textViewBio.setText("Bio not found");
                        }
                    }
                });
            } else {
                textViewUsername.setText("Loading...");
                textViewEmail.setText("Loading...");
            }
        }
    }

    private void saveUserDetailsToPrefs(String username, String email, String bio, String profileImage) {
        SharedPreferences.Editor editor = detailsSharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("email", email);
        editor.putString("bio", bio);
        editor.putString("profileImage", profileImage);
        editor.apply();
    }

    private void setUserData(String username, String email, String bio, String profileImage) {
        textViewUsername.setText(username);
        textViewEmail.setText(email);
        textViewBio.setText(bio);
        Glide.with(ProfileActivity.this)
                .load(profileImage)
                .placeholder(R.drawable.profile)
                .into(imageViewProfileImage);
    }
}
