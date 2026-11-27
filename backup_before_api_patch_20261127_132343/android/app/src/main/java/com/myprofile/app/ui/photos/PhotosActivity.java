package com.myprofile.app.ui.photos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.myprofile.app.R;
import com.myprofile.app.adapter.PhotoAdapter;
import com.myprofile.app.data.model.Photo;
import com.myprofile.app.data.repository.PhotoRepository;
import com.myprofile.app.data.repository.ProfileRepository;
import com.myprofile.app.databinding.ActivityPhotosBinding;
import com.myprofile.app.ui.welcome.WelcomeActivity;
import com.myprofile.app.util.ImageUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotosActivity extends AppCompatActivity {

    private static final String TAG = "Photos";
    private static final int MAX_PHOTOS = 12;

    private ActivityPhotosBinding binding;
    private final PhotoRepository repository = new PhotoRepository();
    private final ProfileRepository profileRepository = new ProfileRepository();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Photo> photos = new ArrayList<>();
    private PhotoAdapter adapter;
    private FirebaseUser user;
    private boolean pickingAvatar = false;

    private final ActivityResultLauncher<PickVisualMediaRequest> picker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) {
                    return;
                }
                if (pickingAvatar) {
                    uploadAvatar(uri);
                } else {
                    askCaption(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPhotosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        adapter = new PhotoAdapter(this::confirmDelete);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        binding.btnChangePhoto.setOnClickListener(v -> {
            pickingAvatar = true;
            pick();
        });
        binding.btnRemovePhoto.setOnClickListener(v -> removeAvatar());
        binding.btnAddGallery.setOnClickListener(v -> {
            if (photos.size() >= MAX_PHOTOS) {
                showMessage(getString(R.string.err_photo_limit));
                return;
            }
            pickingAvatar = false;
            pick();
        });

        load();
    }

    private void pick() {
        picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void load() {
        setLoading(true);
        profileRepository.getProfile(user.getUid(), new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(DocumentSnapshot snapshot) {
                showAvatar(snapshot == null ? null : snapshot.getString("photoUrl"));
                repository.listPhotos(user.getUid(), new PhotoRepository.ListCallback() {
                    @Override
                    public void onLoaded(List<Photo> list) {
                        setLoading(false);
                        photos.clear();
                        photos.addAll(list);
                        adapter.setItems(list);
                        binding.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        Log.e(TAG, "List failed", e);
                        setLoading(false);
                        showMessage(getString(R.string.err_load));
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Load failed", e);
                setLoading(false);
                showMessage(getString(R.string.err_load));
            }
        });
    }

    private void showAvatar(String url) {
        if (url == null || url.isEmpty()) {
            binding.ivAvatar.setImageDrawable(null);
        } else {
            Glide.with(this).load(url).centerCrop().into(binding.ivAvatar);
        }
    }

    private void uploadAvatar(Uri uri) {
        setLoading(true);
        executor.execute(() -> {
            try {
                byte[] data = ImageUtils.compress(this, uri, 800, 85);
                runOnUiThread(() -> repository.uploadAvatar(user.getUid(), data,
                        new PhotoRepository.UrlCallback() {
                            @Override
                            public void onSuccess(String url) {
                                setLoading(false);
                                showAvatar(url);
                                showMessage(getString(R.string.msg_photo_saved));
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                Log.e(TAG, "Avatar upload failed", e);
                                setLoading(false);
                                showMessage(getString(R.string.err_photo));
                            }
                        }));
            } catch (IOException e) {
                Log.e(TAG, "Compress failed", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    showMessage(getString(R.string.err_photo));
                });
            }
        });
    }

    private void removeAvatar() {
        setLoading(true);
        repository.removeAvatar(user.getUid(), new PhotoRepository.ResultCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                showAvatar(null);
                showMessage(getString(R.string.msg_photo_removed));
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Remove failed", e);
                setLoading(false);
                showMessage(getString(R.string.err_save));
            }
        });
    }

    private void askCaption(Uri uri) {
        EditText input = new EditText(this);
        input.setHint(R.string.hint_caption);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        FrameLayout container = new FrameLayout(this);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, 8, pad, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.btn_add_gallery))
                .setView(container)
                .setPositiveButton(R.string.btn_save_item,
                        (dialog, which) -> uploadGallery(uri, input.getText().toString().trim()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void uploadGallery(Uri uri, String caption) {
        setLoading(true);
        executor.execute(() -> {
            try {
                byte[] data = ImageUtils.compress(this, uri, 1280, 80);
                runOnUiThread(() -> repository.addPhoto(user.getUid(), data, caption,
                        new PhotoRepository.ResultCallback() {
                            @Override
                            public void onSuccess() {
                                showMessage(getString(R.string.msg_photo_saved));
                                load();
                            }

                            @Override
                            public void onError(@NonNull Exception e) {
                                Log.e(TAG, "Gallery upload failed", e);
                                setLoading(false);
                                showMessage(getString(R.string.err_photo));
                            }
                        }));
            } catch (IOException e) {
                Log.e(TAG, "Compress failed", e);
                runOnUiThread(() -> {
                    setLoading(false);
                    showMessage(getString(R.string.err_photo));
                });
            }
        });
    }

    private void confirmDelete(Photo photo) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.delete_confirm_title))
                .setMessage(getString(R.string.delete_confirm_message))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    setLoading(true);
                    repository.deletePhoto(user.getUid(), photo,
                            new PhotoRepository.ResultCallback() {
                                @Override
                                public void onSuccess() {
                                    showMessage(getString(R.string.msg_photo_removed));
                                    load();
                                }

                                @Override
                                public void onError(@NonNull Exception e) {
                                    Log.e(TAG, "Delete failed", e);
                                    setLoading(false);
                                    showMessage(getString(R.string.err_save));
                                }
                            });
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setLoading(boolean loading) {
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnChangePhoto.setEnabled(!loading);
        binding.btnAddGallery.setEnabled(!loading);
    }

    private void showMessage(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}