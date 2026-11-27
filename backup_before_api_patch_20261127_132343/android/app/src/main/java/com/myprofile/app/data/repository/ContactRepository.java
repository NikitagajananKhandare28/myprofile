package com.myprofile.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.myprofile.app.data.model.ContactInfo;

import java.util.HashMap;
import java.util.Map;

public class ContactRepository {

    public interface LoadCallback {
        void onLoaded(ContactInfo info);

        void onError(@NonNull Exception e);
    }

    public interface SaveCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private DocumentReference contactDoc(String uid) {
        return db.collection("profiles").document(uid).collection("contact").document("main");
    }

    public void load(@NonNull String uid, @NonNull LoadCallback callback) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    boolean minor = Boolean.TRUE.equals(userDoc.getBoolean("isMinor"));
                    contactDoc(uid).get()
                            .addOnSuccessListener(doc -> callback.onLoaded(toInfo(doc, minor)))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void save(@NonNull String uid, @NonNull ContactInfo info,
                     @NonNull SaveCallback callback) {
        boolean allowPublic = !info.minor;

        Map<String, Object> data = new HashMap<>();
        data.put("phone", info.phone);
        data.put("phoneVisibility", visibility(allowPublic && info.phonePublic));
        data.put("whatsapp", info.whatsapp);
        data.put("whatsappVisibility", visibility(allowPublic && info.whatsappPublic));
        data.put("email", info.email);
        data.put("emailVisibility", visibility(allowPublic && info.emailPublic));
        data.put("location", info.location);
        data.put("locationVisibility", visibility(allowPublic && info.locationPublic));
        data.put("updatedAt", FieldValue.serverTimestamp());

        contactDoc(uid).set(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private ContactInfo toInfo(DocumentSnapshot doc, boolean minor) {
        ContactInfo info = new ContactInfo();
        info.minor = minor;
        if (doc != null && doc.exists()) {
            info.phone = safe(doc.getString("phone"));
            info.whatsapp = safe(doc.getString("whatsapp"));
            info.email = safe(doc.getString("email"));
            info.location = safe(doc.getString("location"));
            info.phonePublic = "public".equals(doc.getString("phoneVisibility"));
            info.whatsappPublic = "public".equals(doc.getString("whatsappVisibility"));
            info.emailPublic = "public".equals(doc.getString("emailVisibility"));
            info.locationPublic = "public".equals(doc.getString("locationVisibility"));
        }
        return info;
    }

    private String visibility(boolean isPublic) {
        return isPublic ? "public" : "private";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}