package com.myprofile.app.data.model;

public class SectionItem {

    private final String id;
    private final String title;
    private final String subtitle;
    private final String period;
    private final String description;
    private final String visibility;

    public SectionItem(String id, String title, String subtitle, String period,
                       String description, String visibility) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.period = period == null ? "" : period;
        this.description = description == null ? "" : description;
        this.visibility = visibility == null ? "" : visibility;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getPeriod() {
        return period;
    }

    public String getDescription() {
        return description;
    }

    public String getVisibility() {
        return visibility;
    }

    public boolean isPublic() {
        return "public".equals(visibility);
    }
}