package com.myprofile.app.ui.builder;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.myprofile.app.R;
import com.myprofile.app.data.model.SectionType;
import com.myprofile.app.data.repository.ProfileRepository;
import com.myprofile.app.databinding.ActivityProfileBuilderBinding;
import com.myprofile.app.ui.photos.PhotosActivity;
import com.myprofile.app.ui.profile.EditProfileActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileBuilderActivity extends AppCompatActivity {

    private static final Map<String, List<String>> SUGGESTED = new HashMap<>();

    static {
        SUGGESTED.put("Personal", Arrays.asList("about", "photos", "social"));
        SUGGESTED.put("Student", Arrays.asList("about", "education", "achievements", "photos"));
        SUGGESTED.put("Sportsperson", Arrays.asList("about", "achievements", "photos", "social"));
        SUGGESTED.put("Teacher / Tutor", Arrays.asList("education", "experience", "services", "contact"));
        SUGGESTED.put("Professional", Arrays.asList("experience", "education", "contact", "social"));
        SUGGESTED.put("Business", Arrays.asList("about", "services", "photos", "contact"));
        SUGGESTED.put("Freelancer / Creator", Arrays.asList("services", "social", "photos", "contact"));
        SUGGESTED.put("Service Provider", Arrays.asList("services", "photos", "contact"));
        SUGGESTED.put("Artist / Other", Arrays.asList("about", "photos", "social", "achievements"));
    }

    private ActivityProfileBuilderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBuilderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnAbout.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));
        binding.btnPhotos.setOnClickListener(v ->
                startActivity(new Intent(this, PhotosActivity.class)));
        binding.btnEducation.setOnClickListener(v -> open(SectionType.EDUCATION));
        binding.btnExperience.setOnClickListener(v -> open(SectionType.EXPERIENCE));
        binding.btnServices.setOnClickListener(v -> open(SectionType.SERVICES));
        binding.btnAchievements.setOnClickListener(v -> open(SectionType.ACHIEVEMENTS));
        binding.btnSocial.setOnClickListener(v -> open(SectionType.SOCIAL_LINKS));
        binding.btnContact.setOnClickListener(v ->
                startActivity(new Intent(this, ContactActivity.class)));
        binding.btnPrivacy.setOnClickListener(v ->
                startActivity(new Intent(this, PrivacyActivity.class)));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            new ProfileRepository().getProfile(user.getUid(), new ProfileRepository.LoadCallback() {
                @Override
                public void onLoaded(DocumentSnapshot snapshot) {
                    if (snapshot != null && snapshot.exists()) {
                        applySuggestions(snapshot.getString("profileType"));
                    }
                }

                @Override
                public void onError(@NonNull Exception e) {
                    // Suggestions are optional, so a failed load is ignored.
                }
            });
        }
    }

    private void applySuggestions(String type) {
        List<String> keys = type == null ? null : SUGGESTED.get(type);
        if (keys == null) {
            return;
        }
        binding.tvSubtitle.setText(getString(R.string.builder_subtitle)
                + "\n" + getString(R.string.suggested_for, type));
        mark(binding.btnAbout, keys, "about");
        mark(binding.btnPhotos, keys, "photos"); // PHOTOS (remove if Phase 8 skipped)
        mark(binding.btnEducation, keys, "education");
        mark(binding.btnExperience, keys, "experience");
        mark(binding.btnServices, keys, "services");
        mark(binding.btnAchievements, keys, "achievements");
        mark(binding.btnSocial, keys, "social");
        mark(binding.btnContact, keys, "contact");
    }

    private void mark(MaterialButton button, List<String> keys, String key) {
        if (keys.contains(key)) {
            button.setText(button.getText() + "  (" + getString(R.string.suggested_tag) + ")");
        }
    }

    private void open(SectionType type) {
        Intent intent = new Intent(this, SectionListActivity.class);
        intent.putExtra(SectionListActivity.EXTRA_TYPE, type.name());
        startActivity(intent);
    }
}
