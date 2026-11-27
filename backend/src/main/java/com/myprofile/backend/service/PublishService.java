package com.myprofile.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.myprofile.backend.config.AppProperties;
import com.myprofile.backend.dto.PublishResponse;
import com.myprofile.backend.exception.AppException;
import com.myprofile.backend.security.UsernamePolicy;
import com.myprofile.backend.util.Await;

@Service
public class PublishService {

    private final Firestore db;
    private final AppProperties props;

    public PublishService(Firestore db, AppProperties props) {
        this.db = db;
        this.props = props;
    }

    public PublishResponse publish(String uid, String username) {
        if (!UsernamePolicy.isValid(username)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "USERNAME_INVALID",
                    "Use 3 to 30 lowercase letters, numbers or hyphens");
        }
        DocumentReference profileRef = db.collection("profiles").document(uid);
        DocumentSnapshot profile = Await.of(profileRef.get());
        if (!profile.exists()) {
            throw new AppException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND",
                    "Create your profile first");
        }
        if ("suspended".equals(profile.getString("status"))) {
            throw new AppException(HttpStatus.FORBIDDEN, "PROFILE_SUSPENDED",
                    "This profile is suspended");
        }
        String existing = profile.getString("username");
        if (existing != null && !existing.isEmpty() && !existing.equals(username)) {
            throw new AppException(HttpStatus.CONFLICT, "USERNAME_LOCKED",
                    "Your profile link cannot change after you publish");
        }

        claimUsername(uid, username);

        Map<String, Object> snapshot = buildSnapshot(uid, profileRef, profile);
        snapshot.put("username", username);
        snapshot.put("updatedAt", FieldValue.serverTimestamp());

        Map<String, Object> update = new HashMap<>();
        update.put("username", username);
        update.put("status", "published");
        update.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.set(db.collection("publicProfiles").document(username), snapshot);
        batch.update(profileRef, update);
        Await.of(batch.commit());

        return new PublishResponse(username, props.publicBaseUrl() + "/" + username, "published");
    }

    public void unpublish(String uid) {
        DocumentReference profileRef = db.collection("profiles").document(uid);
        DocumentSnapshot profile = Await.of(profileRef.get());
        if (!profile.exists()) {
            throw new AppException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile not found");
        }
        String username = profile.getString("username");
        WriteBatch batch = db.batch();
        if (username != null && !username.isEmpty()) {
            batch.delete(db.collection("publicProfiles").document(username));
        }
        if (!"suspended".equals(profile.getString("status"))) {
            Map<String, Object> update = new HashMap<>();
            update.put("status", "unpublished");
            update.put("updatedAt", FieldValue.serverTimestamp());
            batch.update(profileRef, update);
        }
        Await.of(batch.commit());
    }

    private void claimUsername(String uid, String username) {
        DocumentReference ref = db.collection("usernames").document(username);
        Await.of(db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(ref).get();
            if (snap.exists()) {
                if (!uid.equals(snap.getString("uid"))) {
                    throw new AppException(HttpStatus.CONFLICT, "USERNAME_TAKEN",
                            "That name is already taken");
                }
            } else {
                Map<String, Object> claim = new HashMap<>();
                claim.put("uid", uid);
                claim.put("createdAt", FieldValue.serverTimestamp());
                tx.set(ref, claim);
            }
            return null;
        }));
    }

    private Map<String, Object> buildSnapshot(String uid, DocumentReference profileRef,
                                              DocumentSnapshot profile) {
        ApiFuture<DocumentSnapshot> privacy = profileRef.collection("privacy").document("settings").get();
        ApiFuture<DocumentSnapshot> contact = profileRef.collection("contact").document("main").get();
        ApiFuture<DocumentSnapshot> user = db.collection("users").document(uid).get();
        ApiFuture<QuerySnapshot> education = list(profileRef, "education");
        ApiFuture<QuerySnapshot> experience = list(profileRef, "experience");
        ApiFuture<QuerySnapshot> services = list(profileRef, "services");
        ApiFuture<QuerySnapshot> achievements = list(profileRef, "achievements");
        ApiFuture<QuerySnapshot> social = list(profileRef, "socialLinks");
        ApiFuture<QuerySnapshot> photos = list(profileRef, "photos");

        DocumentSnapshot userDoc = Await.of(user);
        boolean minor = Boolean.TRUE.equals(userDoc.getBoolean("isMinor"));

        SnapshotBuilder.Input input = new SnapshotBuilder.Input(
                data(profile), data(Await.of(privacy)), data(Await.of(contact)), minor,
                rows(Await.of(education)), rows(Await.of(experience)),
                rows(Await.of(services)), rows(Await.of(achievements)),
                rows(Await.of(social)), rows(Await.of(photos)));
        return new java.util.LinkedHashMap<>(SnapshotBuilder.build(input));
    }

    private ApiFuture<QuerySnapshot> list(DocumentReference profileRef, String name) {
        return profileRef.collection(name).orderBy("createdAt", Query.Direction.DESCENDING).get();
    }

    private Map<String, Object> data(DocumentSnapshot snap) {
        return snap.exists() && snap.getData() != null ? snap.getData() : Map.of();
    }

    private List<Map<String, Object>> rows(QuerySnapshot snap) {
        return snap.getDocuments().stream()
                .map(DocumentSnapshot::getData)
                .filter(Objects::nonNull)
                .toList();
    }
}