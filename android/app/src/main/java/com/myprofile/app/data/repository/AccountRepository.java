package com.myprofile.app.data.repository;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class AccountRepository {

    public interface ResultCallback {
        void onSuccess();

        void onError(@NonNull Exception e);
    }

    private static final String[] SECTIONS = {
            "education", "experience", "services", "achievements", "socialLinks"
    };

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void deleteAll(@NonNull String uid, String username,
                          @NonNull ResultCallback callback) {
        DocumentReference profile = db.collection("profiles").document(uid);

        List<Task<?>> tasks = new ArrayList<>();
        for (String name : SECTIONS) {
            tasks.add(profile.collection(name).get());
        }

        Tasks.<Object>whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    WriteBatch batch = db.batch();
                    for (Object result : results) {
                        for (DocumentSnapshot d : ((QuerySnapshot) result).getDocuments()) {
                            batch.delete(d.getReference());
                        }
                    }
                    batch.delete(profile.collection("contact").document("main"));
                    batch.delete(profile.collection("privacy").document("settings"));
                    if (username != null && !username.isEmpty()) {
                        batch.delete(db.collection("publicProfiles").document(username));
                        batch.delete(db.collection("usernames").document(username));
                    }
                    batch.delete(profile);
                    batch.delete(db.collection("users").document(uid));
                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }
}