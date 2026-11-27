package com.myprofile.backend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.myprofile.backend.exception.AppException;
import com.myprofile.backend.util.Await;

@Service
public class ReportService {

    public static final Set<String> REASONS = Set.of(
            "Fake or impersonation", "Inappropriate content", "Spam",
            "Personal information exposed", "Other");

    private final Firestore db;

    public ReportService(Firestore db) {
        this.db = db;
    }

    public void create(String username, String reason, String message) {
        if (!REASONS.contains(reason)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "REASON_INVALID", "Choose a valid reason");
        }
        DocumentSnapshot pub = Await.of(db.collection("publicProfiles").document(username).get());
        if (!pub.exists()) {
            throw new AppException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "Profile not found");
        }
        Map<String, Object> doc = new HashMap<>();
        doc.put("username", username);
        doc.put("profileId", pub.getString("ownerUid"));
        doc.put("reason", reason);
        doc.put("message", SnapshotBuilder.clean(message, 300));
        doc.put("status", "open");
        doc.put("createdAt", FieldValue.serverTimestamp());
        Await.of(db.collection("reports").add(doc));
    }
}