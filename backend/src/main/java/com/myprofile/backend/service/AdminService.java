package com.myprofile.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteBatch;
import com.myprofile.backend.exception.AppException;
import com.myprofile.backend.security.UsernamePolicy;
import com.myprofile.backend.util.Await;

@Service
public class AdminService {

    private final Firestore db;

    public AdminService(Firestore db) {
        this.db = db;
    }

    public void takedown(String reportId, String adminUid) {
        DocumentReference reportRef = db.collection("reports").document(reportId);
        DocumentSnapshot report = openReport(reportRef);
        String username = report.getString("username");

        DocumentReference publicRef = db.collection("publicProfiles").document(username);
        DocumentSnapshot pub = Await.of(publicRef.get());
        String ownerUid = pub.exists() ? pub.getString("ownerUid") : report.getString("profileId");

        WriteBatch batch = db.batch();
        if (pub.exists()) {
            batch.delete(publicRef);
        }
        if (ownerUid != null && !ownerUid.isBlank()) {
            DocumentReference profile = db.collection("profiles").document(ownerUid);
            if (Await.of(profile.get()).exists()) {
                batch.update(profile, status("suspended"));
            }
        }
        batch.update(reportRef, review("actioned", "taken_down", adminUid));
        audit(batch, adminUid, "takedown", username, reportId);
        Await.of(batch.commit());
    }

    public void dismiss(String reportId, String adminUid) {
        DocumentReference reportRef = db.collection("reports").document(reportId);
        DocumentSnapshot report = openReport(reportRef);
        WriteBatch batch = db.batch();
        batch.update(reportRef, review("dismissed", "none", adminUid));
        audit(batch, adminUid, "dismiss", report.getString("username"), reportId);
        Await.of(batch.commit());
    }

    public void reinstate(String username, String adminUid) {
        if (!UsernamePolicy.isValid(username)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "USERNAME_INVALID", "Invalid profile name");
        }
        DocumentSnapshot claim = Await.of(db.collection("usernames").document(username).get());
        if (!claim.exists()) {
            throw new AppException(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "No profile with that name");
        }
        DocumentReference profile = db.collection("profiles").document(claim.getString("uid"));
        DocumentSnapshot snap = Await.of(profile.get());
        if (!snap.exists() || !"suspended".equals(snap.getString("status"))) {
            throw new AppException(HttpStatus.CONFLICT, "NOT_SUSPENDED", "That profile is not suspended");
        }
        WriteBatch batch = db.batch();
        batch.update(profile, status("unpublished"));
        audit(batch, adminUid, "reinstate", username, "");
        Await.of(batch.commit());
    }

    private DocumentSnapshot openReport(DocumentReference ref) {
        DocumentSnapshot report = Await.of(ref.get());
        if (!report.exists()) {
            throw new AppException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Report not found");
        }
        if (!"open".equals(report.getString("status"))) {
            throw new AppException(HttpStatus.CONFLICT, "REPORT_CLOSED", "This report is already handled");
        }
        return report;
    }

    private Map<String, Object> status(String value) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", value);
        m.put("updatedAt", FieldValue.serverTimestamp());
        return m;
    }

    private Map<String, Object> review(String status, String action, String adminUid) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", status);
        m.put("action", action);
        m.put("reviewedBy", adminUid);
        m.put("reviewedAt", FieldValue.serverTimestamp());
        return m;
    }

    private void audit(WriteBatch batch, String actor, String action, String target, String reportId) {
        Map<String, Object> log = new HashMap<>();
        log.put("actorUid", actor);
        log.put("action", action);
        log.put("target", target == null ? "" : target);
        log.put("reportId", reportId);
        log.put("createdAt", FieldValue.serverTimestamp());
        batch.set(db.collection("auditLogs").document(), log);
    }
}