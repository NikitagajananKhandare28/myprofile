package com.myprofile.app.data.repository;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PublishRepository {

    public interface SnapshotCallback {
        void onBuilt(Map<String, Object> snapshot);

        void onError(@NonNull Exception e);
    }

    public interface ResultCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    private static final String[] SECTIONS = {
            "education", "experience", "services", "achievements", "socialLinks"
    };

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void buildSnapshot(@NonNull String uid, @NonNull SnapshotCallback callback) {
        DocumentReference profile = db.collection("profiles").document(uid);

        List<Task<?>> tasks = new ArrayList<>();
        tasks.add(profile.get());
        tasks.add(profile.collection("privacy").document("settings").get());
        tasks.add(profile.collection("contact").document("main").get());
        tasks.add(db.collection("users").document(uid).get());
        for (String name : SECTIONS) {
            tasks.add(profile.collection(name)
                    .orderBy("createdAt", Query.Direction.DESCENDING).get());
        }

        Tasks.<Object>whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    try {
                        callback.onBuilt(assemble(results));
                    } catch (Exception e) {
                        callback.onError(e);
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    private Map<String, Object> assemble(List<Object> r) {
        DocumentSnapshot profile = (DocumentSnapshot) r.get(0);
        DocumentSnapshot privacy = (DocumentSnapshot) r.get(1);
        DocumentSnapshot contact = (DocumentSnapshot) r.get(2);
        DocumentSnapshot user = (DocumentSnapshot) r.get(3);

        if (!profile.exists()) {
            throw new IllegalStateException("NO_PROFILE");
        }
        boolean minor = Boolean.TRUE.equals(user.getBoolean("isMinor"));

        Map<String, Object> out = new HashMap<>();
        out.put("ownerUid", s(profile.getString("ownerUid")));
        out.put("displayName", s(profile.getString("displayName")));
        out.put("profileType", s(profile.getString("profileType")));
        out.put("bio", s(profile.getString("bio")));

        String city = s(profile.getString("city"));
        if (isPublic(privacy, "city", false) && !city.isEmpty()) {
            out.put("city", city);
        }

        String about = s(profile.getString("aboutMe"));
        if (isPublic(privacy, "about", true) && !about.isEmpty()) {
            out.put("aboutMe", about);
        }

        Object skillsRaw = profile.get("skills");
        if (isPublic(privacy, "skills", true) && skillsRaw instanceof List) {
            List<String> skills = new ArrayList<>();
            for (Object item : (List<?>) skillsRaw) {
                if (item instanceof String) {
                    skills.add((String) item);
                }
            }
            if (!skills.isEmpty()) {
                out.put("skills", skills);
            }
        }

        putIfAny(out, "education", isPublic(privacy, "education", true),
                items((QuerySnapshot) r.get(4),
                        new String[]{"qualification", "institution", "year"}, null));
        putIfAny(out, "experience", isPublic(privacy, "experience", true),
                items((QuerySnapshot) r.get(5),
                        new String[]{"role", "organization", "duration", "description"}, null));
        putIfAny(out, "services", isPublic(privacy, "services", true),
                items((QuerySnapshot) r.get(6),
                        new String[]{"name", "price", "description"}, null));
        putIfAny(out, "achievements", isPublic(privacy, "achievements", true),
                items((QuerySnapshot) r.get(7),
                        new String[]{"title", "date", "description"}, null));
        putIfAny(out, "socialLinks", true,
                items((QuerySnapshot) r.get(8),
                        new String[]{"platform", "url"}, "visibility"));

        if (!minor && contact != null && contact.exists()) {
            Map<String, Object> c = new HashMap<>();
            for (String key : new String[]{"phone", "whatsapp", "email", "location"}) {
                String value = s(contact.getString(key));
                if ("public".equals(contact.getString(key + "Visibility")) && !value.isEmpty()) {
                    c.put(key, value);
                }
            }
            if (!c.isEmpty()) {
                out.put("contact", c);
            }
        }
        return out;
    }

    private void putIfAny(Map<String, Object> out, String key, boolean allowed,
                          List<Map<String, Object>> list) {
        if (allowed && !list.isEmpty()) {
            out.put(key, list);
        }
    }

    private List<Map<String, Object>> items(QuerySnapshot snap, String[] keys,
                                            String visibilityKey) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DocumentSnapshot d : snap.getDocuments()) {
            if (visibilityKey != null && !"public".equals(d.getString(visibilityKey))) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            for (String k : keys) {
                m.put(k, s(d.getString(k)));
            }
            list.add(m);
        }
        return list;
    }

    private boolean isPublic(DocumentSnapshot privacy, String key, boolean defaultValue) {
        if (privacy == null || !privacy.exists()) {
            return defaultValue;
        }
        String value = privacy.getString(key + "Visibility");
        return value == null ? defaultValue : "public".equals(value);
    }

    private String s(String value) {
        return value == null ? "" : value;
    }

    public void publish(@NonNull String uid, @NonNull String username,
                        @NonNull ResultCallback callback) {
        DocumentReference nameRef = db.collection("usernames").document(username);
        nameRef.get().addOnSuccessListener(doc -> {
            if (doc.exists() && !uid.equals(doc.getString("uid"))) {
                callback.onError(new IllegalStateException("USERNAME_TAKEN"));
                return;
            }
            if (doc.exists()) {
                writePublic(uid, username, callback);
                return;
            }
            Map<String, Object> claim = new HashMap<>();
            claim.put("uid", uid);
            claim.put("createdAt", FieldValue.serverTimestamp());
            nameRef.set(claim)
                    .addOnSuccessListener(unused -> writePublic(uid, username, callback))
                    .addOnFailureListener(callback::onError);
        }).addOnFailureListener(callback::onError);
    }

    private void writePublic(String uid, String username, ResultCallback callback) {
        buildSnapshot(uid, new SnapshotCallback() {
            @Override
            public void onBuilt(Map<String, Object> snapshot) {
                snapshot.put("username", username);
                snapshot.put("updatedAt", FieldValue.serverTimestamp());

                Map<String, Object> profileUpdate = new HashMap<>();
                profileUpdate.put("username", username);
                profileUpdate.put("status", "published");
                profileUpdate.put("updatedAt", FieldValue.serverTimestamp());

                WriteBatch batch = db.batch();
                batch.set(db.collection("publicProfiles").document(username), snapshot);
                batch.update(db.collection("profiles").document(uid), profileUpdate);
                batch.commit()
                        .addOnSuccessListener(unused -> callback.onSuccess())
                        .addOnFailureListener(callback::onError);
            }

            @Override
            public void onError(@NonNull Exception e) {
                callback.onError(e);
            }
        });
    }

    public void unpublish(@NonNull String uid, @NonNull String username,
                          @NonNull ResultCallback callback) {
        Map<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("status", "unpublished");
        profileUpdate.put("updatedAt", FieldValue.serverTimestamp());

        WriteBatch batch = db.batch();
        batch.delete(db.collection("publicProfiles").document(username));
        batch.update(db.collection("profiles").document(uid), profileUpdate);
        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }
}