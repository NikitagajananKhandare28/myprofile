package com.myprofile.app.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.firestore.DocumentSnapshot;
import com.myprofile.app.R;
import com.myprofile.app.data.remote.ApiClient;
import com.myprofile.app.data.repository.ProfileRepository;
import com.myprofile.app.data.repository.PublishRepository;
import com.myprofile.app.data.repository.ServerPublishRepository;
import com.myprofile.app.databinding.ActivityHomeBinding;
import com.myprofile.app.ui.builder.ProfileBuilderActivity;
import com.myprofile.app.ui.preview.PreviewActivity;
import com.myprofile.app.ui.profile.CreateProfileActivity;
import com.myprofile.app.ui.publish.PublishActivity;
import com.myprofile.app.ui.share.ShareActivity;
import com.myprofile.app.ui.welcome.WelcomeActivity;
import com.myprofile.app.util.PhoneUtils;
import com.myprofile.app.util.ProfileCompletion;

import org.json.JSONObject;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "Home";
    private static final long RECENT_LOGIN_MS = 5L * 60L * 1000L;

    private ActivityHomeBinding binding;
    private final ProfileRepository repository = new ProfileRepository();
    private ServerPublishRepository serverRepository;
    private FirebaseUser user;
    private String username = "";
    private boolean pendingSync = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            openWelcome();
            return;
        }
        serverRepository = new ServerPublishRepository(this);

        binding.tvSignedIn.setText(getString(R.string.home_signed_in,
                PhoneUtils.maskPhone(user.getPhoneNumber())));
        binding.btnBuilder.setOnClickListener(v -> {
            pendingSync = true;
            startActivity(new Intent(this, ProfileBuilderActivity.class));
        });
        binding.btnPreview.setOnClickListener(v ->
                startActivity(new Intent(this, PreviewActivity.class)));
        binding.btnPublish.setOnClickListener(v ->
                startActivity(new Intent(this, PublishActivity.class)));
        binding.btnShare.setOnClickListener(v ->
                startActivity(new Intent(this, ShareActivity.class)));
        binding.btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            openWelcome();
        });
        binding.btnDeleteAccount.setOnClickListener(v -> confirmDelete());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user != null) {
            loadProfile();
        }
    }

    private void loadProfile() {
        binding.progress.setVisibility(View.VISIBLE);

        repository.getProfile(user.getUid(), new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(DocumentSnapshot snapshot) {
                binding.progress.setVisibility(View.GONE);
                if (snapshot == null || !snapshot.exists()) {
                    Intent intent = new Intent(HomeActivity.this, CreateProfileActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }
                int percent = ProfileCompletion.percent(snapshot);
                String saved = snapshot.getString("username");
                username = saved == null ? "" : saved;
                String status = snapshot.getString("status");

                binding.tvName.setText(snapshot.getString("displayName"));
                binding.tvType.setText(snapshot.getString("profileType"));
                binding.tvCompletion.setText(getString(R.string.home_completion, percent));
                binding.progressCompletion.setProgress(percent);

                if ("published".equals(status)) {
                    binding.tvStatus.setText(R.string.home_status_published);
                    binding.btnShare.setVisibility(View.VISIBLE);
                } else if ("unpublished".equals(status)) {
                    binding.tvStatus.setText(R.string.home_status_unpublished);
                    binding.btnShare.setVisibility(View.GONE);
                } else {
                    binding.tvStatus.setText(R.string.home_status_draft);
                    binding.btnShare.setVisibility(View.GONE);
                }
                binding.content.setVisibility(View.VISIBLE);

                if (pendingSync && "published".equals(status) && !username.isEmpty()) {
                    pendingSync = false;
                    syncPublicPage();
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Load failed", e);
                binding.progress.setVisibility(View.GONE);
                binding.content.setVisibility(View.VISIBLE);
                Snackbar.make(binding.getRoot(), R.string.err_load, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.btn_retry, v -> loadProfile())
                        .show();
            }
        });
    }

    private void syncPublicPage() {
        Snackbar.make(binding.getRoot(), R.string.sync_updating, Snackbar.LENGTH_SHORT).show();
        serverRepository.publish(user.getUid(), username, new PublishRepository.ResultCallback() {
            @Override
            public void onSuccess() {
                Snackbar.make(binding.getRoot(), R.string.sync_done, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Sync failed", e);
                Snackbar.make(binding.getRoot(), R.string.sync_failed, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_account_title))
                .setMessage(getString(R.string.delete_account_message))
                .setPositiveButton(R.string.btn_delete_account, (dialog, which) -> startDelete())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void startDelete() {
        FirebaseUserMetadata meta = user.getMetadata();
        long lastSignIn = meta == null ? 0L : meta.getLastSignInTimestamp();
        if (System.currentTimeMillis() - lastSignIn > RECENT_LOGIN_MS) {
            askRelogin();
            return;
        }

        binding.progress.setVisibility(View.VISIBLE);
        ApiClient.request(this, "DELETE", "/api/v1/account", null, new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject body) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(HomeActivity.this, R.string.msg_account_deleted,
                        Toast.LENGTH_LONG).show();
                openWelcome();
            }

            @Override
            public void onError(int httpStatus, String code, String message) {
                Log.e(TAG, "Delete failed: " + code);
                binding.progress.setVisibility(View.GONE);
                if ("REAUTH_REQUIRED".equals(code)) {
                    askRelogin();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.err_save, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void askRelogin() {
        Toast.makeText(this, R.string.err_relogin, Toast.LENGTH_LONG).show();
        FirebaseAuth.getInstance().signOut();
        openWelcome();
    }

    private void openWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
