package com.myprofile.backend.config;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;

/**
 * Cloud Storage is optional. If the project has no bucket yet, the backend
 * still starts, and file cleanup is skipped until Storage is set up.
 */
public class StorageAccess {

    private static final Logger log = LoggerFactory.getLogger(StorageAccess.class);

    private final FirebaseApp app;
    private volatile Bucket bucket;

    public StorageAccess(FirebaseApp app) {
        this.app = app;
    }

    public Optional<Bucket> bucket() {
        if (bucket == null) {
            try {
                bucket = StorageClient.getInstance(app).bucket();
            } catch (IllegalArgumentException | StorageException e) {
                log.warn("Cloud Storage bucket is not available, skipping file cleanup: {}",
                        e.getMessage());
            }
        }
        return Optional.ofNullable(bucket);
    }
}