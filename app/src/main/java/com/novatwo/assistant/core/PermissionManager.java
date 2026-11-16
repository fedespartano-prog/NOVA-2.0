
package com.novatwo.assistant.core;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class PermissionManager {

    private PermissionManager() {
        // Classe di utilità.
    }

    public static boolean hasMicrophonePermission(Activity activity) {
        return ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestMicrophonePermission(Activity activity) {
        if (!hasMicrophonePermission(activity)) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    Constants.REQUEST_MICROPHONE
            );
        }
    }
}
