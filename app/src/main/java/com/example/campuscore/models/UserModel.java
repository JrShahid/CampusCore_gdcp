package com.example.campuscore.models;

import com.example.campuscore.utils.AppRoles;

import java.util.ArrayList;
import java.util.List;

public class UserModel {
    private String uid;
    private String name;
    private String email;
    private String role;
    private String department;
    private String departmentId;
    private String primaryDepartmentId;
    private String employeeId;
    private String designation;
    private String semester;
    private String rollNumber;
    private String section;
    private String batch;
    private String registrationNumber;
    private List<String> assignedSubjects;

    public UserModel() {
        // Required for Firestore deserialization.
    }

    public UserModel(String uid, String name, String email, String role, String department, String semester, String rollNumber) {
        this(uid, name, email, role, department, semester, rollNumber, "", "");
    }

    public UserModel(String uid, String name, String email, String role, String department, String semester,
                     String rollNumber, String section, String batch) {
        this(uid, name, email, role, department, semester, rollNumber, section, batch, "");
    }

    public UserModel(String uid, String name, String email, String role, String department, String semester,
                     String rollNumber, String section, String batch, String registrationNumber) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.department = department;
        this.departmentId = department;
        this.semester = semester;
        this.rollNumber = rollNumber;
        this.section = section;
        this.batch = batch;
        this.registrationNumber = registrationNumber;
        this.assignedSubjects = new ArrayList<>();
    }

    public String getUid() {
        return uid == null ? "" : uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role == null ? AppRoles.STUDENT : role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        if (department != null && !department.trim().isEmpty()) {
            return department;
        }
        return getDepartmentId();
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDepartmentId() {
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            return departmentId;
        }
        return department == null ? "" : department;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getPrimaryDepartmentId() {
        if (primaryDepartmentId != null && !primaryDepartmentId.trim().isEmpty()) {
            return primaryDepartmentId;
        }
        return getDepartmentId();
    }

    public void setPrimaryDepartmentId(String primaryDepartmentId) {
        this.primaryDepartmentId = primaryDepartmentId;
    }

    public String getEmployeeId() {
        return employeeId == null ? "" : employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDesignation() {
        return designation == null ? "" : designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getSemester() {
        return semester == null ? "" : semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getRollNumber() {
        return rollNumber == null ? "" : rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getSection() {
        return section == null ? "" : section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getBatch() {
        return batch == null ? "" : batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getRegistrationNumber() {
        return registrationNumber == null ? "" : registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public List<String> getAssignedSubjects() {
        return assignedSubjects == null ? new ArrayList<>() : assignedSubjects;
    }

    public void setAssignedSubjects(List<String> assignedSubjects) {
        this.assignedSubjects = assignedSubjects;
    }

    public boolean hasAssignedSubjects() {
        return assignedSubjects != null && !assignedSubjects.isEmpty();
    }
}
