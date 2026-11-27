package com.myprofile.backend.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

/**
 * Credentials come from Application Default Credentials: the
 * GOOGLE_APPLICATION_CREDENTIALS file locally, and the attached service
 * account on Cloud Run. No secret is ever stored in this repository.
 */
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(AppProperties props) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(props.firebase().projectId());
        String bucketName = props.firebase().storageBucket();
        if (bucketName != null && !bucketName.isBlank()) {
            builder.setStorageBucket(bucketName);
        }
        return FirebaseApp.initializeApp(builder.build());
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    @Bean
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }

    @Bean
    public StorageAccess storageAccess(FirebaseApp app) {
        return new StorageAccess(app);
    }
}