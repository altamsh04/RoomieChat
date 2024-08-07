package com.example.roomiechat;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String PREFS_NAME = "ProfilePrefs";
    private static final String PREF_PROFILE_IMAGE = "profileImage";
    private static final String PREF_USERNAME = "username";
    private static final String PREF_BIO = "bio";

    private ImageView imageViewProfile;
    private TextView editProfileTextView;
    private EditText editTextUsername, editTextBio;
    private Button buttonUpdate, buttonCancel;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String uid;
    private String currentUsername;
    private Uri imageUri;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        imageViewProfile = findViewById(R.id.imageViewProfile);
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextBio = findViewById(R.id.editTextBio);
        buttonUpdate = findViewById(R.id.buttonUpdate);
        buttonCancel = findViewById(R.id.buttonCancel);
        progressBar = findViewById(R.id.progressBar);
        editProfileTextView = findViewById(R.id.editProfileTextView);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        uid = Utils.getUidFromSharedPreferences(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Fetch existing profile data
        fetchProfileData();

        editProfileTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        imageViewProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageChooser();
            }
        });

        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkUsernameAndUpdateProfile();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void fetchProfileData() {
        String cachedProfileImage = sharedPreferences.getString(PREF_PROFILE_IMAGE, null);
        String cachedUsername = sharedPreferences.getString(PREF_USERNAME, null);
        String cachedBio = sharedPreferences.getString(PREF_BIO, null);

        if (cachedProfileImage != null && cachedUsername != null && cachedBio != null) {
            // Use cached data
            Log.d("FETCH_DATA", "FETCHED_FROM_CACHE");
            Glide.with(this).load(cachedProfileImage).into(imageViewProfile);
            editTextUsername.setText(cachedUsername);
            editTextBio.setText(cachedBio);
            currentUsername = cachedUsername;
        } else {
            Log.d("FETCH_DATA", "FETCHED_FROM_DATABASE");
            // Fetch from database and cache it
            if (uid != null) {
                DocumentReference userRef = db.collection("users").document(uid);
                userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                currentUsername = document.getString("username");
                                String bio = document.getString("bio");
                                String profileImageUrl = document.getString("profileImageUrl");

                                if (currentUsername != null && bio != null) {
                                    editTextUsername.setText(currentUsername);
                                    editTextBio.setText(bio);
                                } else {
                                    editTextUsername.setText("Username not found");
                                    editTextBio.setText("Bio not found");
                                }

                                if (profileImageUrl != null) {
                                    Glide.with(EditProfileActivity.this)
                                            .load(profileImageUrl)
                                            .placeholder(R.drawable.profile) // Add a placeholder image
                                            .into(imageViewProfile);

                                    // Cache the data
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString(PREF_PROFILE_IMAGE, profileImageUrl);
                                    editor.putString(PREF_USERNAME, currentUsername);
                                    editor.putString(PREF_BIO, bio);
                                    editor.apply();
                                } else {
                                    imageViewProfile.setImageResource(R.drawable.profile); // Set placeholder if no URL found
                                }
                            } else {
                                Toast.makeText(EditProfileActivity.this, "No profile data found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Failed to fetch profile data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imageViewProfile.setImageURI(imageUri);
        }
    }

    private void checkUsernameAndUpdateProfile() {
        String newUsername = editTextUsername.getText().toString().trim();
        String newBio = editTextBio.getText().toString().trim();

        if (TextUtils.isEmpty(newUsername)) {
            editTextUsername.setError("Username is required");
            return;
        }

        if (TextUtils.isEmpty(newBio)) {
            editTextBio.setError("Bio is required");
            return;
        }

        // Only check for existing username if it has been changed
        if (!newUsername.equals(currentUsername)) {
            db.collection("users").whereEqualTo("username", newUsername).get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && task.getResult().isEmpty()) {
                                updateProfile(newUsername, newBio);
                            } else {
                                Toast.makeText(EditProfileActivity.this, "Username already exists", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.d("CHECK_USERNAME_ERROR", e.getMessage()));
        } else {
            // Username hasn't changed, just update the profile
            updateProfile(newUsername, newBio);
        }
    }

    private void updateProfile(String newUsername, String newBio) {
        if (imageUri != null) {
            uploadProfileImage(newUsername, newBio);
        } else {
            updateFirestoreProfile(newUsername, newBio, null);
        }
    }

    private void uploadProfileImage(String newUsername, String newBio) {
        progressBar.setVisibility(View.VISIBLE);
        String fileExtension = getFileExtension(imageUri);
        StorageReference profileImageRef = storageReference.child("profile_images/" + uid + "." + fileExtension);

        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        profileImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                progressBar.setVisibility(View.GONE);
                                String imageUrl = uri.toString();
                                updateFirestoreProfile(newUsername, newBio, imageUrl);
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditProfileActivity.this, "Failed to upload profile image", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void updateFirestoreProfile(String newUsername, String newBio, String imageUrl) {
        DocumentReference userRef = db.collection("users").document(uid);

        if (imageUrl != null) {
            userRef.update("username", newUsername, "bio", newBio, "profileImageUrl", imageUrl)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Update SharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(PREF_PROFILE_IMAGE, imageUrl);
                                editor.putString(PREF_USERNAME, newUsername);
                                editor.putString(PREF_BIO, newBio);
                                editor.apply();

                                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(EditProfileActivity.this, ProfileActivity.class));
                                finish(); // Close the activity after successful update
                            } else {
                                Toast.makeText(EditProfileActivity.this, "Profile update failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            userRef.update("username", newUsername, "bio", newBio)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                // Update SharedPreferences
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString(PREF_USERNAME, newUsername);
                                editor.putString(PREF_BIO, newBio);
                                editor.apply();

                                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(EditProfileActivity.this, ProfileActivity.class));
                                finish(); // Close the activity after successful update
                            } else {
                                Toast.makeText(EditProfileActivity.this, "Profile update failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
}