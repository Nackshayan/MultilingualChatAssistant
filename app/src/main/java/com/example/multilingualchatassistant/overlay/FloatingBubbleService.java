package com.example.multilingualchatassistant.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.multilingualchatassistant.R;
import com.example.multilingualchatassistant.ui.MainActivity;

public class FloatingBubbleService extends Service {

    private static final String TAG = "BUBBLE";
    private static final String CHANNEL_ID = "bubble_channel";
    private static final int NOTIF_ID = 101;

    private WindowManager windowManager;
    private View bubbleView;
    private WindowManager.LayoutParams params;

    private int initialX, initialY;
    private float initialTouchX, initialTouchY;

    private boolean isDragging = false;
    private static final int DRAG_THRESHOLD_PX = 10;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate called");

        // ✅ Must run before heavy work
        startAsForeground();

        boolean canOverlay = Settings.canDrawOverlays(this);
        Log.d(TAG, "canDrawOverlays=" + canOverlay);

        if (!canOverlay) {
            Log.d(TAG, "Overlay permission missing -> stopSelf()");
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        bubbleView = new ImageView(this);
        ((ImageView) bubbleView).setImageResource(R.mipmap.ic_launcher_round);

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 200;

        try {
            Log.d(TAG, "Adding bubble view now...");
            windowManager.addView(bubbleView, params);
            Log.d(TAG, "Bubble view added OK");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add bubble view: " + e.getMessage(), e);
            stopSelf();
            return;
        }

        bubbleView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    isDragging = false;
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - initialTouchX;
                    float dy = event.getRawY() - initialTouchY;

                    if (Math.abs(dx) > DRAG_THRESHOLD_PX || Math.abs(dy) > DRAG_THRESHOLD_PX) {
                        isDragging = true;
                    }

                    params.x = initialX + (int) dx;
                    params.y = initialY + (int) dy;

                    try {
                        if (bubbleView.getParent() != null) {
                            windowManager.updateViewLayout(bubbleView, params);
                        }
                    } catch (Exception ignored) {}
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        Log.d(TAG, "Bubble tapped -> open MainActivity");
                        Intent intent = new Intent(FloatingBubbleService.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    return true;
            }
            return false;
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand called");
        return START_STICKY;
    }

    private void startAsForeground() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Bubble",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the floating chat bubble running");
            if (nm != null) nm.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Chat bubble active")
                .setContentText("Tap to return to chat")
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // ✅ IMPORTANT: provide the type on API 29+ (matches manifest dataSync)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, notification);
        }

        Log.d(TAG, "Foreground notification started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy called");

        try {
            if (bubbleView != null && bubbleView.getParent() != null && windowManager != null) {
                windowManager.removeView(bubbleView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error removing bubble: " + e.getMessage(), e);
        }

        bubbleView = null;
        windowManager = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
