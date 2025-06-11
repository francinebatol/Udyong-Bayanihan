package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;

public class UserDataHelper {
    public static void passUserData(Intent intent, String userId, String userType, Bundle details) {
        if (userType.equals("admin")) {
            // Pass admin-specific data
            intent.putExtra("userId", userId);
            intent.putExtra("userType", "admin");
            intent.putExtra("amAccountId", userId);
            intent.putExtra("amUsername", details.getString("amUsername"));
            intent.putExtra("amEmail", details.getString("amEmail"));
            intent.putExtra("amDetails", details);
        } else {
            // Pass user-specific data
            intent.putExtra("userId", userId);
            intent.putExtra("userType", "user");
            intent.putExtra("uaddressId", details.getString("uaddressId"));
            intent.putExtra("unameId", details.getString("unameId"));
            intent.putExtra("uotherDetails", details.getString("uotherDetails"));
        }
    }

    public static Bundle extractUserData(Intent intent) {
        Bundle data = new Bundle();
        String userType = intent.getStringExtra("userType");
        String userId = intent.getStringExtra("userId");

        data.putString("userId", userId);
        data.putString("userType", userType);

        if ("admin".equals(userType)) {
            Bundle amDetails = intent.getBundleExtra("amDetails");
            if (amDetails != null) {
                data.putAll(amDetails);
            }
            data.putString("amAccountId", intent.getStringExtra("amAccountId"));
            data.putString("amUsername", intent.getStringExtra("amUsername"));
            data.putString("amEmail", intent.getStringExtra("amEmail"));
        } else {
            data.putString("uaddressId", intent.getStringExtra("uaddressId"));
            data.putString("unameId", intent.getStringExtra("unameId"));
            data.putString("uotherDetails", intent.getStringExtra("uotherDetails"));
        }

        return data;
    }
}