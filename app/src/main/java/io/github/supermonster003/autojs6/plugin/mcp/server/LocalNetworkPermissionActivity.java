package io.github.supermonster003.autojs6.plugin.mcp.server;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextThemeWrapper;

/** User-initiated permission handoff. No launcher entry, content page or runtime startup. */
public final class LocalNetworkPermissionActivity extends Activity {
    private static final int PERMISSION_REQUEST = 37;
    private static final int SETTINGS_REQUEST = 38;
    private int phase;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT < 37 || LocalNetworkAccess.isGranted(this)) {
            finishResult();
            return;
        }
        phase = state == null ? 0 : state.getInt("phase", 0);
        if (state == null) {
            // Let Android decide whether to show the system dialog, including after an upgrade.
            requestPermissions(new String[]{LocalNetworkAccess.PERMISSION}, PERMISSION_REQUEST);
        } else if (phase == 1) {
            showSettingsDialog();
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != PERMISSION_REQUEST) return;
        if (results.length == 0 || LocalNetworkAccess.isGranted(this) || shouldShowRequestPermissionRationale(LocalNetworkAccess.PERMISSION)) {
            finishResult();
        } else {
            showSettingsDialog();
        }
    }

    private void showSettingsDialog() {
        phase = 1;
        boolean night = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int theme = night ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert;
        new AlertDialog.Builder(new ContextThemeWrapper(this, theme))
                .setTitle(R.string.local_network_title)
                .setMessage(R.string.local_network_explanation)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> finishResult())
                .setPositiveButton(R.string.local_network_settings, (dialog, which) -> {
                    phase = 2;
                    startActivityForResult(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName())), SETTINGS_REQUEST);
                })
                .setOnCancelListener(dialog -> finishResult())
                .show();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == SETTINGS_REQUEST) finishResult();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("phase", phase);
        super.onSaveInstanceState(state);
    }

    private void finishResult() {
        setResult(LocalNetworkAccess.isGranted(this) ? RESULT_OK : RESULT_CANCELED);
        finish();
    }
}
