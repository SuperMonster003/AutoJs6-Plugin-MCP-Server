package io.github.supermonster003.autojs6.plugin.mcp.server;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Locale;

/** The grant belongs to the plugin UID, including its runtime subprocesses. */
public final class LocalNetworkAccess {
    public static final String PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK";
    private LocalNetworkAccess() {}

    public static boolean isGranted(Context context) {
        return Build.VERSION.SDK_INT < 37
                || context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isPermissionFailure(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("eperm") || lower.contains("eacces")
                || lower.contains("permission denied") || lower.contains("operation not permitted")
                || lower.contains("timeout") || lower.contains("timed out") || lower.contains("fetch failed");
    }

    public static String explainFailure(Context context, String message) {
        if (isGranted(context) || !isPermissionFailure(message)) return message;
        return message + "\n" + context.getString(R.string.local_network_failure_hint);
    }
}
