package com.example.multilingualchatassistant.overlay;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.multilingualchatassistant.R;
import com.example.multilingualchatassistant.ui.MainActivity;

public class FloatingBubbleService extends Service {

    private WindowManager windowManager;
    private View bubbleView;
    private View closeTargetView;

    private WindowManager.LayoutParams bubbleParams;
    private WindowManager.LayoutParams closeParams;

    private long touchDownTime;
    private boolean isDragging = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // If no overlay permission, just stop
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // Inflate your bubble layout (layout_floating_bubble.xml)
        bubbleView = LayoutInflater.from(this)
                .inflate(R.layout.layout_floating_bubble, null);
        ImageView ivBubble = bubbleView.findViewById(R.id.ivBubble);

        int size = dp(45);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        bubbleParams = new WindowManager.LayoutParams(
                size,
                size,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        bubbleParams.gravity = Gravity.TOP | Gravity.START;
        bubbleParams.x = dp(16);
        bubbleParams.y = dp(120);

        windowManager.addView(bubbleView, bubbleParams);

        ivBubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchDownTime = System.currentTimeMillis();
                        isDragging = false;

                        initialX = bubbleParams.x;
                        initialY = bubbleParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();

                        showCloseTarget();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;

                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isDragging = true;
                        }

                        bubbleParams.x = initialX + (int) dx;
                        bubbleParams.y = initialY + (int) dy;

                        if (windowManager != null && bubbleView != null) {
                            windowManager.updateViewLayout(bubbleView, bubbleParams);
                        }

                        updateCloseTargetHighlight();
                        return true;

                    case MotionEvent.ACTION_UP:
                        long clickDuration = System.currentTimeMillis() - touchDownTime;

                        // ✅ IMPORTANT: check drop area BEFORE hiding the target
                        boolean droppedInClose = isDroppedInCloseArea();

                        hideCloseTarget();

                        if (!isDragging && clickDuration < 200) {
                            // Simple tap → open app + remove bubble
                            playClickAnimation(ivBubble, () -> openMainApp());
                        } else {
                            if (droppedInClose) {
                                // Dropped on trash → animate & destroy
                                playDismissAnimation(ivBubble, () -> destroyBubble());
                            } else {
                                // Snap to nearest edge
                                snapBubbleToEdge();
                            }
                        }
                        return true;
                }
                return false;
            }
        });
    }

    // -------- open app & destroy bubble --------

    private void openMainApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        // remove bubble service after opening app
        destroyBubble();
    }

    private void destroyBubble() {
        stopSelf();   // onDestroy() will remove views
    }

    // -------- trash / close target --------

    private void showCloseTarget() {
        if (windowManager == null || closeTargetView != null) return;

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        closeTargetView = LayoutInflater.from(this)
                .inflate(R.layout.overlay_close_target, null);

        int size = dp(72);
        closeParams = new WindowManager.LayoutParams(
                size,
                size,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        closeParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        closeParams.y = dp(24);

        windowManager.addView(closeTargetView, closeParams);
    }

    private void hideCloseTarget() {
        if (windowManager != null && closeTargetView != null) {
            windowManager.removeView(closeTargetView);
            closeTargetView = null;
            closeParams = null;
        }
    }

    private boolean isDroppedInCloseArea() {
        if (closeTargetView == null || bubbleView == null) return false;

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;

        int bubbleW = bubbleView.getWidth() > 0 ? bubbleView.getWidth() : dp(45);
        int bubbleH = bubbleView.getHeight() > 0 ? bubbleView.getHeight() : dp(45);

        int bubbleCenterX = bubbleParams.x + bubbleW / 2;
        int bubbleCenterY = bubbleParams.y + bubbleH / 2;

        int targetCenterX = screenW / 2;
        int targetCenterY = screenH - dp(72); // approx centre of trash icon

        int dx = bubbleCenterX - targetCenterX;
        int dy = bubbleCenterY - targetCenterY;

        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < dp(90);  // distance threshold
    }

    private void updateCloseTargetHighlight() {
        if (closeTargetView == null) return;

        if (isDroppedInCloseArea()) {
            closeTargetView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(80).start();
        } else {
            closeTargetView.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
        }
    }

    // -------- animations --------

    private void snapBubbleToEdge() {
        if (bubbleView == null || bubbleParams == null || windowManager == null) return;

        int screenW = getResources().getDisplayMetrics().widthPixels;
        int bubbleW = bubbleView.getWidth() > 0 ? bubbleView.getWidth() : dp(45);

        int middle = screenW / 2;
        int bubbleCenterX = bubbleParams.x + bubbleW / 2;

        int targetX = (bubbleCenterX < middle) ? 0 : (screenW - bubbleW);

        ValueAnimator animator = ValueAnimator.ofInt(bubbleParams.x, targetX);
        animator.setDuration(200);
        animator.addUpdateListener(animation -> {
            bubbleParams.x = (int) animation.getAnimatedValue();
            if (windowManager != null && bubbleView != null) {
                windowManager.updateViewLayout(bubbleView, bubbleParams);
            }
        });
        animator.start();
    }

    private void playClickAnimation(View v, Runnable endAction) {
        v.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(80)
                .withEndAction(() ->
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(80)
                                .withEndAction(endAction)
                                .start()
                )
                .start();
    }

    private void playDismissAnimation(View v, Runnable endAction) {
        v.animate()
                .scaleX(0.1f)
                .scaleY(0.1f)
                .alpha(0f)
                .setDuration(150)
                .withEndAction(endAction)
                .start();
    }

    // -------- lifecycle --------

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bubbleView != null && windowManager != null) {
            windowManager.removeView(bubbleView);
        }
        hideCloseTarget();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
