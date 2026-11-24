package com.myprofile.app.ui.builder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.myprofile.app.R;
import com.myprofile.app.data.repository.PrivacyRepository;
import com.myprofile.app.databinding.ActivityPrivacyBinding;
import com.myprofile.app.ui.welcome.WelcomeActivity;

import java.util.HashMap;
import java.util.Map;

public class PrivacyActivity extends AppCompatActivity {

    private static final String TAG = "Privacy";

    private ActivityPrivacyBinding binding;
    private final PrivacyRepository repository = new PrivacyRepository();
    private FirebaseUser user;
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPrivacyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding.btnSave.setOnClickListener(v -> onSaveClicked());
        load();
    }

    private MaterialSwitch[] switches() {
        return new MaterialSwitch[]{
                binding.swCity, binding.swAbout, binding.swSkills, binding.swEducation,
                binding.swExperience, binding.swServices, binding.swAchievements
        };
    }

    private void load() {
        setLoading(true);
        repository.load(user.getUid(), new PrivacyRepository.LoadCallback() {
            @Override
            public void onLoaded(Map<String, Boolean> publicFlags) {
                setLoading(false);
                MaterialSwitch[] all = switches();
                for (int i = 0; i < PrivacyRepository.KEYS.length; i++) {
                    all[i].setChecked(Boolean.TRUE.equals(
                            publicFlags.get(PrivacyRepository.KEYS[i])));
                }
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

    private void onSaveClicked() {
        if (!loaded) {
            return;
        }
        Map<String, Boolean> flags = new HashMap<>();
        MaterialSwitch[] all = switches();
        for (int i = 0; i < PrivacyRepository.KEYS.length; i++) {
            flags.put(PrivacyRepository.KEYS[i], all[i].isChecked());
        }

        setLoading(true);
        repository.save(user.getUid(), flags, new PrivacyRepository.SaveCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                showMessage(getString(R.string.msg_saved));
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Save failed", e);
                setLoading(false);
                showMessage(getString(R.string.err_save));
            }
        });
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}