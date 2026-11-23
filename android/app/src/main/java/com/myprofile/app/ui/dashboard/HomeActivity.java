package com.myprofile.app.ui.dashboard;

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
import com.myprofile.app.data.model.SectionType;
import com.myprofile.app.data.repository.ProfileRepository;
import com.myprofile.app.databinding.ActivityHomeBinding;
import com.myprofile.app.ui.builder.SectionListActivity;
import com.myprofile.app.ui.profile.CreateProfileActivity;
import com.myprofile.app.ui.profile.EditProfileActivity;
import com.myprofile.app.ui.welcome.WelcomeActivity;
import com.myprofile.app.util.PhoneUtils;
import com.myprofile.app.util.ProfileCompletion;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "Home";

    private ActivityHomeBinding binding;
    private final ProfileRepository repository = new ProfileRepository();
    private FirebaseUser user;

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

        binding.tvSignedIn.setText(getString(R.string.home_signed_in,
            PhoneUtils.maskPhone(user.getPhoneNumber())));
        binding.btnEditProfile.setOnClickListener(v ->
            startActivity(new Intent(this, EditProfileActivity.class)));
        binding.btnEducation.setOnClickListener(v -> openSection(SectionType.EDUCATION));
        binding.btnExperience.setOnClickListener(v -> openSection(SectionType.EXPERIENCE));
        binding.btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            openWelcome();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (user != null) {
            loadProfile();
        }
    }

    private void openSection(SectionType type) {
        Intent intent = new Intent(this, SectionListActivity.class);
        intent.putExtra(SectionListActivity.EXTRA_TYPE, type.name());
        startActivity(intent);
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
                binding.tvName.setText(snapshot.getString("displayName"));
                binding.tvType.setText(snapshot.getString("profileType"));
                binding.tvCompletion.setText(getString(R.string.home_completion, percent));
                binding.progressCompletion.setProgress(percent);
                binding.content.setVisibility(View.VISIBLE);
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

    private void openWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
