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

    private static final String PREFS_NAME = "ProfilePrefs";
    private static final String PREF_PROFILE_IMAGE = "profileImage";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_BIO = "bio";
    private static final String PREF_EMAIL = "email";

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

        SharedPreferences detailsSharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor detailsEditor = detailsSharedPreferences.edit();
        detailsEditor.putString(PREF_PROFILE_IMAGE, null);
        detailsEditor.putString(PREF_USERNAME, null);
        detailsEditor.putString(PREF_BIO, null);
        detailsEditor.putString(PREF_EMAIL, null);
        detailsEditor.apply();

        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void fetchUserDetails() {
        SharedPreferences detailsSharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String cachedProfileImage = detailsSharedPreferences.getString(PREF_PROFILE_IMAGE, null);
        String cachedUsername = detailsSharedPreferences.getString(PREF_USERNAME, null);
        String cachedBio = detailsSharedPreferences.getString(PREF_BIO, null);
        String cachedEmail = detailsSharedPreferences.getString(PREF_EMAIL, null);

        if (cachedProfileImage != null && cachedUsername != null && cachedBio != null && cachedEmail != null) {
            // Use cached data
            Glide.with(this).load(cachedProfileImage).into(imageViewProfileImage);
            textViewUsername.setText(cachedUsername);
            textViewEmail.setText(cachedEmail);
            textViewBio.setText(cachedBio);
            Log.d("FETCH_DATA", "CACHED");
        } else {
            // Fetch from Firestore and cache it
            String uid = Utils.getUidFromSharedPreferences(ProfileActivity.this);
            if (uid != null) {
                Utils.fetchUserDetailsFromFirestore(uid, new Utils.UserDetailsCallback() {
                    @Override
                    public void onCallback(String username, String email, String bio, String profileImage) {
                        if (username != null && email != null) {
                            textViewUsername.setText(username);
                            textViewEmail.setText(email);
                            textViewBio.setText(bio);

                            SharedPreferences.Editor editor = detailsSharedPreferences.edit();
                            editor.putString(PREF_PROFILE_IMAGE, profileImage);
                            editor.putString(PREF_USERNAME, username);
                            editor.putString(PREF_BIO, bio);
                            editor.putString(PREF_EMAIL, email);
                            editor.apply();

                            Log.d("FETCH_DATA", "FIRESTORE");
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
                textViewUsername.setText("Username not found");
                textViewEmail.setText("Email not found");
                textViewBio.setText("Bio not found");
            }
        }
    }
}
