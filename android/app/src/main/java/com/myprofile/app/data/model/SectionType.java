package com.myprofile.app.data.model;

import com.myprofile.app.R;

public enum SectionType {

    EDUCATION(
            "education", "qualification", "institution", "year", null, null,
            100, 20, true, false, null,
            R.string.section_education,
            R.string.hint_qualification, R.string.hint_institution, R.string.hint_year, 0,
            R.string.edu_empty, R.string.edu_add, R.string.edu_edit),

    EXPERIENCE(
            "experience", "role", "organization", "duration", "description", null,
            100, 40, true, false, null,
            R.string.section_experience,
            R.string.hint_role, R.string.hint_organization, R.string.hint_duration,
            R.string.hint_description,
            R.string.exp_empty, R.string.exp_add, R.string.exp_edit),

    SERVICES(
            "services", "name", "price", null, "description", null,
            40, 0, false, false, null,
            R.string.section_services,
            R.string.hint_service_name, R.string.hint_price, 0, R.string.hint_description,
            R.string.srv_empty, R.string.srv_add, R.string.srv_edit),

    ACHIEVEMENTS(
            "achievements", "title", null, "date", "description", null,
            0, 30, false, false, null,
            R.string.section_achievements,
            R.string.hint_achievement_title, 0, R.string.hint_achievement_date,
            R.string.hint_description,
            R.string.ach_empty, R.string.ach_add, R.string.ach_edit),

    SOCIAL_LINKS(
            "socialLinks", "platform", "url", null, null, "visibility",
            300, 0, true, true,
            new String[]{"Instagram", "YouTube", "Facebook", "LinkedIn",
                    "Twitter / X", "GitHub", "Website", "Other"},
            R.string.section_social,
            R.string.hint_platform, R.string.hint_url, 0, 0,
            R.string.soc_empty, R.string.soc_add, R.string.soc_edit);

    public final String collection;
    public final String titleKey;
    public final String subtitleKey;
    public final String periodKey;
    public final String descriptionKey;
    public final String visibilityKey;

    public final int subtitleMax;
    public final int periodMax;
    public final boolean subtitleRequired;
    public final boolean urlSubtitle;
    public final String[] titleChoices;

    public final int screenTitleRes;
    public final int titleHintRes;
    public final int subtitleHintRes;
    public final int periodHintRes;
    public final int descriptionHintRes;
    public final int emptyRes;
    public final int addTitleRes;
    public final int editTitleRes;

    SectionType(String collection, String titleKey, String subtitleKey, String periodKey,
                String descriptionKey, String visibilityKey,
                int subtitleMax, int periodMax, boolean subtitleRequired,
                boolean urlSubtitle, String[] titleChoices,
                int screenTitleRes, int titleHintRes, int subtitleHintRes,
                int periodHintRes, int descriptionHintRes,
                int emptyRes, int addTitleRes, int editTitleRes) {
        this.collection = collection;
        this.titleKey = titleKey;
        this.subtitleKey = subtitleKey;
        this.periodKey = periodKey;
        this.descriptionKey = descriptionKey;
        this.visibilityKey = visibilityKey;
        this.subtitleMax = subtitleMax;
        this.periodMax = periodMax;
        this.subtitleRequired = subtitleRequired;
        this.urlSubtitle = urlSubtitle;
        this.titleChoices = titleChoices;
        this.screenTitleRes = screenTitleRes;
        this.titleHintRes = titleHintRes;
        this.subtitleHintRes = subtitleHintRes;
        this.periodHintRes = periodHintRes;
        this.descriptionHintRes = descriptionHintRes;
        this.emptyRes = emptyRes;
        this.addTitleRes = addTitleRes;
        this.editTitleRes = editTitleRes;
    }

    public boolean hasSubtitle() {
        return subtitleKey != null;
    }

    public boolean hasPeriod() {
        return periodKey != null;
    }

    public boolean hasDescription() {
        return descriptionKey != null;
    }

    public boolean hasVisibility() {
        return visibilityKey != null;
    }

    public boolean hasChoices() {
        return titleChoices != null;
    }

    public boolean isValidChoice(String value) {
        if (titleChoices == null) {
            return true;
        }
        for (String choice : titleChoices) {
            if (choice.equals(value)) {
                return true;
            }
        }
        return false;
    }
}