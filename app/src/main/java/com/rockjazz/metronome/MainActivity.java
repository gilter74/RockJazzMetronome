package com.rockjazz.metronome;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {
    private static final int REQUEST_AUDIO_PERMISSION = 4101;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 4102;
    private static final String ASSET_HOST = "https://appassets.androidplatform.net";

    private WebView webView;
    private PermissionRequest pendingAudioRequest;
    private boolean pendingNativeMicCallback = false;
    private WebViewAssetLoader assetLoader;
    private BroadcastReceiver stopReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        webView.setBackgroundColor(0xFF080D16);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(
                    WebView view, String url) {
                return assetLoader.shouldInterceptRequest(Uri.parse(url));
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermissionRequest(request));
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingAudioRequest == request) pendingAudioRequest = null;
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        webView.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);
        webView.addJavascriptInterface(new MetroTunerBridge(), "MetroTunerBridge");
        setContentView(webView);

        registerStopReceiver();
        requestNotificationPermissionIfNeeded();
        webView.loadUrl(ASSET_HOST + "/assets/index.html");
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        Uri origin = request.getOrigin();
        boolean trustedLocalOrigin = origin != null
                && "https".equalsIgnoreCase(origin.getScheme())
                && "appassets.androidplatform.net".equalsIgnoreCase(origin.getHost());
        if (!trustedLocalOrigin) {
            request.deny();
            return;
        }
        boolean asksForAudio = false;
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) asksForAudio = true;
        }
        if (!asksForAudio) {
            request.deny();
            return;
        }
        if (Build.VERSION.SDK_INT < 23
                || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
        } else {
            if (pendingAudioRequest != null && pendingAudioRequest != request) {
                try { pendingAudioRequest.deny(); } catch (RuntimeException ignored) {}
            }
            pendingAudioRequest = request;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (pendingAudioRequest != null) {
                try {
                    if (granted) {
                        pendingAudioRequest.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                    } else {
                        pendingAudioRequest.deny();
                    }
                } catch (RuntimeException ignored) {
                    // The WebView may have cancelled the request while Android displayed its permission dialog.
                }
                pendingAudioRequest = null;
            }

            if (pendingNativeMicCallback && webView != null) {
                pendingNativeMicCallback = false;
                webView.evaluateJavascript(
                        "window.onNativeMicPermissionResult && window.onNativeMicPermissionResult(" + granted + ")", null);
            }
        }
    }

    private void registerStopReceiver() {
        stopReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (webView != null) {
                    runOnUiThread(() -> webView.evaluateJavascript(
                            "document.getElementById('resetBtn')?.click();", null));
                }
            }
        };
        IntentFilter filter = new IntentFilter(MetronomeKeepAliveService.ACTION_FORCE_STOP);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(stopReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stopReceiver, filter);
        }
    }

    @Override
    protected void onDestroy() {
        // If the Activity is actually closed, stop the keep-alive service to avoid a stale wake lock.
        stopService(new Intent(this, MetronomeKeepAliveService.class));
        if (stopReceiver != null) {
            try { unregisterReceiver(stopReceiver); } catch (Exception ignored) {}
        }
        if (webView != null) {
            webView.removeJavascriptInterface("MetroTunerBridge");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private class MetroTunerBridge {
        @JavascriptInterface
        public boolean isAndroidApp() {
            return true;
        }

        @JavascriptInterface
        public boolean hasRecordAudioPermission() {
            return Build.VERSION.SDK_INT < 23
                    || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface
        public void requestRecordAudioPermission() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT < 23
                        || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    webView.evaluateJavascript("window.onNativeMicPermissionResult && window.onNativeMicPermissionResult(true)", null);
                } else {
                    pendingNativeMicCallback = true;
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
                }
            });
        }

        @JavascriptInterface
        public void startKeepAlive(int bpm, int beatsPerBar, int denominator,
                                   int subdivisions, int volume, boolean accent, String soundType) {
            startOrUpdateNativeMetronome(bpm, beatsPerBar, denominator, subdivisions, volume, accent, soundType);
        }

        @JavascriptInterface
        public void updateMetronome(int bpm, int beatsPerBar, int denominator,
                                    int subdivisions, int volume, boolean accent, String soundType) {
            startOrUpdateNativeMetronome(bpm, beatsPerBar, denominator, subdivisions, volume, accent, soundType);
        }

        @JavascriptInterface
        public void stopKeepAlive() {
            stopService(new Intent(MainActivity.this, MetronomeKeepAliveService.class));
        }

        private void startOrUpdateNativeMetronome(int bpm, int beatsPerBar, int denominator,
                                                   int subdivisions, int volume, boolean accent,
                                                   String soundType) {
            Intent intent = new Intent(MainActivity.this, MetronomeKeepAliveService.class);
            intent.setAction(MetronomeKeepAliveService.ACTION_START);
            intent.putExtra(MetronomeKeepAliveService.EXTRA_BPM, bpm);
            intent.putExtra(MetronomeKeepAliveService.EXTRA_BEATS, beatsPerBar);
            intent.putExtra(MetronomeKeepAliveService.EXTRA_DENOMINATOR, denominator);
            intent.putExtra(MetronomeKeepAliveService.EXTRA_SUBDIVISIONS, subdivisions);
            intent.putExtra(MetronomeKeepAliveService.EXTRA_VOLUME, volume);
            intent.putExtra(MetronomeKeepAliveService.EXTRA_ACCENT, accent);
            intent.putExtra(MetronomeKeepAliveService.EXTRA_SOUND, soundType);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }
}
