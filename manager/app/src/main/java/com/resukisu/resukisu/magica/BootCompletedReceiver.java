package com.resukisu.resukisu.magica;

import static com.resukisu.resukisu.magica.AppZygotePreload.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        var action = intent.getAction();
        // Skip LOCKED_BOOT_COMPLETED — credential-encrypted storage not available yet,
        // KernelSUApplication will crash. Only handle BOOT_COMPLETED (after unlock).
        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            return;
        }
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !"com.resukisu.resukisu.magica.LAUNCH".equals(action)) {
            return;
        }
        try {
            // If KSU not loaded, try GhostLock exploit first
            boolean ksuLoaded = false;
            try {
                java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.FileReader("/proc/modules"));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("kernelsu ")) { ksuLoaded = true; break; }
                }
                br.close();
            } catch (Exception ignored) {}

            if (!ksuLoaded) {
                // Check if su is already available (e.g., after soft reboot)
                boolean suAvailable = false;
                try {
                    Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
                    java.io.BufferedReader suReader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(proc.getInputStream()));
                    String output = suReader.readLine();
                    suReader.close();
                    if (!proc.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        proc.destroyForcibly();
                    }
                    if (output != null && output.contains("uid=0")) {
                        suAvailable = true;
                    }
                } catch (Exception ignored) {}

                if (!suAvailable) {
                    context.startForegroundService(new Intent(context, GhostlockService.class));
                    Log.i(TAG, "GhostlockService started (KSU not loaded, su not available)");
                } else {
                    Log.i(TAG, "Skipping GhostlockService (su already available)");
                }
            }

            context.startService(new Intent(context, MagicaService.class));
            Log.i(TAG, "MagicaService started from boot action: " + action);
        } catch (Throwable e) {
            Log.e(TAG, "Failed to start service from boot action: " + action, e);
        }
    }
}
