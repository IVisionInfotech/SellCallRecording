package com.sellcallrecording.permission;

import android.content.Context;
import android.widget.Toast;

import java.util.ArrayList;

public abstract class PermissionHandler {

    public abstract void onPermissionGranted();

    public void onPermissionDenied(Context context, ArrayList<String> deniedPermissions) {
        if (PermissionsHandler.loggingEnabled) {
            StringBuilder builder = new StringBuilder();
            builder.append("Denied:");
            for (String permission : deniedPermissions) {
                builder.append(" ");
                builder.append(permission);
            }
            PermissionsHandler.log(builder.toString());
        }
        Toast.makeText(context, "Permission Denied.", Toast.LENGTH_SHORT).show();
    }


    public boolean onPermissionNeverAskAgain(Context context, ArrayList<String> blockedList) {
        if (PermissionsHandler.loggingEnabled) {
            StringBuilder builder = new StringBuilder();
            builder.append("Set not to ask again:");
            for (String permission : blockedList) {
                builder.append(" ");
                builder.append(permission);
            }
            PermissionsHandler.log(builder.toString());
        }
        return true;
    }
    public void onPermissionDeniedOnce(Context context, ArrayList<String> justBlockedList,
                                       ArrayList<String> deniedPermissions) {
        if (PermissionsHandler.loggingEnabled) {
            StringBuilder builder = new StringBuilder();
            builder.append("Just set not to ask again:");
            for (String permission : justBlockedList) {
                builder.append(" ");
                builder.append(permission);
            }
            PermissionsHandler.log(builder.toString());
        }
        onPermissionDenied(context, deniedPermissions);
    }

}
