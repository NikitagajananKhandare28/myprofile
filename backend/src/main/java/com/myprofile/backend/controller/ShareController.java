package com.myprofile.backend.controller;

import java.util.concurrent.TimeUnit;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.myprofile.backend.config.AppProperties;
import com.myprofile.backend.security.UsernamePolicy;
import com.myprofile.backend.service.SnapshotBuilder;
import com.myprofile.backend.util.Await;

/** Link-preview page. WhatsApp and other apps read these tags; people are redirected on. */
@RestController
public class ShareController {

    private final Firestore db;
    private final AppProperties props;

    public ShareController(Firestore db, AppProperties props) {
        this.db = db;
        this.props = props;
    }

    @GetMapping(value = "/p/{username}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@PathVariable String username) {
        if (!UsernamePolicy.isValid(username)) {
            return notFound();
        }
        DocumentSnapshot doc = Await.of(db.collection("publicProfiles").document(username).get());
        if (!doc.exists()) {
            return notFound();
        }

        String name = nz(doc.getString("displayName"));
        String type = nz(doc.getString("profileType"));
        String bio = nz(doc.getString("bio"));
        String photo = nz(doc.getString("photoUrl"));

        String title = HtmlUtils.htmlEscape(name + " | MyProfile");
        String description = HtmlUtils.htmlEscape(
                bio.isEmpty() ? type + " on MyProfile" : SnapshotBuilder.clean(bio, 160));
        String url = HtmlUtils.htmlEscape(props.publicBaseUrl() + "/" + username);
        String image = photo.startsWith(SnapshotBuilder.STORAGE_PREFIX)
                ? "<meta property=\"og:image\" content=\"" + HtmlUtils.htmlEscape(photo) + "\">" : "";

        String html = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
                + "<title>" + title + "</title>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<meta name=\"description\" content=\"" + description + "\">"
                + "<meta property=\"og:type\" content=\"profile\">"
                + "<meta property=\"og:title\" content=\"" + title + "\">"
                + "<meta property=\"og:description\" content=\"" + description + "\">"
                + "<meta property=\"og:url\" content=\"" + url + "\">"
                + image
                + "<meta name=\"twitter:card\" content=\"summary\">"
                + "<meta http-equiv=\"refresh\" content=\"0;url=" + url + "\">"
                + "</head><body><p>Opening profile... <a href=\"" + url + "\">Continue</a></p></body></html>";

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
                .body(html);
    }

    private ResponseEntity<String> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.TEXT_HTML)
                .body("<!doctype html><title>Not found</title><p>Profile not found.</p>");
    }

    private String nz(String value) {
        return value == null ? "" : value;
    }
}