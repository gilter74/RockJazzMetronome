package com.rockjazz.metronome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class MetronomeKeepAliveService extends Service {
    public static final String ACTION_START = "com.rockjazz.metronome.action.START_METRONOME";
    public static final String ACTION_FORCE_STOP = "com.rockjazz.metronome.action.FORCE_STOP_METRONOME";
    private static final String CHANNEL_ID = "metrotuner_metronome_playback";
    private static final int NOTIFICATION_ID = 3801;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "RockJazzMetroTuner:MetronomePlayback");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_FORCE_STOP.equals(intent.getAction())) {
            sendBroadcast(new Intent(ACTION_FORCE_STOP).setPackage(getPackageName()));
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, buildNotification());
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "MetroTuner metronom", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Metronom çalışırken arka plan sesini sürdürür.");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent stopIntent = new Intent(this, MetronomeKeepAliveService.class);
        stopIntent.setAction(ACTION_FORCE_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 3802, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("RockJazz MetroTuner")
                .setContentText("Metronom arka planda çalışıyor")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_media_pause, "Durdur", stopPendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (RuntimeException ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
