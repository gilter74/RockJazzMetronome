package com.rockjazz.metronome;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MetronomeKeepAliveService extends Service {
    public static final String ACTION_START = "com.rockjazz.metronome.action.START_METRONOME";
    public static final String ACTION_FORCE_STOP = "com.rockjazz.metronome.action.FORCE_STOP_METRONOME";

    public static final String EXTRA_BPM = "bpm";
    public static final String EXTRA_BEATS = "beats";
    public static final String EXTRA_DENOMINATOR = "denominator";
    public static final String EXTRA_SUBDIVISIONS = "subdivisions";
    public static final String EXTRA_VOLUME = "volume";
    public static final String EXTRA_ACCENT = "accent";
    public static final String EXTRA_SOUND = "sound";

    private static final String CHANNEL_ID = "metrotuner_metronome_playback";
    private static final int NOTIFICATION_ID = 3801;

    private PowerManager.WakeLock wakeLock;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickTask;
    private ToneGenerator toneGenerator;

    private int bpm = 120;
    private int beatsPerBar = 4;
    private int denominator = 4;
    private int subdivisions = 1;
    private int volume = 80;
    private boolean accentEnabled = true;
    private String soundType = "mechanical";
    private long tickIndex = 0L;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        scheduler = Executors.newSingleThreadScheduledExecutor();
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

        if (intent != null) {
            bpm = clamp(intent.getIntExtra(EXTRA_BPM, bpm), 20, 300);
            beatsPerBar = clamp(intent.getIntExtra(EXTRA_BEATS, beatsPerBar), 1, 16);
            denominator = allowedDenominator(intent.getIntExtra(EXTRA_DENOMINATOR, denominator));
            subdivisions = clamp(intent.getIntExtra(EXTRA_SUBDIVISIONS, subdivisions), 1, 4);
            volume = clamp(intent.getIntExtra(EXTRA_VOLUME, volume), 0, 100);
            accentEnabled = intent.getBooleanExtra(EXTRA_ACCENT, accentEnabled);
            String requestedSound = intent.getStringExtra(EXTRA_SOUND);
            if (requestedSound != null) soundType = requestedSound;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        configureToneGenerator();
        scheduleTicks();
        return START_NOT_STICKY;
    }

    private synchronized void scheduleTicks() {
        if (tickTask != null) tickTask.cancel(false);
        tickIndex = 0L;
        double milliseconds = 60000.0 / bpm * (4.0 / denominator) / subdivisions;
        long periodNanos = Math.max(10_000_000L, (long) (milliseconds * 1_000_000.0));
        tickTask = scheduler.scheduleAtFixedRate(
                this::playTick, 45_000_000L, periodNanos, TimeUnit.NANOSECONDS);
    }

    private void playTick() {
        if (volume <= 0 || toneGenerator == null) return;

        long current = tickIndex++;
        int subdivisionIndex = (int) (current % subdivisions);
        int beatIndex = (int) ((current / subdivisions) % beatsPerBar);
        boolean accent = accentEnabled && beatIndex == 0 && subdivisionIndex == 0;

        int tone;
        int duration;
        if (accent) {
            tone = ToneGenerator.TONE_PROP_ACK;
            duration = 55;
        } else if ("wood".equals(soundType)) {
            tone = ToneGenerator.TONE_PROP_PROMPT;
            duration = 48;
        } else if ("digital".equals(soundType)) {
            tone = ToneGenerator.TONE_PROP_BEEP2;
            duration = 32;
        } else if ("bell".equals(soundType)) {
            tone = ToneGenerator.TONE_PROP_ACK;
            duration = 105;
        } else if ("drum".equals(soundType)) {
            tone = ToneGenerator.TONE_DTMF_1;
            duration = 65;
        } else if ("rim".equals(soundType)) {
            tone = ToneGenerator.TONE_PROP_NACK;
            duration = 38;
        } else {
            tone = ToneGenerator.TONE_PROP_BEEP;
            duration = 38;
        }

        double periodMs = 60000.0 / bpm * (4.0 / denominator) / subdivisions;
        duration = (int) Math.max(10, Math.min(duration, periodMs * 0.75));
        try {
            toneGenerator.startTone(tone, duration);
        } catch (RuntimeException ignored) {
            // Audio route may briefly change while the foreground service is running.
        }
    }

    private void configureToneGenerator() {
        if (toneGenerator != null) {
            try { toneGenerator.release(); } catch (RuntimeException ignored) {}
            toneGenerator = null;
        }
        if (volume > 0) {
            try {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, volume);
            } catch (RuntimeException ignored) {
                toneGenerator = null;
            }
        }
    }

    private int allowedDenominator(int value) {
        return (value == 1 || value == 2 || value == 4 || value == 8 || value == 16) ? value : 4;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "MetroTuner metronom", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Metronom çalışırken ekran kapalı olsa da ritmi sürdürür.");
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
                .setContentText("Metronom arka planda çalışıyor • " + bpm + " BPM")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(android.R.drawable.ic_media_pause, "Durdur", stopPendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        if (tickTask != null) tickTask.cancel(true);
        if (scheduler != null) scheduler.shutdownNow();
        if (toneGenerator != null) {
            try { toneGenerator.release(); } catch (RuntimeException ignored) {}
            toneGenerator = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (RuntimeException ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
