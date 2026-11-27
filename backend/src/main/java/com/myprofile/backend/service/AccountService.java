package com.myprofile.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.myprofile.backend.config.StorageAccess;
import com.myprofile.backend.exception.AppException;
import com.myprofile.backend.util.Await;

@Service
public class AccountService {

    private static final List<String> SUB_COLLECTIONS = List.of(
            "education", "experience", "services", "achievements", "socialLinks",
            "photos", "contact", "privacy");

    private final Firestore db;
    private final StorageAccess storage;
    private final FirebaseAuth auth;

    public AccountService(Firestore db, StorageAccess storage, FirebaseAuth auth) {
        this.db = db;
        this.storage = storage;
        this.auth = auth;
    }

    /** Safe to call again if an earlier attempt stopped half way. */
    public void deleteAccount(String uid) {
        DocumentReference profileRef = db.collection("profiles").document(uid);
        DocumentSnapshot profile = Await.of(profileRef.get());
        String username = profile.exists() ? profile.getString("username") : null;

        for (String name : SUB_COLLECTIONS) {
            deleteAll(profileRef.collection(name));
        }

        WriteBatch batch = db.batch();
        if (username != null && !username.isEmpty()) {
            batch.delete(db.collection("publicProfiles").document(username));
            DocumentSnapshot claim = Await.of(db.collection("usernames").document(username).get());
            if (claim.exists() && uid.equals(claim.getString("uid"))) {
                batch.delete(claim.getReference());
            }
        }
        batch.delete(profileRef);
        batch.delete(db.collection("users").document(uid));
        Await.of(batch.commit());

        storage.bucket().ifPresent(bucket -> {
            try {
                for (Blob blob : bucket.list(Storage.BlobListOption.prefix("users/" + uid + "/"))
                        .iterateAll()) {
                    blob.delete();
                }
            } catch (RuntimeException e) {
                throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_UNAVAILABLE",
                        "Could not remove your files. Please try again.");
            }
        });

        try {
            auth.deleteUser(uid);
        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) {
                throw new AppException(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_UNAVAILABLE",
                        "Could not remove your login. Please try again.");
            }
        }
    }

    private void deleteAll(CollectionReference collection) {
        List<DocumentReference> refs = new ArrayList<>();
        collection.listDocuments().forEach(refs::add);
        for (int i = 0; i < refs.size(); i += 400) {
            WriteBatch batch = db.batch();
            for (DocumentReference ref : refs.subList(i, Math.min(i + 400, refs.size()))) {
                batch.delete(ref);
            }
            Await.of(batch.commit());
        }
    }
}