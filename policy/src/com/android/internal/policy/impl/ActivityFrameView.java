package com.android.internal.policy.impl;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceSession;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewRootImpl;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;

public class ActivityFrameView extends WindowFrameView {
    private static int ACTIVITY_FRAME_DEFAULT_FLAGS = 31;
    private static int ACTIVITY_FRAME_DEFAULT_SUPPORTED_FLAGS = -2130702529;
    private static int ACTIVITY_FRAME_TABLET_SUPPORTED_FLAGS = -2130702537;
    private static final boolean DEBUG = false;
    private static final String TAG = "ActivityFrameView";
    private boolean firstLayout = true;
    OnClickListener mClick = new OnClickListener() {
        public void onClick(View v) {
            if (v == ActivityFrameView.this.mMaxBtn) {
                ActivityFrameView.this.maxOrRestoreWindow();
            } else if (v == ActivityFrameView.this.mMinBtn) {
                try {
                    ActivityManagerNative.getDefault().moveActivityTaskToBack(ActivityFrameView.this.mWindow.mAppToken, true);
                } catch (RemoteException e) {
                }
            }
        }
    };
    SimpleOnGestureListener mCloseListener = new SimpleOnGestureListener() {
        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onSingleTapUp(android.view.MotionEvent r5) {
            /*
            r4 = this;
            r3 = 1;
            r1 = com.android.internal.policy.impl.ActivityFrameView.this;
            r1 = r1.mWindow;
            r0 = r1.getCallback();
            if (r0 == 0) goto L_0x0014;
        L_0x000b:
            r1 = 0;
            r1 = r0.dispatchWindowCloseClicked(r1);	 Catch:{ Throwable -> 0x0013 }
            if (r1 == 0) goto L_0x0014;
        L_0x0012:
            return r3;
        L_0x0013:
            r1 = move-exception;
        L_0x0014:
            r1 = com.android.internal.policy.impl.ActivityFrameView.this;	 Catch:{ Exception -> 0x0027 }
            r2 = 1;
            r1.mClosed = r2;	 Catch:{ Exception -> 0x0027 }
            r1 = android.app.ActivityManagerNative.getDefault();	 Catch:{ Exception -> 0x0027 }
            r2 = com.android.internal.policy.impl.ActivityFrameView.this;	 Catch:{ Exception -> 0x0027 }
            r2 = r2.mWindow;	 Catch:{ Exception -> 0x0027 }
            r2 = r2.mAppToken;	 Catch:{ Exception -> 0x0027 }
            r1.closeActivityTask(r2);	 Catch:{ Exception -> 0x0027 }
            goto L_0x0012;
        L_0x0027:
            r1 = move-exception;
            goto L_0x0012;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.policy.impl.ActivityFrameView.3.onSingleTapUp(android.view.MotionEvent):boolean");
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onMouseRightSingleTapUp(android.view.MotionEvent r5) {
            /*
            r4 = this;
            r3 = 1;
            r1 = com.android.internal.policy.impl.ActivityFrameView.this;
            r1 = r1.mWindow;
            r0 = r1.getCallback();
            if (r0 == 0) goto L_0x0014;
        L_0x000b:
            r1 = 1;
            r1 = r0.dispatchWindowCloseClicked(r1);	 Catch:{ Throwable -> 0x0013 }
            if (r1 == 0) goto L_0x0014;
        L_0x0012:
            return r3;
        L_0x0013:
            r1 = move-exception;
        L_0x0014:
            r1 = com.android.internal.policy.impl.ActivityFrameView.this;	 Catch:{ Exception -> 0x0027 }
            r2 = 1;
            r1.mClosed = r2;	 Catch:{ Exception -> 0x0027 }
            r1 = android.app.ActivityManagerNative.getDefault();	 Catch:{ Exception -> 0x0027 }
            r2 = com.android.internal.policy.impl.ActivityFrameView.this;	 Catch:{ Exception -> 0x0027 }
            r2 = r2.mWindow;	 Catch:{ Exception -> 0x0027 }
            r2 = r2.mAppToken;	 Catch:{ Exception -> 0x0027 }
            r1.closeActivityTask(r2);	 Catch:{ Exception -> 0x0027 }
            goto L_0x0012;
        L_0x0027:
            r1 = move-exception;
            goto L_0x0012;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.policy.impl.ActivityFrameView.3.onMouseRightSingleTapUp(android.view.MotionEvent):boolean");
        }
    };
    SurfaceSession mFxSession;
    GestureDetector mGestureDetector = new GestureDetector(getContext(), this.mCloseListener);
    OnTouchListener mHeaderTouch = new OnTouchListener() {
        private static final long sDoubleClickInterval = 500;
        private Rect mDownFrame = new Rect();
        private int mDownX = 0;
        private int mDownY = 0;
        private Rect mHalfResize = new Rect();
        private Rect mHalfRestoreFrame = new Rect();
        private long mLastTimeClick = 0;
        private int mLastX = 0;
        private int mLastY = 0;
        private boolean mTouchOnHeader = ActivityFrameView.DEBUG;
        private Rect r = new Rect();

        public boolean onTouch(View v, MotionEvent event) {
            int rawX = (int) event.getRawX();
            int rawY = (int) event.getRawY();
            if (ActivityFrameView.this.uiMode == 1) {
                return true;
            }
            if (event.getAction() == 0) {
                float StartThreshold = event.getToolType(0) == 3 ? 0.0f : (float) (ActivityFrameView.this.mFrameHeaderHeight / 3);
                if (event.getY() > ((float) ActivityFrameView.this.mFrameHeaderHeight) || ((float) rawY) < StartThreshold) {
                    Log.e(ActivityFrameView.TAG, "mHeader OnTouch Down outOf Frame:  X:" + rawX + " Y" + rawY + " X:" + event.getX() + "  WindowFrame:" + ActivityFrameView.this.mFrame.toString());
                    return ActivityFrameView.DEBUG;
                }
                this.mDownFrame.set(ActivityFrameView.this.mDecor.getViewRootImpl().mWinFrame);
                this.mTouchOnHeader = true;
                this.mDownX = (int) event.getRawX();
                this.mDownY = (int) event.getRawY();
                this.mLastX = this.mDownX;
                this.mLastY = this.mDownY;
                long time = System.currentTimeMillis();
                if (time - this.mLastTimeClick < sDoubleClickInterval) {
                    ActivityFrameView.this.maxOrRestoreWindow();
                    this.mLastTimeClick = 0;
                } else {
                    this.mLastTimeClick = time;
                }
            } else if (2 == event.getAction()) {
                if (!this.mTouchOnHeader) {
                    return ActivityFrameView.DEBUG;
                }
                if (this.mDownX == rawX && this.mDownY == rawY) {
                    return true;
                }
                try {
                    if (!ActivityFrameView.this.mMaximized) {
                        int dx = rawX - this.mDownX;
                        int dy = rawY - this.mDownY;
                        this.r.set(this.mDownFrame);
                        this.r.offset(dx, dy);
                        if (inResizingArea(rawX, rawY)) {
                            ActivityManagerNative.getDefault().showResizingFrame(this.mHalfResize);
                        } else {
                            if (!this.mHalfResize.isEmpty()) {
                                this.mHalfResize.setEmpty();
                                ActivityManagerNative.getDefault().hideResizingFrame();
                            }
                            if (ActivityFrameView.this.mInHalfSize) {
                                this.r.set(this.mHalfRestoreFrame);
                                this.r.left = rawX - (this.r.width() / 2);
                                if (this.r.left < (-ActivityFrameView.this.mBorderPadding.left)) {
                                    this.r.left = -ActivityFrameView.this.mBorderPadding.left;
                                }
                                this.r.right = this.r.left + this.mHalfRestoreFrame.width();
                                if (this.r.right > ActivityFrameView.this.mRealDisplayMetrics.widthPixels + ActivityFrameView.this.mBorderPadding.right) {
                                    this.r.right = ActivityFrameView.this.mRealDisplayMetrics.widthPixels + ActivityFrameView.this.mBorderPadding.right;
                                    this.r.left = this.r.right - this.mHalfRestoreFrame.width();
                                }
                                this.r.top = 0;
                                this.r.bottom = this.mHalfRestoreFrame.height();
                                ActivityFrameView.this.setWindowPos(this.r);
                                this.mDownFrame.set(this.r);
                                this.mDownX = rawX;
                                this.mDownY = rawY;
                                this.mLastX = rawX;
                                this.mLastY = rawY;
                                ActivityFrameView.this.mInHalfSize = ActivityFrameView.DEBUG;
                            } else if (ActivityFrameView.this.fitWindowInScreen(this.r)) {
                                ActivityManagerNative.getDefault().moveAppWindow(ActivityFrameView.this.mWindow.mAppToken, rawX - this.mLastX, rawY - this.mLastY);
                                this.mLastX = rawX;
                                this.mLastY = rawY;
                            }
                        }
                    } else if (this.mLastTimeClick != 0) {
                        Rect rect = ActivityManagerNative.getDefault().getRestoredAppWindowSize(ActivityFrameView.this.mContext.getPackageName());
                        this.r.left = rawX - (rect.width() / 2);
                        if (this.r.left < (-ActivityFrameView.this.mBorderPadding.left)) {
                            this.r.left = -ActivityFrameView.this.mBorderPadding.left;
                        }
                        this.r.right = this.r.left + rect.width();
                        if (this.r.right > ActivityFrameView.this.mRealDisplayMetrics.widthPixels + ActivityFrameView.this.mBorderPadding.right) {
                            this.r.right = ActivityFrameView.this.mRealDisplayMetrics.widthPixels + ActivityFrameView.this.mBorderPadding.right;
                            this.r.left = this.r.right - rect.width();
                        }
                        this.r.top = 0;
                        this.r.bottom = rect.height();
                        if (ActivityFrameView.this.fitWindowInScreen(this.r) && ActivityFrameView.this.setWindowPos(this.r)) {
                            ActivityFrameView.this.setMaximized(ActivityFrameView.DEBUG);
                            this.mDownFrame.set(this.r);
                            this.mDownX = rawX;
                            this.mDownY = rawY;
                            this.mLastX = rawX;
                            this.mLastY = rawY;
                        }
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                this.mLastTimeClick = 0;
            } else if (3 == event.getAction() || 1 == event.getAction()) {
                this.mTouchOnHeader = ActivityFrameView.DEBUG;
                if (!this.mHalfResize.isEmpty()) {
                   Rect rect = new Rect(this.mHalfResize.left - ActivityFrameView.this.mBorderPadding.left, this.mHalfResize.top - ActivityFrameView.this.mBorderPadding.top, this.mHalfResize.right + ActivityFrameView.this.mBorderPadding.right, this.mHalfResize.bottom + ActivityFrameView.this.mBorderPadding.bottom);
                    this.mHalfRestoreFrame.set(this.mDownFrame);
                    ActivityFrameView.this.setWindowPos(rect);
                    ActivityFrameView.this.mInHalfSize = true;
                    this.mHalfResize.setEmpty();
                }
            }
            return true;
        }

        private boolean inResizingArea(int x, int y) {
            int resizing_border = ActivityFrameView.this.mStretchBorderSize * 2;
            if (x < resizing_border) {
                this.mHalfResize.set(0, 0, ActivityFrameView.this.mRealDisplayMetrics.widthPixels / 2, ActivityFrameView.this.mRealDisplayMetrics.heightPixels - ActivityFrameView.this.mDockBarHeight);
                return true;
            } else if (x <= ActivityFrameView.this.mRealDisplayMetrics.widthPixels - resizing_border) {
                return ActivityFrameView.DEBUG;
            } else {
                this.mHalfResize.set(ActivityFrameView.this.mRealDisplayMetrics.widthPixels / 2, 0, ActivityFrameView.this.mRealDisplayMetrics.widthPixels, ActivityFrameView.this.mRealDisplayMetrics.heightPixels - ActivityFrameView.this.mDockBarHeight);
                return true;
            }
        }
    };
    private boolean mInHalfSize = DEBUG;
    private int mSupportedFrameFlag = ACTIVITY_FRAME_DEFAULT_SUPPORTED_FLAGS;

    public ActivityFrameView(Context context, View decorView, int flags) {
        super(context, decorView, 17367286, flags);
        this.mHeader.setOnTouchListener(this.mHeaderTouch);
        if (getContext().getPackageName().equalsIgnoreCase("com.chaozhuo.browser.x86") || getContext().getPackageName().equalsIgnoreCase("com.chaozhuo.browser")) {
            float scale = getContext().getResources().getDisplayMetrics().density;
            LayoutParams lp = (LayoutParams) this.mStretchBtn.getLayoutParams();
            lp.bottomMargin = (int) (scale + 0.5f);
            this.mStretchBtn.setLayoutParams(lp);
            lp = (LayoutParams) this.mMinBtn.getLayoutParams();
            lp.bottomMargin = (int) (scale + 0.5f);
            this.mMinBtn.setLayoutParams(lp);
            lp = (LayoutParams) this.mMaxBtn.getLayoutParams();
            lp.bottomMargin = (int) (scale + 0.5f);
            this.mMaxBtn.setLayoutParams(lp);
            lp = (LayoutParams) this.mCloseBtn.getLayoutParams();
            lp.bottomMargin = (int) (scale + 0.5f);
            this.mCloseBtn.setLayoutParams(lp);
        }
        this.mMinBtn.setOnClickListener(this.mClick);
        this.mCloseBtn.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                ActivityFrameView.this.mGestureDetector.onTouchEvent(event);
                return ActivityFrameView.DEBUG;
            }
        });
        this.mMaxBtn.setOnClickListener(this.mClick);
        updateUiMode(context.getResources().getConfiguration().system_mode);
        if (flags == 0) {
            flags = ACTIVITY_FRAME_DEFAULT_FLAGS;
        }
        setFlags(flags);
    }

    public int getSupportedFrameFlag() {
        return this.mSupportedFrameFlag;
    }

    public void updateUiMode(int mode) {
        boolean z = true;
        if (mode != this.uiMode && mode != 0) {
            this.uiMode = mode;
            if (this.uiMode == 1) {
                this.mSupportedFrameFlag = ACTIVITY_FRAME_TABLET_SUPPORTED_FLAGS;
                this.mMaxBtn.setVisibility(8);
                setWindowExpandable(DEBUG);
            } else {
                this.mSupportedFrameFlag = ACTIVITY_FRAME_DEFAULT_SUPPORTED_FLAGS;
                this.mMaxBtn.setVisibility(0);
                if (!this.mMaximized) {
                    setWindowExpandable(true);
                }
            }
            if (this.uiMode == 1) {
                z = DEBUG;
            }
            setHasShadowBackground(z);
        }
    }

    protected boolean onResize(Rect frame) {
        if (fitWindowInScreen(frame)) {
            this.mInHalfSize = DEBUG;
            setWindowPos(frame);
        }
        return true;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mHideNaviBarSet && !this.mMaximized) {
            this.mDecor.getViewRootImpl().forceFullScreenAttr(ViewRootImpl.FORCE_UN_FULL_SCREEN);
        }
    }

    public void onWindowSystemUiVisibilityChanged(int visible) {
        if ((visible & 2) != 0) {
            this.mHideNaviBarSet = true;
        } else {
            this.mHideNaviBarSet = DEBUG;
        }
        if (this.mHideNaviBarSet) {
            if (this.mMaximized) {
                this.mWindowFullScreen = true;
                hideWindowFrame(true);
                try {
                    ActivityManagerNative.getDefault().setAppFullScreen(this.mWindow.mAppToken, this.mWindowFullScreen);
                } catch (Exception e) {
                }
            } else if (this.mDecor.getViewRootImpl() != null) {
                this.mDecor.getViewRootImpl().forceFullScreenAttr(ViewRootImpl.FORCE_UN_FULL_SCREEN);
            }
        } else if (this.mWindowFullScreen) {
            this.mWindowFullScreen = DEBUG;
            hideWindowFrame(DEBUG);
            try {
                ActivityManagerNative.getDefault().setAppFullScreen(this.mWindow.mAppToken, this.mWindowFullScreen);
            } catch (Exception e2) {
            }
        }
    }

    protected boolean handleKeyEvent(KeyEvent event) {
        boolean z = true;
        int keyCode = event.getKeyCode();
        boolean down = event.getAction() == 0 ? true : DEBUG;
        int shiftlessModifiers = event.getModifiers() & -194;
        if ((!KeyEvent.metaStateHasModifiers(shiftlessModifiers, 2) && !KeyEvent.metaStateHasModifiers(shiftlessModifiers, 65536)) || keyCode != 111) {
            return DEBUG;
        }
        if (down && this.mDecor.getViewRootImpl().forceFullScreenAttr(0)) {
            if (this.mWindowFullScreen) {
                z = DEBUG;
            }
            this.mWindowFullScreen = z;
            hideWindowFrame(this.mWindowFullScreen);
            try {
                ActivityManagerNative.getDefault().setAppFullScreen(this.mWindow.mAppToken, this.mWindowFullScreen);
            } catch (Exception e) {
            }
        }
        return true;
    }

    public void hideWindowFrame(boolean hide) {
        if (hide != this.mHideFrame) {
            this.mHideFrame = hide;
            if (this.mHideFrame) {
                this.mHeader.setVisibility(8);
                setHasShadowBackground(DEBUG);
                return;
            }
            this.mHeader.setVisibility(0);
            setHasShadowBackground(true);
        }
    }

    public boolean setWindowPos(Rect r) {
        boolean ret = DEBUG;
        try {
            ret = ActivityManagerNative.getDefault().resizeAppWindow(this.mWindow.mAppToken, r);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return ret;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.firstLayout) {
            this.mFrame.set(this.mDecor.getViewRootImpl().mWinFrame);
            this.mFrameHeaderHeight = this.mHeader.getMeasuredHeight();
            updateFrameInsets();
            if (this.mDecor.getViewRootImpl().mWinFrame.equals(new Rect(-this.mBorderPadding.left, -this.mBorderPadding.top, this.mRealDisplayMetrics.widthPixels + this.mBorderPadding.left, (this.mRealDisplayMetrics.heightPixels - this.mDockBarHeight) + this.mBorderPadding.bottom))) {
                setMaximized(true);
            }
            this.firstLayout = DEBUG;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    private void setMaximized(boolean maximize) {
        if (this.mMaximized != maximize) {
            this.mMaximized = maximize;
            if (this.mMaximized) {
                ((ImageView) this.mMaxBtn).setImageResource(17303435);
                setWindowExpandable(DEBUG);
                if (this.mHideNaviBarSet) {
                    this.mDecor.getViewRootImpl().forceFullScreenAttr(0);
                    return;
                }
                return;
            }
            ((ImageView) this.mMaxBtn).setImageResource(17303429);
            if (this.uiMode != 1) {
                setWindowExpandable(true);
            }
        }
    }

    private void maxOrRestoreWindow() {
        boolean z = true;
        try {
            if (!this.mMaximized) {
                if (ActivityManagerNative.getDefault().maximizeOrRestoreAppWindow(this.mWindow.mAppToken, !this.mMaximized ? true : DEBUG)) {
                    if (this.mMaximized) {
                        z = DEBUG;
                    }
                    setMaximized(z);
                }
            } else if (setWindowPos(ActivityManagerNative.getDefault().getRestoredAppWindowSize(this.mContext.getPackageName()))) {
                if (this.mMaximized) {
                    z = DEBUG;
                }
                setMaximized(z);
            }
        } catch (Exception e) {
        }
    }
}
