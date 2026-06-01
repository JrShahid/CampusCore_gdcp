package com.example.campuscore.models;

public class DepartmentModel {
    private String departmentId = "";
    private String departmentName = "";
    private boolean active = true;

    public DepartmentModel() {
    }

    public DepartmentModel(String departmentId, String departmentName, boolean active) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.active = active;
    }

    public String getDepartmentId() {
        return departmentId == null ? "" : departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName == null ? "" : departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
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
