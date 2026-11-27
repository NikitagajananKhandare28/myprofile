package com.myprofile.app.ui.share;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.myprofile.app.R;
import com.myprofile.app.data.repository.ProfileRepository;
import com.myprofile.app.databinding.ActivityShareBinding;
import com.myprofile.app.ui.welcome.WelcomeActivity;
import com.myprofile.app.util.AppConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ShareActivity extends AppCompatActivity {

    private static final String TAG = "Share";

    private ActivityShareBinding binding;
    private String link = "";
    private Bitmap qr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        showButtons(false);
        binding.btnCopy.setOnClickListener(v -> copyLink());
        binding.btnShareLink.setOnClickListener(v -> shareLink());
        binding.btnWhatsapp.setOnClickListener(v -> shareWhatsapp());
        binding.btnShareQr.setOnClickListener(v -> shareQr());

        new ProfileRepository().getProfile(user.getUid(), new ProfileRepository.LoadCallback() {
            @Override
            public void onLoaded(DocumentSnapshot snapshot) {
                String status = snapshot == null ? null : snapshot.getString("status");
                String name = snapshot == null ? null : snapshot.getString("username");
                if (!"published".equals(status) || name == null || name.isEmpty()) {
                    binding.tvLink.setText(R.string.share_not_published);
                    return;
                }
                link = AppConfig.PUBLIC_BASE_URL + "p/" + name;
                binding.tvLink.setText(link);
                try {
                    qr = createQr(link, 800);
                    binding.ivQr.setImageBitmap(qr);
                } catch (WriterException e) {
                    Log.e(TAG, "QR failed", e);
                }
                showButtons(true);
            }

            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Load failed", e);
                binding.tvLink.setText(R.string.err_load);
            }
        });
    }

    private void showButtons(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        binding.btnCopy.setVisibility(v);
        binding.btnShareLink.setVisibility(v);
        binding.btnWhatsapp.setVisibility(v);
        binding.btnShareQr.setVisibility(v);
        binding.ivQr.setVisibility(v);
    }

    private Bitmap createQr(String content, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter()
                .encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                pixels[y * size + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
        return bitmap;
    }

    private void copyLink() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Profile link", link));
        Snackbar.make(binding.getRoot(), R.string.msg_link_copied, Snackbar.LENGTH_SHORT).show();
    }

    private Intent textIntent() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_message, link));
        return send;
    }

    private void shareLink() {
        startActivity(Intent.createChooser(textIntent(), getString(R.string.btn_share_link)));
    }

    private void shareWhatsapp() {
        Intent send = textIntent();
        send.setPackage("com.whatsapp");
        try {
            startActivity(send);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), R.string.err_whatsapp, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void shareQr() {
        if (qr == null) {
            return;
        }
        try {
            File dir = new File(getCacheDir(), "qr");
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            File file = new File(dir, "profile-qr.png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                qr.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, getString(R.string.btn_share_qr)));
        } catch (IOException e) {
            Log.e(TAG, "QR share failed", e);
        }
    }
}
