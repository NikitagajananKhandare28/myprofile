package com.myprofile.app.ui.builder;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.myprofile.app.data.model.SectionType;
import com.myprofile.app.databinding.ActivityProfileBuilderBinding;
import com.myprofile.app.ui.profile.EditProfileActivity;

public class ProfileBuilderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityProfileBuilderBinding binding =
            ActivityProfileBuilderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnAbout.setOnClickListener(v ->
            startActivity(new Intent(this, EditProfileActivity.class)));
        binding.btnEducation.setOnClickListener(v -> open(SectionType.EDUCATION));
        binding.btnExperience.setOnClickListener(v -> open(SectionType.EXPERIENCE));
        binding.btnServices.setOnClickListener(v -> open(SectionType.SERVICES));
        binding.btnAchievements.setOnClickListener(v -> open(SectionType.ACHIEVEMENTS));
        binding.btnSocial.setOnClickListener(v -> open(SectionType.SOCIAL_LINKS));
    }

    private void open(SectionType type) {
        Intent intent = new Intent(this, SectionListActivity.class);
        intent.putExtra(SectionListActivity.EXTRA_TYPE, type.name());
        startActivity(intent);
    }
}
