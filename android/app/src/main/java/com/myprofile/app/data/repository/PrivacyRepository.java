package com.myprofile.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PrivacyRepository {

    public static final String[] KEYS = {
            "city", "about", "skills", "education", "experience", "services", "achievements"
    };

    public interface LoadCallback {
        void onLoaded(Map<String, Boolean> publicFlags);

        void onError(@NonNull Exception e);
    }

    public interface SaveCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference privacyDoc(String uid) {
        return db.collection("profiles").document(uid).collection("privacy").document("settings");
    }

    public void load(@NonNull String uid, @NonNull LoadCallback callback) {
        privacyDoc(uid).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Boolean> flags = new HashMap<>();
                    for (String key : KEYS) {
                        boolean defaultValue = !"city".equals(key);
                        if (doc != null && doc.exists()) {
                            String stored = doc.getString(key + "Visibility");
                            flags.put(key, stored == null ? defaultValue : "public".equals(stored));
                        } else {
                            flags.put(key, defaultValue);
                        }
                    }
                    callback.onLoaded(flags);
                })
                .addOnFailureListener(callback::onError);
    }

    public void save(@NonNull String uid, @NonNull Map<String, Boolean> publicFlags,
                     @NonNull SaveCallback callback) {
        Map<String, Object> data = new HashMap<>();
        for (String key : KEYS) {
            boolean isPublic = Boolean.TRUE.equals(publicFlags.get(key));
            data.put(key + "Visibility", isPublic ? "public" : "private");
        }
        data.put("updatedAt", FieldValue.serverTimestamp());

        privacyDoc(uid).set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}