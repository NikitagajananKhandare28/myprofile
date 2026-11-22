package com.myprofile.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileRepository {

    public static final String CONSENT_VERSION = "v1";
    public static final int SCHEMA_VERSION = 1;

    public interface SaveCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    public interface LoadCallback {
        void onLoaded(DocumentSnapshot snapshot);

        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void createProfile(@NonNull FirebaseUser user,
                              @NonNull String displayName,
                              @NonNull String profileType,
                              @NonNull String city,
                              @NonNull String bio,
                              boolean isMinor,
                              boolean guardianConsent,
                              @NonNull SaveCallback callback) {
        String uid = user.getUid();

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid", uid);
        userDoc.put("mobileE164", user.getPhoneNumber());
        userDoc.put("status", "active");
        userDoc.put("isMinor", isMinor);
        userDoc.put("guardianConsent", guardianConsent);
        userDoc.put("consentVersion", CONSENT_VERSION);
        userDoc.put("createdAt", FieldValue.serverTimestamp());
        userDoc.put("lastLoginAt", FieldValue.serverTimestamp());

        Map<String, Object> profileDoc = new HashMap<>();
        profileDoc.put("ownerUid", uid);
        profileDoc.put("displayName", displayName);
        profileDoc.put("profileType", profileType);
        profileDoc.put("city", city);
        profileDoc.put("bio", bio);
        profileDoc.put("status", "draft");
        profileDoc.put("schemaVersion", SCHEMA_VERSION);
        profileDoc.put("createdAt", FieldValue.serverTimestamp());
        profileDoc.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(uid), userDoc);
        batch.set(db.collection("profiles").document(uid), profileDoc);
        batch.commit()
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onError);
    }

    public void updateProfile(@NonNull String uid,
                              @NonNull String displayName,
                              @NonNull String profileType,
                              @NonNull String city,
                              @NonNull String bio,
                              @NonNull String aboutMe,
                              @NonNull List<String> skills,
                              @NonNull SaveCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", displayName);
        updates.put("profileType", profileType);
        updates.put("city", city);
        updates.put("bio", bio);
        updates.put("aboutMe", aboutMe);
        updates.put("skills", skills);
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("profiles").document(uid).update(updates)
            .addOnSuccessListener(unused -> callback.onSuccess())
            .addOnFailureListener(callback::onError);
    }

    public void getProfile(@NonNull String uid, @NonNull LoadCallback callback) {
        db.collection("profiles").document(uid).get()
            .addOnSuccessListener(callback::onLoaded)
            .addOnFailureListener(callback::onError);
    }
}
