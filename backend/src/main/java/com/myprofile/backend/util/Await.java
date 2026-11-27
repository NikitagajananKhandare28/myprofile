package com.myprofile.backend.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.google.api.core.ApiFuture;
import com.myprofile.backend.exception.AppException;

public final class Await {

    private static final Logger log = LoggerFactory.getLogger(Await.class);

    private Await() {
    }

    public static <T> T of(ApiFuture<T> future) {
        try {
            return future.get(25, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (ExecutionException e) {
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof AppException app) {
                    throw app;
                }
                cause = cause.getCause();
            }
            throw unavailable(e);
        } catch (TimeoutException e) {
            throw unavailable(e);
        }
    }

    private static AppException unavailable(Exception e) {
        log.error("Backend call failed", e);
        return new AppException(HttpStatus.SERVICE_UNAVAILABLE, "UNAVAILABLE",
                "The service is busy. Please try again.");
    }
}