package com.myprofile.app.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.myprofile.app.data.model.Photo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PhotoRepository {

    public interface UrlCallback {
        void onSuccess(String url);

        void onError(@NonNull Exception e);
    }

    public interface ResultCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    public interface ListCallback {
        void onLoaded(List<Photo> photos);

        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private CollectionReference photosRef(String uid) {
        return db.collection("profiles").document(uid).collection("photos");
    }

    private StorageMetadata jpeg() {
        return new StorageMetadata.Builder().setContentType("image/jpeg").build();
    }

    public void uploadAvatar(@NonNull String uid, @NonNull byte[] data,
                             @NonNull UrlCallback callback) {
        StorageReference ref = storage.getReference().child("users/" + uid + "/avatar.jpg");
        ref.putBytes(data, jpeg())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        throw e != null ? e : new IOException("Upload failed");
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    Map<String, Object> update = new HashMap<>();
                    update.put("photoUrl", url);
                    update.put("updatedAt", FieldValue.serverTimestamp());
                    db.collection("profiles").document(uid).update(update)
                            .addOnSuccessListener(unused -> callback.onSuccess(url))
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void removeAvatar(@NonNull String uid, @NonNull ResultCallback callback) {
        storage.getReference().child("users/" + uid + "/avatar.jpg").delete()
                .addOnCompleteListener(done -> {
                    Map<String, Object> update = new HashMap<>();
                    update.put("photoUrl", FieldValue.delete());
                    update.put("updatedAt", FieldValue.serverTimestamp());
                    db.collection("profiles").document(uid).update(update)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                });
    }

    public void addPhoto(@NonNull String uid, @NonNull byte[] data, @NonNull String caption,
                         @NonNull ResultCallback callback) {
        String id = UUID.randomUUID().toString();
        String path = "users/" + uid + "/gallery/" + id + ".jpg";
        StorageReference ref = storage.getReference().child(path);
        ref.putBytes(data, jpeg())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception e = task.getException();
                        throw e != null ? e : new IOException("Upload failed");
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("url", uri.toString());
                    doc.put("path", path);
                    doc.put("caption", caption);
                    doc.put("createdAt", FieldValue.serverTimestamp());
                    doc.put("updatedAt", FieldValue.serverTimestamp());
                    photosRef(uid).document(id).set(doc)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    public void deletePhoto(@NonNull String uid, @NonNull Photo photo,
                            @NonNull ResultCallback callback) {
        Runnable removeDoc = () -> photosRef(uid).document(photo.getId()).delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
        if (photo.getPath().isEmpty()) {
            removeDoc.run();
            return;
        }
        storage.getReference().child(photo.getPath()).delete()
                .addOnCompleteListener(done -> removeDoc.run());
    }

    public void listPhotos(@NonNull String uid, @NonNull ListCallback callback) {
        photosRef(uid).orderBy("createdAt", Query.Direction.DESCENDING).get()
                .addOnSuccessListener(snapshot -> {
                    List<Photo> list = new ArrayList<>();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        list.add(new Photo(d.getId(), d.getString("url"),
                                d.getString("path"), d.getString("caption")));
                    }
                    callback.onLoaded(list);
                })
                .addOnFailureListener(callback::onError);
    }
}