package com.android.internal.policy.impl;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;/**add by xiezhongtian*/
import android.graphics.Rect;
import android.widget.LinearLayout;
import android.widget.TextView;/**end*/
import android.view.Window.Callback;
import android.view.WindowManager.LayoutParams;

public class DialogFrameView extends WindowFrameView {
    private static int DIALOG_FRAME_DEFAULT_FLAGS = 2;
    private static int DIALOG_FRAME_SUPPORTED_FLAGS = -2147479785;
    private Window mWindow;
    OnClickListener mClick = new OnClickListener() {
        public void onClick(View v) {
            if (v == DialogFrameView.this.mCloseBtn) {
                Callback cb = DialogFrameView.this.mWindow.getCallback();
                if (cb != null) {
                    try {
                        cb.dispatchWindowCloseClicked(false);
                        DialogFrameView.this.mClosed = true;
                    } catch (Throwable th) {
                    }
                }
            }
        }
    };
    OnTouchListener mHeaderTouch = new OnTouchListener() {
        private int mLastX = 0;
        private int mLastY = 0;

        public boolean onTouch(View v, MotionEvent event) {
            int rawX = (int) event.getRawX();
            int rawY = (int) event.getRawY();
            if (event.getAction() == 0) {
                this.mLastX = (int) event.getRawX();
                this.mLastY = (int) event.getRawY();
            } else if (2 == event.getAction()) {
                //LayoutParams lp = DialogFrameView.this.mWindow.getAttributes();/**due to compile errror*/
                //lp.x += rawX - this.mLastX;
                //lp.y += rawY - this.mLastY;
                //DialogFrameView.this.mFrame.offset(rawX - this.mLastX, rawY - this.mLastY);
                //DialogFrameView.this.mWindow.setAttributes(lp);
                this.mLastX = rawX;
                this.mLastY = rawY;
            } else if (3 == event.getAction()) {
                this.mLastX = 0;
                this.mLastY = 0;
            }
            return true;
        }
    };

    public DialogFrameView(Context context, View decorView, int flags) {
        super(context, decorView, 17367118, flags);
        this.mHeader.setOnTouchListener(this.mHeaderTouch);
        this.mCloseBtn.setOnClickListener(this.mClick);
        if (flags == 0) {
            flags = DIALOG_FRAME_DEFAULT_FLAGS;
        }
        setFlags(flags);
        this.mTitle.setVisibility(0);
    }

    public int getSupportedFrameFlag() {
        return DIALOG_FRAME_SUPPORTED_FLAGS;
    }
}
