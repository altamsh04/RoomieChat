package com.example.roomiechat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {

    private TextView textViewUsername, textViewEmail, textViewBio;
    private ImageView imageViewProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        textViewUsername = findViewById(R.id.textViewUsername);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewBio = findViewById(R.id.textViewBio);
        imageViewProfileImage = findViewById(R.id.profileImage);

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

    }

    private void openEditProfileActivity() {
        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void logoutUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("RoomieChatPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchUserDetails() {
        String uid = Utils.getUidFromSharedPreferences(ProfileActivity.this);
        if (uid != null) {
            Utils.fetchUserDetailsFromFirestore(uid, new Utils.UserDetailsCallback() {
                @Override
                public void onCallback(String username, String email, String bio, String profileImage) {
                    if (username != null && email != null) {
                        textViewUsername.setText(username);
                        textViewEmail.setText(email);
                        textViewBio.setText(bio);
                    } else {
                        textViewUsername.setText("Username not found");
                        textViewEmail.setText("Email not found");
                        textViewBio.setText("Bio not found");
                    }
                    if (profileImage != null) {
                        Glide.with(ProfileActivity.this)
                                .load(profileImage)
                                .placeholder(R.drawable.profile) // Add a placeholder image
                                .into(imageViewProfileImage);
                    } else {
                        imageViewProfileImage.setImageResource(R.drawable.profile); // Set placeholder if no URL found
                    }
                }
            });
        } else {
            textViewUsername.setText("UID not found in SharedPreferences");
            textViewEmail.setText("UID not found in SharedPreferences");
        }

    }
}
