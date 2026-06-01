package com.example.campuscore.models;

public class PendingTeacherModel {
    private String employeeId = "";
    private String name = "";
    private String primaryDepartmentId = "";
    private String designation = "";
    private String email = "";
    private String uid = "";
    private boolean active = true;

    public PendingTeacherModel() {
    }

    public PendingTeacherModel(String employeeId, String name, String primaryDepartmentId,
                               String designation, String email, boolean active) {
        this.employeeId = employeeId;
        this.name = name;
        this.primaryDepartmentId = primaryDepartmentId;
        this.designation = designation;
        this.email = email;
        this.active = active;
    }

    public String getEmployeeId() {
        return employeeId == null ? "" : employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrimaryDepartmentId() {
        return primaryDepartmentId == null ? "" : primaryDepartmentId;
    }

    public void setPrimaryDepartmentId(String primaryDepartmentId) {
        this.primaryDepartmentId = primaryDepartmentId;
    }

    public String getDesignation() {
        return designation == null ? "" : designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getEmail() {
        return email == null ? "" : email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUid() {
        return uid == null ? "" : uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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
}
