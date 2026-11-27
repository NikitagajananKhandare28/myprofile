package com.myprofile.app.ui.preview;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.myprofile.app.R;
import com.myprofile.app.data.repository.PublishRepository;
import com.myprofile.app.databinding.ActivityPreviewBinding;
import com.myprofile.app.ui.welcome.WelcomeActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreviewActivity extends AppCompatActivity {

    private static final String TAG = "Preview";

    private ActivityPreviewBinding binding;
    private final PublishRepository repository = new PublishRepository();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        binding.progress.setVisibility(View.VISIBLE);
        repository.buildSnapshot(user.getUid(), new PublishRepository.SnapshotCallback() {
            @Override
            public void onBuilt(Map<String, Object> snapshot) {
                binding.progress.setVisibility(View.GONE);
                render(snapshot);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Build failed", e);
                binding.progress.setVisibility(View.GONE);
                Snackbar.make(binding.getRoot(), R.string.err_load, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void render(Map<String, Object> s) {
        binding.container.removeAllViews();

        addText(getString(R.string.preview_note), 13, false, R.color.text_secondary, 0);
        addImage(str(s.get("photoUrl")), 120, 12, true);
        addText(str(s.get("displayName")), 28, true, R.color.navy_900, 16);
        addText(str(s.get("profileType")), 16, false, R.color.blue_600, 2);
        if (!str(s.get("city")).isEmpty()) {
            addText(str(s.get("city")), 14, false, R.color.text_secondary, 2);
        }
        if (!str(s.get("bio")).isEmpty()) {
            addText(str(s.get("bio")), 16, false, R.color.text_primary, 12);
        }
        if (!str(s.get("aboutMe")).isEmpty()) {
            heading(R.string.preview_about);
            addText(str(s.get("aboutMe")), 15, false, R.color.text_primary, 6);
        }

        Object skills = s.get("skills");
        if (skills instanceof List && !((List<?>) skills).isEmpty()) {
            heading(R.string.preview_skills);
            StringBuilder sb = new StringBuilder();
            for (Object skill : (List<?>) skills) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(String.valueOf(skill));
            }
            addText(sb.toString(), 15, false, R.color.text_primary, 6);
        }

        entries(R.string.section_experience, s.get("experience"),
                "role", "organization", "duration", "description");
        entries(R.string.section_education, s.get("education"),
                "qualification", "institution", "year", null);
        entries(R.string.section_services, s.get("services"),
                "name", "price", null, "description");
        entries(R.string.section_achievements, s.get("achievements"),
                "title", null, "date", "description");
        entries(R.string.section_social, s.get("socialLinks"),
                "platform", "url", null, null);

        Object gallery = s.get("gallery");
        if (gallery instanceof List && !((List<?>) gallery).isEmpty()) {
            heading(R.string.gallery_label);
            for (Object g : (List<?>) gallery) {
                if (g instanceof Map) {
                    Map<?, ?> item = (Map<?, ?>) g;
                    addImage(str(item.get("url")), 200, 12, false);
                    if (!str(item.get("caption")).isEmpty()) {
                        addText(str(item.get("caption")), 13, false, R.color.text_secondary, 4);
                    }
                }
            }
        }

        Object contact = s.get("contact");
        if (contact instanceof Map) {
            Map<?, ?> c = (Map<?, ?>) contact;
            heading(R.string.preview_contact);
            contactLine(R.string.label_phone, c.get("phone"));
            contactLine(R.string.label_whatsapp, c.get("whatsapp"));
            contactLine(R.string.label_email, c.get("email"));
            contactLine(R.string.label_location, c.get("location"));
        }
    }

    private void contactLine(int labelRes, Object value) {
        String text = str(value);
        if (!text.isEmpty()) {
            addText(getString(labelRes) + ": " + text, 15, false, R.color.text_primary, 6);
        }
    }

    @SuppressWarnings("unchecked")
    private void entries(int headingRes, Object raw, String titleKey, String subKey,
                         String periodKey, String descKey) {
        if (!(raw instanceof List) || ((List<?>) raw).isEmpty()) {
            return;
        }
        heading(headingRes);
        for (Object o : (List<Object>) raw) {
            if (!(o instanceof Map)) {
                continue;
            }
            Map<String, Object> item = (Map<String, Object>) o;
            addText(str(item.get(titleKey)), 16, true, R.color.navy_900, 12);
            if (subKey != null && !str(item.get(subKey)).isEmpty()) {
                addText(str(item.get(subKey)), 15, false, R.color.text_primary, 2);
            }
            if (periodKey != null && !str(item.get(periodKey)).isEmpty()) {
                addText(str(item.get(periodKey)), 13, false, R.color.blue_600, 2);
            }
            if (descKey != null && !str(item.get(descKey)).isEmpty()) {
                addText(str(item.get(descKey)), 14, false, R.color.text_secondary, 4);
            }
        }
    }

    private void addImage(String url, int heightDp, int topDp, boolean square) {
        if (url == null || url.isEmpty()) {
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundResource(R.drawable.bg_logo);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                square ? (int) (heightDp * d) : android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (heightDp * d));
        lp.topMargin = (int) (topDp * d);
        binding.container.addView(iv, lp);
        Glide.with(this).load(url).centerCrop().into(iv);
    }

    private void heading(int stringRes) {
        addText(getString(stringRes), 18, true, R.color.navy_900, 28);
    }

    private void addText(String value, int sp, boolean bold, int colorRes, int topDp) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(ContextCompat.getColor(this, colorRes));
        if (bold) {
            tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        }
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (topDp * getResources().getDisplayMetrics().density);
        binding.container.addView(tv, lp);
    }

    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}