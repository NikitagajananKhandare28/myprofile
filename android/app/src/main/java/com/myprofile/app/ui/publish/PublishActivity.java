package com.myprofile.app.ui.publish;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.myprofile.app.R;
import com.myprofile.app.data.repository.ProfileRepository;
import com.myprofile.app.data.repository.PublishRepository;
import com.myprofile.app.data.repository.ServerPublishRepository;
import com.myprofile.app.databinding.ActivityPublishBinding;
import com.myprofile.app.ui.preview.PreviewActivity;
import com.myprofile.app.ui.share.ShareActivity;
import com.myprofile.app.ui.welcome.WelcomeActivity;
import com.myprofile.app.util.AppConfig;
import com.myprofile.app.util.UsernameUtils;

public class PublishActivity extends AppCompatActivity {

    private static final String TAG = "Publish";

    private ActivityPublishBinding binding;
    private final ProfileRepository profileRepository = new ProfileRepository();
    private final PublishRepository publishRepository = new PublishRepository();
    private FirebaseUser user;
    private String savedUsername = "";
    private boolean loaded = false;
    private ServerPublishRepository serverRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPublishBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        serverRepository = new ServerPublishRepository(this);
        binding.tilUsername.setPrefixText(AppConfig.PUBLIC_HOST + "/");
        binding.btnPublish.setOnClickListener(v -> onPublishClicked());
        binding.btnUnpublish.setOnClickListener(v -> onUnpublishClicked());
        binding.btnPreview.setOnClickListener(v ->
                startActivity(new Intent(this, PreviewActivity.class)));
        binding.btnShare.setOnClickListener(v ->
                startActivity(new Intent(this, ShareActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user != null) {
            loadState();
        }
    }

    private void loadState() {
        setLoading(true);
        profileRepository.getProfile(user.getUid(), new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(DocumentSnapshot snapshot) {
                setLoading(false);
                if (snapshot == null || !snapshot.exists()) {
                    showMessage(getString(R.string.err_load));
                    return;
                }
                String status = snapshot.getString("status");
                String name = snapshot.getString("username");
                savedUsername = name == null ? "" : name;

                if (!savedUsername.isEmpty()) {
                    binding.etUsername.setText(savedUsername);
                    binding.etUsername.setEnabled(false);
                } else if (binding.etUsername.getText() == null
                        || binding.etUsername.getText().length() == 0) {
                    binding.etUsername.setText(
                            UsernameUtils.suggestFrom(snapshot.getString("displayName")));
                }

                boolean published = "published".equals(status);
                if (published) {
                    binding.tvStatus.setText(R.string.home_status_published);
                    binding.btnPublish.setText(R.string.btn_publish_update);
                } else if ("unpublished".equals(status)) {
                    binding.tvStatus.setText(R.string.home_status_unpublished);
                    binding.btnPublish.setText(R.string.btn_publish);
                } else {
                    binding.tvStatus.setText(R.string.home_status_draft);
                    binding.btnPublish.setText(R.string.btn_publish);
                }
                binding.btnUnpublish.setVisibility(published ? View.VISIBLE : View.GONE);
                binding.btnShare.setVisibility(published ? View.VISIBLE : View.GONE);
                loaded = true;
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Load failed", e);
                setLoading(false);
                showMessage(getString(R.string.err_load));
            }
        });
    }

    private void onPublishClicked() {
        if (!loaded) {
            return;
        }
        binding.tilUsername.setError(null);

        String username = !savedUsername.isEmpty()
                ? savedUsername
                : UsernameUtils.normalize(textOf(binding.etUsername.getText()));
        if (!UsernameUtils.isValid(username)) {
            binding.tilUsername.setError(getString(R.string.err_username_invalid));
            return;
        }

        setLoading(true);
        serverRepository.publish(user.getUid(), username, new PublishRepository.ResultCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                showMessage(getString(R.string.msg_published));
                loadState();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Publish failed", e);
                setLoading(false);
                if (e instanceof IllegalStateException
                        && "USERNAME_TAKEN".equals(e.getMessage())) {
                    binding.tilUsername.setError(getString(R.string.err_username_taken,
                            String.join(", ", UsernameUtils.alternatives(username))));
                } else {
                    showMessage(getString(R.string.err_publish));
                }
            }
        });
    }

    private void onUnpublishClicked() {
        if (savedUsername.isEmpty()) {
            return;
        }
        setLoading(true);
        serverRepository.unpublish(user.getUid(), savedUsername,
                new PublishRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        showMessage(getString(R.string.msg_unpublished));
                        loadState();
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.e(TAG, "Unpublish failed", e);
                        setLoading(false);
                        showMessage(getString(R.string.err_publish));
                    }
                });
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnPublish.setEnabled(!loading);
        binding.btnUnpublish.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}




