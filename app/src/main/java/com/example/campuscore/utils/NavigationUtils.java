package com.example.campuscore.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.example.campuscore.activities.admin.AdminDashboardActivity;
import com.example.campuscore.activities.auth.LoginActivity;
import com.example.campuscore.activities.auth.VerifyEmailActivity;
import com.example.campuscore.activities.student.StudentDashboardActivity;
import com.example.campuscore.activities.teacher.TeacherDashboardActivity;
import com.example.campuscore.firebase.FirebaseUserRepository;
import com.example.campuscore.models.UserModel;

public class NavigationUtils {
    private NavigationUtils() {
    }

    public static void openDashboard(Activity activity, UserModel user) {
        Intent intent;
        String role = user.getRole().toLowerCase();
        if (FirebaseUserRepository.ROLE_ADMIN.equals(role)) {
            intent = new Intent(activity, AdminDashboardActivity.class);
        } else if (FirebaseUserRepository.ROLE_TEACHER.equals(role)) {
            intent = new Intent(activity, TeacherDashboardActivity.class);
        } else {
            intent = new Intent(activity, StudentDashboardActivity.class);
        }
        intent.putExtra(IntentConstants.EXTRA_USER_NAME, user.getName());
        intent.putExtra(IntentConstants.EXTRA_USER_EMAIL, user.getEmail());
        intent.putExtra(IntentConstants.EXTRA_USER_ROLE, role);
        activity.startActivity(intent);
        activity.finish();
    }

    public static void openLoginAndClear(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public static void openVerifyEmailAndClear(Context context) {
        Intent intent = new Intent(context, VerifyEmailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
