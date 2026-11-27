package com.myprofile.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.myprofile.app.R;
import com.myprofile.app.data.repository.ProfileRepository;
import com.myprofile.app.databinding.ActivityEditProfileBinding;
import com.myprofile.app.ui.welcome.WelcomeActivity;
import com.myprofile.app.util.ProfileTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfile";
    private static final int MAX_SKILLS = 20;
    private static final int MAX_SKILL_LENGTH = 30;

    private ActivityEditProfileBinding binding;
    private final ProfileRepository repository = new ProfileRepository();
    private final List<String> skills = new ArrayList<>();
    private FirebaseUser user;
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
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

        binding.btnAddSkill.setOnClickListener(v -> addSkillFromInput());
        binding.etSkill.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addSkillFromInput();
                return true;
            }
            return false;
        });
        binding.btnSave.setOnClickListener(v -> onSaveClicked());

        loadProfile();
    }

    private void loadProfile() {
        setLoading(true);
        repository.getProfile(user.getUid(), new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(DocumentSnapshot snapshot) {
                setLoading(false);
                if (snapshot == null || !snapshot.exists()) {
                    showError(getString(R.string.err_load));
                    return;
                }
                binding.etName.setText(snapshot.getString("displayName"));
                binding.actvType.setText(snapshot.getString("profileType"), false);
                binding.etCity.setText(snapshot.getString("city"));
                binding.etBio.setText(snapshot.getString("bio"));
                binding.etAbout.setText(snapshot.getString("aboutMe"));

                skills.clear();
                binding.chipGroupSkills.removeAllViews();
                Object stored = snapshot.get("skills");
                if (stored instanceof List) {
                    for (Object item : (List<?>) stored) {
                        if (item instanceof String) {
                            skills.add((String) item);
                            addChip((String) item);
                        }
                    }
                }
                loaded = true;
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Load failed", e);
                setLoading(false);
                showError(getString(R.string.err_load));
            }
        });
    }

    private void addSkillFromInput() {
        binding.tilSkill.setError(null);
        String skill = textOf(binding.etSkill.getText());
        if (skill.isEmpty()) {
            return;
        }
        if (skill.length() > MAX_SKILL_LENGTH) {
            binding.tilSkill.setError(getString(R.string.err_skill_length));
            return;
        }
        if (skills.size() >= MAX_SKILLS) {
            binding.tilSkill.setError(getString(R.string.err_skill_limit));
            return;
        }
        for (String existing : skills) {
            if (existing.toLowerCase(Locale.ROOT).equals(skill.toLowerCase(Locale.ROOT))) {
                binding.tilSkill.setError(getString(R.string.err_skill_duplicate));
                return;
            }
        }
        skills.add(skill);
        addChip(skill);
        binding.etSkill.setText("");
    }

    private void addChip(String skill) {
        Chip chip = new Chip(this);
        chip.setText(skill);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            skills.remove(skill);
            binding.chipGroupSkills.removeView(chip);
        });
        binding.chipGroupSkills.addView(chip);
    }

    private void onSaveClicked() {
        if (!loaded) {
            return;
        }
        binding.tilName.setError(null);
        binding.tilType.setError(null);

        String name = textOf(binding.etName.getText());
        String type = textOf(binding.actvType.getText());
        String city = textOf(binding.etCity.getText());
        String bio = textOf(binding.etBio.getText());
        String about = textOf(binding.etAbout.getText());

        if (name.length() < 2) {
            binding.tilName.setError(getString(R.string.err_name));
            return;
        }
        if (!ProfileTypes.isValid(type)) {
            binding.tilType.setError(getString(R.string.err_type));
            return;
        }

        setLoading(true);
        repository.updateProfile(user.getUid(), name, type, city, bio, about,
            new ArrayList<>(skills), new ProfileRepository.SaveCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    setResult(RESULT_OK);
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
        binding.btnSave.setEnabled(!loading);
    }

    private void showError(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }
}
