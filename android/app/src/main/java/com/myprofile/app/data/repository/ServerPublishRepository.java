package com.myprofile.app.data.repository;

import android.content.Context;

import com.myprofile.app.data.remote.ApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ServerPublishRepository {

    private final Context context;

    public ServerPublishRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void publish(String uid, String username, PublishRepository.ResultCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("username", username);
        } catch (JSONException e) {
            callback.onError(e);
            return;
        }
        ApiClient.request(context, "POST", "/api/v1/profile/publish", body, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                callback.onSuccess();
            }

            @Override
            public void onError(int httpStatus, String code, String message) {
                if ("USERNAME_TAKEN".equals(code)) {
                    callback.onError(new IllegalStateException("USERNAME_TAKEN"));
                } else {
                    callback.onError(new IOException(code + ": " + message));
                }
            }
        });
    }

    public void unpublish(String uid, String username, PublishRepository.ResultCallback callback) {
        ApiClient.request(context, "POST", "/api/v1/profile/unpublish", new JSONObject(),
                new ApiClient.Callback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onError(int httpStatus, String code, String message) {
                        callback.onError(new IOException(code + ": " + message));
                    }
                });
    }
}
