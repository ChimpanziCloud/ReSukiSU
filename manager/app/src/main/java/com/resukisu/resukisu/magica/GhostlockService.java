package com.resukisu.resukisu.magica;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.*;

public class GhostlockService extends Service {
    private static final String TAG = "GhostLock";
    private static final String CHANNEL = "ghostlock";
    private static volatile boolean running = false;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel ch = new NotificationChannel(CHANNEL, "GhostLock",
            NotificationManager.IMPORTANCE_HIGH);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ghostlock:root");
        wakeLock.acquire(600000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (running) return START_NOT_STICKY;
        Notification notif = new Notification.Builder(this, CHANNEL)
            .setContentTitle("GhostLock").setContentText("Jailbreaking...")
            .setSmallIcon(android.R.drawable.ic_lock_lock).setOngoing(true).build();
        startForeground(1, notif);
        new Thread(this::run).start();
        return START_NOT_STICKY;
    }

    private void run() {
        running = true;
        try {
            if (isRooted()) { Log.i(TAG, "already rooted"); done(); return; }

            File bin = new File(getApplicationInfo().nativeLibraryDir, "libghostlock.so");
            if (!bin.exists()) { Log.e(TAG, "no libghostlock.so"); done(); return; }

            extractAdbKey();
            copyToSharedPath(bin);

            String log = getCacheDir().getAbsolutePath() + "/ghostlock_boot.log";
            ProcessBuilder pb = new ProcessBuilder(bin.getAbsolutePath(), "--bootstrap");
            pb.redirectErrorStream(true);
            pb.redirectOutput(new File(log));
            Process proc = pb.start();
            Log.i(TAG, "exploit launched (foreground)");

            boolean rooted = false;
            // Wait for exploit to finish (runs in this process, not phantom)
            proc.waitFor(600, java.util.concurrent.TimeUnit.SECONDS);

            // Check if root was achieved
            for (int i = 0; i < 30; i++) {
                if (isRooted()) {
                    Log.i(TAG, "ROOT achieved");
                    rooted = true;
                    break;
                }
                Thread.sleep(2000);
            }
            showResult(rooted ? "Root successful" : "Jailbreak failed");
        } catch (Exception e) {
            Log.e(TAG, "error", e);
            showResult("Error: " + e.getMessage());
        }
        done();
    }

    private boolean isRooted() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String out = r.readLine();
                return out != null && out.contains("uid=0");
            }
        } catch (Exception e) { return false; }
    }

    private void extractAdbKey() {
        File dst = new File("/data/local/tmp/a/adbkey");
        if (dst.exists()) return;
        try {
            new File("/data/local/tmp/a").mkdirs();
            try (InputStream in = getAssets().open("adbkey");
                 FileOutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[4096]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
        } catch (Exception e) { Log.w(TAG, "adbkey extract: " + e.getMessage()); }
    }

    private void copyToSharedPath(File src) {
        File dst = new File("/data/local/tmp/a/e");
        try {
            new File("/data/local/tmp/a").mkdirs();
            try (InputStream in = new FileInputStream(src);
                 FileOutputStream out = new FileOutputStream(dst)) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            dst.setExecutable(true);
        } catch (Exception e) { Log.w(TAG, "copy to shared: " + e.getMessage()); }
    }

    private void showResult(String text) {
        Notification notif = new Notification.Builder(this, CHANNEL)
            .setContentTitle("GhostLock").setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock).setOngoing(false).build();
        getSystemService(NotificationManager.class).notify(2, notif);
    }

    private void done() {
        running = false;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_DETACH);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
