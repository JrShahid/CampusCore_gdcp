package com.example.campuscore.services;

import android.util.Log;

import com.example.campuscore.models.DepartmentModel;
import com.example.campuscore.models.PendingTeacherModel;
import com.example.campuscore.models.SubjectModel;
import com.example.campuscore.models.TeachingAssignmentModel;
import com.example.campuscore.models.UserModel;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class TeachingAssignmentNormalizer {
    private static final String TAG = "AssignmentNormalizer";

    private TeachingAssignmentNormalizer() {
    }

    public static TeachingAssignmentModel normalizeForSave(TeachingAssignmentModel assignment,
                                                           UserModel teacher,
                                                           SubjectModel subject,
                                                           DepartmentModel department) {
        assignment.setTeacherUid(teacher.getUid());
        assignment.setTeacherName(teacher.getName().trim());
        assignment.setEmployeeId(normalizeEmployeeId(teacher.getEmployeeId()));
        assignment.setSubjectCode(subject.getSubjectCode().trim().toUpperCase(Locale.US));
        assignment.setSubjectName(subject.getSubjectName().trim());
        assignment.setDepartmentId(department.getDepartmentId().trim());
        assignment.setDepartmentLabel(departmentLabel(department));
        assignment.setSemester(subject.getSemester().trim());
        Log.d(TAG, "Normalized assignment for save: " + assignment.getAssignmentId());
        return assignment;
    }

    public static TeachingAssignmentModel normalizeForSave(TeachingAssignmentModel assignment,
                                                           PendingTeacherModel teacher,
                                                           SubjectModel subject,
                                                           DepartmentModel department) {
        assignment.setTeacherUid(teacher.getUid());
        assignment.setTeacherName(teacher.getName().trim());
        assignment.setEmployeeId(normalizeEmployeeId(teacher.getEmployeeId()));
        assignment.setSubjectCode(subject.getSubjectCode().trim().toUpperCase(Locale.US));
        assignment.setSubjectName(subject.getSubjectName().trim());
        assignment.setDepartmentId(department.getDepartmentId().trim());
        assignment.setDepartmentLabel(departmentLabel(department));
        assignment.setSemester(subject.getSemester().trim());
        Log.d(TAG, "Normalized pending-teacher assignment for save: " + assignment.getAssignmentId());
        return assignment;
    }

    public static boolean needsRepair(TeachingAssignmentModel assignment) {
        return isBlank(assignment.getTeacherName())
                || isBlank(assignment.getEmployeeId())
                || isBlank(assignment.getSubjectName())
                || isBlank(assignment.getDepartmentLabel());
    }

    public static Map<String, Object> repairValues(TeachingAssignmentModel assignment,
                                                   UserModel teacher,
                                                   SubjectModel subject,
                                                   DepartmentModel department) {
        Map<String, Object> values = new HashMap<>();
        if (teacher != null) {
            if (isBlank(assignment.getTeacherUid()) && !isBlank(teacher.getUid())) {
                values.put("teacherUid", teacher.getUid());
            }
            if (isBlank(assignment.getTeacherName()) && !isBlank(teacher.getName())) {
                values.put("teacherName", teacher.getName().trim());
            }
            if (isBlank(assignment.getEmployeeId()) && !isBlank(teacher.getEmployeeId())) {
                values.put("employeeId", normalizeEmployeeId(teacher.getEmployeeId()));
            }
        }
        if (subject != null && isBlank(assignment.getSubjectName()) && !isBlank(subject.getSubjectName())) {
            values.put("subjectName", subject.getSubjectName().trim());
        }
        if (department != null && isBlank(assignment.getDepartmentLabel())) {
            values.put("departmentLabel", departmentLabel(department));
        }
        if (!values.isEmpty()) {
            Log.d(TAG, "Prepared assignment repair for " + assignment.getAssignmentId() + ": " + values.keySet());
        }
        return values;
    }

    public static String validateComplete(TeachingAssignmentModel assignment) {
        if (isBlank(assignment.getTeacherName())) {
            return "Teacher name is required for teaching assignments.";
        }
        if (isBlank(assignment.getEmployeeId())) {
            return "Teacher employee ID is required for teaching assignments.";
        }
        if (isBlank(assignment.getSubjectCode()) || isBlank(assignment.getSubjectName())) {
            return "A valid subject is required for teaching assignments.";
        }
        if (isBlank(assignment.getDepartmentId())) {
            return "A valid department is required for teaching assignments.";
        }
        if (isBlank(assignment.getSemester()) || isBlank(assignment.getSection())) {
            return "Semester and section are required for teaching assignments.";
        }
        return "";
    }

    public static String normalizeEmployeeId(String employeeId) {
        return employeeId == null ? "" : employeeId.trim().toUpperCase(Locale.US);
    }

    private static String departmentLabel(DepartmentModel department) {
        String name = department.getDepartmentName().trim();
        return name.isEmpty() ? department.getDepartmentId().trim() : department.getDepartmentId().trim() + " - " + name;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
