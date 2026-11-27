package com.myprofile.app.data.model;

public class Photo {

    private final String id;
    private final String url;
    private final String path;
    private final String caption;

    public Photo(String id, String url, String path, String caption) {
        this.id = id;
        this.url = url == null ? "" : url;
        this.path = path == null ? "" : path;
        this.caption = caption == null ? "" : caption;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public String getCaption() {
        return caption;
    }
}