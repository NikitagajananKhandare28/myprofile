package com.myprofile.app.ui.builder;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.myprofile.app.R;
import com.myprofile.app.data.model.ContactInfo;
import com.myprofile.app.data.repository.ContactRepository;
import com.myprofile.app.databinding.ActivityContactBinding;
import com.myprofile.app.ui.welcome.WelcomeActivity;

public class ContactActivity extends AppCompatActivity {

    private static final String TAG = "Contact";
    private static final String PHONE_PATTERN = "^[+]?[0-9]{7,15}$";

    private ActivityContactBinding binding;
    private final ContactRepository repository = new ContactRepository();
    private FirebaseUser user;
    private boolean loaded = false;
    private boolean minor = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityContactBinding.inflate(getLayoutInflater());
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

    private void load() {
        setLoading(true);
        repository.load(user.getUid(), new ContactRepository.LoadCallback() {
            @Override
            public void onLoaded(ContactInfo info) {
                setLoading(false);
                minor = info.minor;

                binding.etPhone.setText(info.phone);
                binding.etWhatsapp.setText(info.whatsapp);
                binding.etEmail.setText(info.email);
                binding.etLocation.setText(info.location);

                binding.swPhone.setChecked(!minor && info.phonePublic);
                binding.swWhatsapp.setChecked(!minor && info.whatsappPublic);
                binding.swEmail.setChecked(!minor && info.emailPublic);
                binding.swLocation.setChecked(!minor && info.locationPublic);

                if (minor) {
                    binding.tvMinorNote.setVisibility(View.VISIBLE);
                    binding.swPhone.setEnabled(false);
                    binding.swWhatsapp.setEnabled(false);
                    binding.swEmail.setEnabled(false);
                    binding.swLocation.setEnabled(false);
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
        binding.tilPhone.setError(null);
        binding.tilWhatsapp.setError(null);
        binding.tilEmail.setError(null);
        binding.tilLocation.setError(null);

        String phone = cleanNumber(textOf(binding.etPhone.getText()));
        String whatsapp = cleanNumber(textOf(binding.etWhatsapp.getText()));
        String email = textOf(binding.etEmail.getText());
        String location = textOf(binding.etLocation.getText());

        boolean valid = true;
        if (!phone.isEmpty() && !phone.matches(PHONE_PATTERN)) {
            binding.tilPhone.setError(getString(R.string.err_invalid_phone));
            valid = false;
        }
        if (!whatsapp.isEmpty() && !whatsapp.matches(PHONE_PATTERN)) {
            binding.tilWhatsapp.setError(getString(R.string.err_invalid_phone));
            valid = false;
        }
        if (!email.isEmpty() && (email.length() > 100
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            binding.tilEmail.setError(getString(R.string.err_invalid_email));
            valid = false;
        }
        if (location.length() > 100) {
            binding.tilLocation.setError(getString(R.string.err_too_long, 100));
            valid = false;
        }
        if (!valid) {
            return;
        }

        ContactInfo info = new ContactInfo();
        info.minor = minor;
        info.phone = phone;
        info.whatsapp = whatsapp;
        info.email = email;
        info.location = location;
        info.phonePublic = binding.swPhone.isChecked();
        info.whatsappPublic = binding.swWhatsapp.isChecked();
        info.emailPublic = binding.swEmail.isChecked();
        info.locationPublic = binding.swLocation.isChecked();

        setLoading(true);
        repository.save(user.getUid(), info, new ContactRepository.SaveCallback() {
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

    private String cleanNumber(String raw) {
        return raw.replaceAll("[\\s\\-()]", "");
    }

    private String textOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSave.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}