package com.myprofile.app.ui.builder;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.myprofile.app.R;
import com.myprofile.app.adapter.SectionAdapter;
import com.myprofile.app.data.model.SectionItem;
import com.myprofile.app.data.model.SectionType;
import com.myprofile.app.data.repository.SectionRepository;
import com.myprofile.app.databinding.ActivitySectionListBinding;
import com.myprofile.app.databinding.DialogSectionItemBinding;
import com.myprofile.app.ui.welcome.WelcomeActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SectionListActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "section_type";

    private static final String TAG = "SectionList";
    private static final int MAX_ITEMS = 20;
    private static final int MAX_TEXT = 100;
    private static final int MAX_DESCRIPTION = 500;
    private static final int MAX_URL = 300;

    private ActivitySectionListBinding binding;
    private final SectionRepository repository = new SectionRepository();
    private final List<SectionItem> currentItems = new ArrayList<>();
    private SectionAdapter adapter;
    private SectionType type;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySectionListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        String typeName = getIntent().getStringExtra(EXTRA_TYPE);
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        if (typeName == null) {
            finish();
            return;
        }
        type = SectionType.valueOf(typeName);

        binding.tvScreenTitle.setText(type.screenTitleRes);
        binding.tvEmpty.setText(type.emptyRes);

        adapter = new SectionAdapter(new SectionAdapter.Listener() {
            @Override
            public void onEdit(SectionItem item) {
                showEditDialog(item);
            }

            @Override
            public void onDelete(SectionItem item) {
                confirmDelete(item);
            }
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        binding.btnAdd.setOnClickListener(v -> {
            if (currentItems.size() >= MAX_ITEMS) {
                showMessage(getString(R.string.err_item_limit));
            } else {
                showEditDialog(null);
            }
        });

        loadItems();
    }

    private void loadItems() {
        setLoading(true);
        repository.list(user.getUid(), type, new SectionRepository.ListCallback() {
            @Override
            public void onLoaded(List<SectionItem> items) {
                setLoading(false);
                currentItems.clear();
                currentItems.addAll(items);
                adapter.setItems(items);
                binding.tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Load failed", e);
                setLoading(false);
                showMessage(getString(R.string.err_load));
            }
        });
    }

    private void showEditDialog(SectionItem existing) {
        DialogSectionItemBinding d = DialogSectionItemBinding.inflate(getLayoutInflater());

        if (type.hasChoices()) {
            d.tilChoice.setVisibility(View.VISIBLE);
            d.tilTitle.setVisibility(View.GONE);
            d.tilChoice.setHint(getString(type.titleHintRes));
            d.actvChoice.setAdapter(new ArrayAdapter<>(
                    this, android.R.layout.simple_list_item_1, type.titleChoices));
            if (existing != null) {
                d.actvChoice.setText(existing.getTitle(), false);
            }
        } else {
            d.tilTitle.setHint(getString(type.titleHintRes));
            if (existing != null) {
                d.etTitle.setText(existing.getTitle());
            }
        }

        if (type.hasSubtitle()) {
            d.tilSubtitle.setHint(getString(type.subtitleHintRes));
            d.etSubtitle.setFilters(
                    new InputFilter[]{new InputFilter.LengthFilter(type.subtitleMax)});
            if (type.urlSubtitle) {
                d.etSubtitle.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            }
            if (existing != null) {
                d.etSubtitle.setText(existing.getSubtitle());
            }
        } else {
            d.tilSubtitle.setVisibility(View.GONE);
        }

        if (type.hasPeriod()) {
            d.tilPeriod.setHint(getString(type.periodHintRes));
            d.etPeriod.setFilters(
                    new InputFilter[]{new InputFilter.LengthFilter(type.periodMax)});
            if (existing != null) {
                d.etPeriod.setText(existing.getPeriod());
            }
        } else {
            d.tilPeriod.setVisibility(View.GONE);
        }

        if (type.hasDescription()) {
            d.tilDescription.setHint(getString(type.descriptionHintRes));
            d.tilDescription.setVisibility(View.VISIBLE);
            if (existing != null) {
                d.etDescription.setText(existing.getDescription());
            }
        }

        if (type.hasVisibility()) {
            d.cbPublic.setVisibility(View.VISIBLE);
            d.cbPublic.setChecked(existing == null || existing.isPublic());
        }

        int dialogTitleRes = existing == null ? type.addTitleRes : type.editTitleRes;
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(dialogTitleRes))
                .setView(d.getRoot())
                .setPositiveButton(R.string.btn_save_item, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(shown ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    d.tilChoice.setError(null);
                    d.tilTitle.setError(null);
                    d.tilSubtitle.setError(null);
                    d.tilPeriod.setError(null);
                    d.tilDescription.setError(null);

                    boolean valid = true;

                    String title;
                    if (type.hasChoices()) {
                        title = textOf(d.actvChoice.getText());
                        if (title.isEmpty() || !type.isValidChoice(title)) {
                            d.tilChoice.setError(getString(R.string.err_platform));
                            valid = false;
                        }
                    } else {
                        title = textOf(d.etTitle.getText());
                        if (title.length() < 2) {
                            d.tilTitle.setError(getString(R.string.err_field_required));
                            valid = false;
                        } else if (title.length() > MAX_TEXT) {
                            d.tilTitle.setError(getString(R.string.err_too_long, MAX_TEXT));
                            valid = false;
                        }
                    }

                    String subtitle = type.hasSubtitle()
                            ? textOf(d.etSubtitle.getText()) : "";
                    if (type.hasSubtitle()) {
                        if (type.urlSubtitle) {
                            subtitle = normalizeUrl(subtitle);
                            if (!isValidUrl(subtitle)) {
                                d.tilSubtitle.setError(getString(R.string.err_invalid_url));
                                valid = false;
                            }
                        } else if (type.subtitleRequired && subtitle.length() < 2) {
                            d.tilSubtitle.setError(getString(R.string.err_field_required));
                            valid = false;
                        } else if (subtitle.length() > type.subtitleMax) {
                            d.tilSubtitle.setError(
                                    getString(R.string.err_too_long, type.subtitleMax));
                            valid = false;
                        }
                    }

                    String period = type.hasPeriod() ? textOf(d.etPeriod.getText()) : "";
                    if (type.hasPeriod() && period.length() > type.periodMax) {
                        d.tilPeriod.setError(getString(R.string.err_too_long, type.periodMax));
                        valid = false;
                    }

                    String description = type.hasDescription()
                            ? textOf(d.etDescription.getText()) : "";
                    if (description.length() > MAX_DESCRIPTION) {
                        d.tilDescription.setError(
                                getString(R.string.err_too_long, MAX_DESCRIPTION));
                        valid = false;
                    }

                    String visibility = "";
                    if (type.hasVisibility()) {
                        visibility = d.cbPublic.isChecked() ? "public" : "private";
                    }

                    if (!valid) {
                        return;
                    }

                    dialog.dismiss();
                    save(existing, title, subtitle, period, description, visibility);
                }));
        dialog.show();
    }

    private String normalizeUrl(String raw) {
        if (raw.isEmpty()) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://")) {
            return raw;
        }
        if (lower.startsWith("http://")) {
            return "https://" + raw.substring(7);
        }
        return "https://" + raw;
    }

    private boolean isValidUrl(String url) {
        return !url.isEmpty()
                && url.length() <= MAX_URL
                && url.startsWith("https://")
                && url.length() > "https://".length()
                && Patterns.WEB_URL.matcher(url).matches();
    }

    private void save(SectionItem existing, String title, String subtitle,
                      String period, String description, String visibility) {
        setLoading(true);
        SectionRepository.ResultCallback callback = new SectionRepository.ResultCallback() {
            @Override
            public void onSuccess() {
                showMessage(getString(R.string.msg_item_saved));
                loadItems();
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Save failed", e);
                setLoading(false);
                showMessage(getString(R.string.err_save));
            }
        };

        if (existing == null) {
            repository.add(user.getUid(), type,
                    new SectionItem(null, title, subtitle, period, description, visibility),
                    callback);
        } else {
            repository.update(user.getUid(), type,
                    new SectionItem(existing.getId(), title, subtitle, period,
                            description, visibility),
                    callback);
        }
    }

    private void confirmDelete(SectionItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(getString(R.string.delete_confirm_message))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> delete(item))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void delete(SectionItem item) {
        setLoading(true);
        repository.delete(user.getUid(), type, item.getId(),
                new SectionRepository.ResultCallback() {
                    @Override
                    public void onSuccess() {
                        showMessage(getString(R.string.msg_item_deleted));
                        loadItems();
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.e(TAG, "Delete failed", e);
                        setLoading(false);
                        showMessage(getString(R.string.err_save));
                    }
                });
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnAdd.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}