package com.myprofile.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.myprofile.app.R;
import com.myprofile.app.data.repository.ProfileRepository;
import com.myprofile.app.databinding.ActivityCreateProfileBinding;
import com.myprofile.app.ui.dashboard.HomeActivity;
import com.myprofile.app.ui.welcome.WelcomeActivity;
import com.myprofile.app.util.ProfileTypes;

public class CreateProfileActivity extends AppCompatActivity {

    private static final String TAG = "CreateProfile";

    private ActivityCreateProfileBinding binding;
    private final ProfileRepository repository = new ProfileRepository();
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding.actvType.setAdapter(new ArrayAdapter<>(
            this, android.R.layout.simple_list_item_1, ProfileTypes.LABELS));

        binding.cbMinor.setOnCheckedChangeListener((button, checked) -> {
            binding.cbGuardian.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (!checked) {
                binding.cbGuardian.setChecked(false);
            }
        });

        binding.btnContinue.setOnClickListener(v -> onContinueClicked());
    }

    private void onContinueClicked() {
        binding.tilName.setError(null);
        binding.tilType.setError(null);

        String name = textOf(binding.etName.getText());
        String type = textOf(binding.actvType.getText());
        String city = textOf(binding.etCity.getText());
        String bio = textOf(binding.etBio.getText());
        boolean isMinor = binding.cbMinor.isChecked();
        boolean guardian = binding.cbGuardian.isChecked();

        if (name.length() < 2) {
            binding.tilName.setError(getString(R.string.err_name));
            return;
        }
        if (!ProfileTypes.isValid(type)) {
            binding.tilType.setError(getString(R.string.err_type));
            return;
        }
        if (isMinor && !guardian) {
            showError(getString(R.string.err_guardian));
            return;
        }

        setLoading(true);
        repository.createProfile(user, name, type, city, bio, isMinor, guardian,
            new ProfileRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    Intent intent = new Intent(CreateProfileActivity.this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(@NonNull Exception e) {
                    Log.e(TAG, "Save failed", e);
                    setLoading(false);
                    showError(getString(R.string.err_save));
                }
            });
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnContinue.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}
