
package com.myprofile.app.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.myprofile.app.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Calls the backend with the signed-in user's Firebase ID token. */
public final class ApiClient {

    public interface Callback {
        void onSuccess(JSONObject body);

        void onError(int httpStatus, String code, String message);
    }

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ApiClient() {
    }

    public static void request(Context context, String method, String path,
                               JSONObject body, Callback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError(401, "UNAUTHENTICATED", "Not signed in");
            return;
        }
        String base = context.getString(R.string.api_base_url);
        user.getIdToken(false)
                .addOnSuccessListener(result -> EXECUTOR.execute(
                        () -> execute(base, method, path, body, result.getToken(), callback)))
                .addOnFailureListener(e ->
                        callback.onError(401, "TOKEN_ERROR", "Could not get a sign-in token"));
    }

    private static void execute(String base, String method, String path, JSONObject body,
                                String token, Callback callback) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(base + path).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(40000);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");
            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(bytes);
                }
            }
            final int status = conn.getResponseCode();
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            final JSONObject json = parse(read(stream));
            if (status >= 200 && status < 300) {
                MAIN.post(() -> callback.onSuccess(json));
            } else {
                final String code = json.optString("code", "ERROR");
                final String message = json.optString("message", "Request failed");
                MAIN.post(() -> callback.onError(status, code, message));
            }
        } catch (IOException e) {
            MAIN.post(() -> callback.onError(0, "NETWORK", "Network error"));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static JSONObject parse(String text) {
        try {
            return text.isEmpty() ? new JSONObject() : new JSONObject(text);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
