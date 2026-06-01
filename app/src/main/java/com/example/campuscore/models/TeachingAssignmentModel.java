package com.example.campuscore.models;

public class TeachingAssignmentModel {
    private String assignmentId = "";
    private String teacherUid = "";
    private String employeeId = "";
    private String teacherName = "";
    private String subjectCode = "";
    private String subjectName = "";
    private String departmentId = "";
    private String departmentLabel = "";
    private String semester = "";
    private String section = "";
    private boolean active = true;
    private String assignedBy = "";

    public TeachingAssignmentModel() {
    }

    public TeachingAssignmentModel(String assignmentId, String teacherUid, String teacherName, String subjectCode,
                                   String subjectName, String departmentId, String semester, String section,
                                   boolean active) {
        this.assignmentId = assignmentId;
        this.teacherUid = teacherUid;
        this.teacherName = teacherName;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.departmentId = departmentId;
        this.semester = semester;
        this.section = section;
        this.active = active;
    }

    public String getAssignmentId() {
        return assignmentId == null ? "" : assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getTeacherUid() {
        return teacherUid == null ? "" : teacherUid;
    }

    public void setTeacherUid(String teacherUid) {
        this.teacherUid = teacherUid;
    }

    public String getEmployeeId() {
        return employeeId == null ? "" : employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getTeacherName() {
        return teacherName == null ? "" : teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
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

    public String getDepartmentLabel() {
        return departmentLabel == null ? "" : departmentLabel;
    }

    public void setDepartmentLabel(String departmentLabel) {
        this.departmentLabel = departmentLabel;
    }

    public String getSemester() {
        return semester == null ? "" : semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getSection() {
        return section == null ? "" : section;
    }

    public void setSection(String section) {
        this.section = section;
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

    public String getAssignedBy() {
        return assignedBy == null ? "" : assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String displayLabel() {
        String subject = getSubjectName().isEmpty() ? getSubjectCode() : getSubjectName();
        return subject + " - " + getDepartmentId() + " " + getSemester() + " Section " + getSection();
    }
}
