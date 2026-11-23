package com.myprofile.app.data.model;

import com.myprofile.app.R;

public enum SectionType {

    EDUCATION(
        "education", "qualification", "institution", "year", null, 20,
        R.string.section_education,
        R.string.hint_qualification, R.string.hint_institution, R.string.hint_year, 0,
        R.string.edu_empty, R.string.edu_add, R.string.edu_edit),

    EXPERIENCE(
        "experience", "role", "organization", "duration", "description", 40,
        R.string.section_experience,
        R.string.hint_role, R.string.hint_organization, R.string.hint_duration,
        R.string.hint_description,
        R.string.exp_empty, R.string.exp_add, R.string.exp_edit);

    public final String collection;
    public final String titleKey;
    public final String subtitleKey;
    public final String periodKey;
    public final String descriptionKey;
    public final int periodMax;

    public final int screenTitleRes;
    public final int titleHintRes;
    public final int subtitleHintRes;
    public final int periodHintRes;
    public final int descriptionHintRes;
    public final int emptyRes;
    public final int addTitleRes;
    public final int editTitleRes;

    SectionType(String collection, String titleKey, String subtitleKey, String periodKey,
                String descriptionKey, int periodMax, int screenTitleRes,
                int titleHintRes, int subtitleHintRes, int periodHintRes,
                int descriptionHintRes, int emptyRes, int addTitleRes, int editTitleRes) {
        this.collection = collection;
        this.titleKey = titleKey;
        this.subtitleKey = subtitleKey;
        this.periodKey = periodKey;
        this.descriptionKey = descriptionKey;
        this.periodMax = periodMax;
        this.screenTitleRes = screenTitleRes;
        this.titleHintRes = titleHintRes;
        this.subtitleHintRes = subtitleHintRes;
        this.periodHintRes = periodHintRes;
        this.descriptionHintRes = descriptionHintRes;
        this.emptyRes = emptyRes;
        this.addTitleRes = addTitleRes;
        this.editTitleRes = editTitleRes;
    }

    public boolean hasDescription() {
        return descriptionKey != null;
    }
}
