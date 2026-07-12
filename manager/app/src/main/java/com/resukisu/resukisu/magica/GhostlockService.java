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
            NotificationManager.IMPORTANCE_LOW);
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
            String cmd = "setsid " + bin.getAbsolutePath() + " --bootstrap > " + log + " 2>&1 &";
            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            Log.i(TAG, "exploit launched");

            for (int i = 0; i < 300; i++) {
                Thread.sleep(2000);
                if (isRooted()) {
                    Log.i(TAG, "ROOT achieved");
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error", e);
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

    private void done() {
        running = false;
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        stopForeground(STOP_FOREGROUND_DETACH);
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
