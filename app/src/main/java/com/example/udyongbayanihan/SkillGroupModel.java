package com.example.udyongbayanihan;

public class SkillGroupModel {
    private String skillName;
    private boolean userHasSkill;
    private String requestStatus; // "PENDING", "APPROVED", "NONE"

    public SkillGroupModel(String skillName, boolean userHasSkill) {
        this.skillName = skillName;
        this.userHasSkill = userHasSkill;
        this.requestStatus = "NONE";
    }

    // Getters and setters
    public String getSkillName() { return skillName; }
    public boolean getUserHasSkill() { return userHasSkill; }
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String status) { this.requestStatus = status; }

    public void setUserHasSkill(boolean userHasSkill) {
        this.userHasSkill = userHasSkill;
    }

    public boolean isUserHasSkill() {
        return userHasSkill;
    }
}
