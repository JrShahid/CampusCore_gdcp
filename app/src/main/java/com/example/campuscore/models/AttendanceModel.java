package com.example.campuscore.models;

import com.google.firebase.Timestamp;

public class AttendanceModel {
    private String attendanceId;
    private String sessionId;
    private String assignmentId;
    private String studentUid;
    private String studentName;
    private String rollNumber;
    private String registrationNumber;
    private String teacherUid;
    private String subject;
    private String semester;
    private String department;
    private String date;
    private String status;
    private Timestamp timestamp;

    public AttendanceModel() {
    }

    public AttendanceModel(String attendanceId, String studentUid, String studentName, String rollNumber,
                           String teacherUid, String subject, String semester, String department,
                           String date, String status, Timestamp timestamp) {
        this.attendanceId = attendanceId;
        this.studentUid = studentUid;
        this.studentName = studentName;
        this.rollNumber = rollNumber;
        this.teacherUid = teacherUid;
        this.subject = subject;
        this.semester = semester;
        this.department = department;
        this.date = date;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getAttendanceId() {
        return attendanceId == null ? "" : attendanceId;
    }

    public void setAttendanceId(String attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getSessionId() {
        return sessionId == null ? "" : sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAssignmentId() {
        return assignmentId == null ? "" : assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getStudentUid() {
        return studentUid == null ? "" : studentUid;
    }

    public void setStudentUid(String studentUid) {
        this.studentUid = studentUid;
    }

    public String getStudentName() {
        return studentName == null ? "" : studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRollNumber() {
        return rollNumber == null ? "" : rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getRegistrationNumber() {
        return registrationNumber == null ? "" : registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getTeacherUid() {
        return teacherUid == null ? "" : teacherUid;
    }

    public void setTeacherUid(String teacherUid) {
        this.teacherUid = teacherUid;
    }

    public String getSubject() {
        return subject == null ? "" : subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSemester() {
        return semester == null ? "" : semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getDepartment() {
        return department == null ? "" : department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDate() {
        return date == null ? "" : date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status == null ? "Absent" : status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
