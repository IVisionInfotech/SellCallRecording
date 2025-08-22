package com.sellcallrecording.permission;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <pre>
 * Helper class for handling runtime permissions.
 * Created on 12/11/2022 on 9:32 PM
 * </pre>
 *
 * @author Vinod Baste
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class PermissionsHandler {

    static boolean loggingEnabled = true;

    /**
     * Disable logs.
     */
    public static void disableLogging() {
        loggingEnabled = false;
    }

    static void log(String message) {
        if (loggingEnabled) Log.d("PermissionHandler", message);
    }

    public static void requestPermission(Context context, String permission, String rationale,
                                         PermissionHandler handler) {
        requestPermission(context, new String[]{permission}, rationale, null, handler);
    }
    public static void requestPermission(Context context, String permission, int rationaleId,
                                         PermissionHandler handler) {
        String rationale = null;
        try {
            rationale = context.getString(rationaleId);
        } catch (Exception ignored) {
        }
        requestPermission(context, new String[]{permission}, rationale, null, handler);
    }
    public static void requestPermission(final Context context, String[] permissions, String rationale,
                                         Options options, final PermissionHandler handler) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            handler.onPermissionGranted();
            log("Android version < 23");
        } else {
            Set<String> permissionsSet = new LinkedHashSet<>();
            Collections.addAll(permissionsSet, permissions);
            boolean allPermissionProvided = true;
            for (String aPermission : permissionsSet) {
                if (context.checkSelfPermission(aPermission) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionProvided = false;
                    break;
                }
            }

            if (allPermissionProvided) {
                handler.onPermissionGranted();
                PermissionsActivity.permissionHandler = null;
            } else {
                PermissionsActivity.permissionHandler = handler;
                ArrayList<String> permissionsList = new ArrayList<>(permissionsSet);

                Intent intent = new Intent(context, PermissionsActivity.class)
                        .putExtra(PermissionsActivity.EXTRA_PERMISSIONS, permissionsList)
                        .putExtra(PermissionsActivity.EXTRA_RATIONALE, rationale)
                        .putExtra(PermissionsActivity.EXTRA_OPTIONS, options);
                if (options != null && options.createNewTask) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
            }
        }
    }
    public static void requestPermission(final Context context, String[] permissions, int rationaleId,
                                         Options options, final PermissionHandler handler) {
        String rationale = null;
        try {
            rationale = context.getString(rationaleId);
        } catch (Exception ignored) {
        }
        requestPermission(context, permissions, rationale, options, handler);
    }

    public static class Options implements Serializable {

        String settingsText = "Settings";
        String rationaleDialogTitle = "Permissions Required";
        String settingsDialogTitle = "Permissions Required";
        String settingsDialogMessage = "Required permission(s) have been set" +
                " not to ask again! Please provide them from settings.";
        boolean sendBlockedToSettings = true;
        boolean createNewTask = false;

        public Options setSettingsText(String settingsText) {
            this.settingsText = settingsText;
            return this;
        }

        public Options setCreateNewTask(boolean createNewTask) {
            this.createNewTask = createNewTask;
            return this;
        }

        public Options setRationaleDialogTitle(String rationaleDialogTitle) {
            this.rationaleDialogTitle = rationaleDialogTitle;
            return this;
        }

        public Options setSettingsDialogTitle(String settingsDialogTitle) {
            this.settingsDialogTitle = settingsDialogTitle;
            return this;
        }

        public Options setSettingsDialogMessage(String settingsDialogMessage) {
            this.settingsDialogMessage = settingsDialogMessage;
            return this;
        }

        public Options sendDontAskAgainToSettings(boolean send) {
            sendBlockedToSettings = send;
            return this;
        }
    }

}
