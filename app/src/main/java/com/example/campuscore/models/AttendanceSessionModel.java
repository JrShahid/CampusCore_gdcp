package com.example.campuscore.models;

import com.google.firebase.Timestamp;

import java.util.Date;

public class AttendanceSessionModel {
    private String sessionId = "";
    private String assignmentId = "";
    private String teacherUid = "";
    private String teacherName = "";
    private String employeeId = "";
    private String subjectCode = "";
    private String subjectName = "";
    private String departmentId = "";
    private String semester = "";
    private String section = "";
    private String date = "";
    private int totalStudents;
    private int presentCount;
    private int absentCount;
    private boolean locked = true;
    private Timestamp submittedAt;
    private Timestamp editableUntil;
    private Timestamp lastModifiedAt;
    private String lastModifiedBy = "";
    private Timestamp timestamp;

    public AttendanceSessionModel() {
    }

    public String getSessionId() { return sessionId == null ? "" : sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAssignmentId() { return assignmentId == null ? "" : assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
    public String getTeacherUid() { return teacherUid == null ? "" : teacherUid; }
    public void setTeacherUid(String teacherUid) { this.teacherUid = teacherUid; }
    public String getTeacherName() { return teacherName == null ? "" : teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getEmployeeId() { return employeeId == null ? "" : employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getSubjectCode() { return subjectCode == null ? "" : subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public String getSubjectName() { return subjectName == null ? "" : subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public String getDepartmentId() { return departmentId == null ? "" : departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public String getSemester() { return semester == null ? "" : semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getSection() { return section == null ? "" : section; }
    public void setSection(String section) { this.section = section; }
    public String getDate() { return date == null ? "" : date; }
    public void setDate(String date) { this.date = date; }
    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }
    public int getPresentCount() { return presentCount; }
    public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
    public int getAbsentCount() { return absentCount; }
    public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }
    public boolean isLocked() { return locked; }
    public boolean getIsLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public void setIsLocked(boolean locked) { this.locked = locked; }
    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }
    public Timestamp getEditableUntil() { return editableUntil; }
    public void setEditableUntil(Timestamp editableUntil) { this.editableUntil = editableUntil; }
    public Timestamp getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(Timestamp lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
    public String getLastModifiedBy() { return lastModifiedBy == null ? "" : lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isEditableNow() {
        return editableUntil != null && new Date().before(editableUntil.toDate());
    }

    public String displaySubject() {
        return getSubjectName().isEmpty() ? getSubjectCode() : getSubjectName();
    }
}
