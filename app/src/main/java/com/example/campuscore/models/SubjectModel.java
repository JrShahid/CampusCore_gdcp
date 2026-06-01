package com.example.campuscore.models;

public class SubjectModel {
    private String subjectCode = "";
    private String subjectName = "";
    private String departmentId = "";
    private String semester = "";
    private boolean active = true;

    public SubjectModel() {
    }

    public SubjectModel(String subjectCode, String subjectName, String departmentId, String semester, boolean active) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.departmentId = departmentId;
        this.semester = semester;
        this.active = active;
    }

    public String getSubjectCode() {
        return subjectCode == null ? "" : subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName == null ? "" : subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDepartmentId() {
        return departmentId == null ? "" : departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getSemester() {
        return semester == null ? "" : semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getIsActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setIsActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return getSubjectCode() + " - " + getSubjectName();
    }
}
