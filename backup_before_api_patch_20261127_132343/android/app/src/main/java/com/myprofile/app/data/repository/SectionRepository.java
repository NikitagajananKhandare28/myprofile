package com.myprofile.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.myprofile.app.data.model.SectionItem;
import com.myprofile.app.data.model.SectionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SectionRepository {

    public interface ListCallback {
        void onLoaded(List<SectionItem> items);

        void onError(@NonNull Exception e);
    }

    public interface ResultCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private CollectionReference collection(String uid, SectionType type) {
        return db.collection("profiles").document(uid).collection(type.collection);
    }

    public void list(@NonNull String uid, @NonNull SectionType type,
                     @NonNull ListCallback callback) {
        collection(uid, type)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<SectionItem> items = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String visibility = "";
                        if (type.hasVisibility()) {
                            visibility = doc.getString(type.visibilityKey);
                            if (visibility == null) {
                                visibility = "private";
                            }
                        }
                        items.add(new SectionItem(
                                doc.getId(),
                                doc.getString(type.titleKey),
                                type.hasSubtitle() ? doc.getString(type.subtitleKey) : "",
                                type.hasPeriod() ? doc.getString(type.periodKey) : "",
                                type.hasDescription() ? doc.getString(type.descriptionKey) : "",
                                visibility));
                    }
                    callback.onLoaded(items);
                })
                .addOnFailureListener(callback::onError);
    }

    public void add(@NonNull String uid, @NonNull SectionType type,
                    @NonNull SectionItem item, @NonNull ResultCallback callback) {
        Map<String, Object> data = toMap(type, item);
        data.put("createdAt", FieldValue.serverTimestamp());
        data.put("updatedAt", FieldValue.serverTimestamp());
        collection(uid, type).add(data)
                .addOnSuccessListener(ref -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void update(@NonNull String uid, @NonNull SectionType type,
                       @NonNull SectionItem item, @NonNull ResultCallback callback) {
        Map<String, Object> data = toMap(type, item);
        data.put("updatedAt", FieldValue.serverTimestamp());
        collection(uid, type).document(item.getId()).update(data)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    public void delete(@NonNull String uid, @NonNull SectionType type,
                       @NonNull String itemId, @NonNull ResultCallback callback) {
        collection(uid, type).document(itemId).delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    private Map<String, Object> toMap(SectionType type, SectionItem item) {
        Map<String, Object> data = new HashMap<>();
        data.put(type.titleKey, item.getTitle());
        if (type.hasSubtitle()) {
            data.put(type.subtitleKey, item.getSubtitle());
        }
        if (type.hasPeriod()) {
            data.put(type.periodKey, item.getPeriod());
        }
        if (type.hasDescription()) {
            data.put(type.descriptionKey, item.getDescription());
        }
        if (type.hasVisibility()) {
            data.put(type.visibilityKey, item.getVisibility());
        }
        return data;
    }
}