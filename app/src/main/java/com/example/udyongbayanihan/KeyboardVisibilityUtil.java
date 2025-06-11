package com.example.udyongbayanihan;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;

public class KeyboardVisibilityUtil {

    private final View rootView;
    private final Window window;
    private OnKeyboardVisibilityListener onKeyboardVisibilityListener;
    private int lastVisibleDecorViewHeight;

    public KeyboardVisibilityUtil(Activity activity) {
        this.rootView = activity.findViewById(android.R.id.content);
        this.window = activity.getWindow();
        this.lastVisibleDecorViewHeight = 0;
    }

    public void setKeyboardVisibilityListener(OnKeyboardVisibilityListener listener) {
        this.onKeyboardVisibilityListener = listener;
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(getViewTreeObserver());
    }

    private ViewTreeObserver.OnGlobalLayoutListener getViewTreeObserver() {
        return () -> {
            Rect r = new Rect();
            window.getDecorView().getWindowVisibleDisplayFrame(r);
            int visibleDecorViewHeight = r.height();

            // If lastVisibleDecorViewHeight is 0, set it to visibleDecorViewHeight and return
            if (lastVisibleDecorViewHeight == 0) {
                lastVisibleDecorViewHeight = visibleDecorViewHeight;
                return;
            }

            // If the visible height has changed and is smaller than before, keyboard is shown
            if (lastVisibleDecorViewHeight > visibleDecorViewHeight + 200) {
                // Keyboard is shown
                if (onKeyboardVisibilityListener != null) {
                    onKeyboardVisibilityListener.onKeyboardVisibilityChanged(true);
                }
            } else if (lastVisibleDecorViewHeight < visibleDecorViewHeight - 200) {
                // Keyboard is hidden
                if (onKeyboardVisibilityListener != null) {
                    onKeyboardVisibilityListener.onKeyboardVisibilityChanged(false);
                }
            }

            lastVisibleDecorViewHeight = visibleDecorViewHeight;
        };
    }

    public interface OnKeyboardVisibilityListener {
        void onKeyboardVisibilityChanged(boolean isVisible);
    }
}