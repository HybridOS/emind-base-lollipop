package com.android.internal.policy.impl;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.input.InputManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class WindowFrameView extends FrameLayout {
    private static boolean DEBUG = false;
    public static final int FLAG_WINDOW_FRAME_BACK_BTN_DISABLE = 256;
    public static final int FLAG_WINDOW_FRAME_BACK_BTN_MASK = 1;
    public static final int FLAG_WINDOW_FRAME_CLOSE_BTN_DISABLE = 2048;
    public static final int FLAG_WINDOW_FRAME_EXPANDABLE_MASK = 16;
    public static final int FLAG_WINDOW_FRAME_HIDE_FRAME_MASK = Integer.MIN_VALUE;
    public static final int FLAG_WINDOW_FRAME_HIDE_HEADER_MASK = 32;
    public static final int FLAG_WINDOW_FRAME_MAX_BTN_DISABLE = 1024;
    public static final int FLAG_WINDOW_FRAME_MAX_BTN_MASK = 8;
    public static final int FLAG_WINDOW_FRAME_MIN_BTN_DISABLE = 512;
    public static final int FLAG_WINDOW_FRAME_MIN_BTN_MASK = 4;
    public static final int FLAG_WINDOW_FRAME_TITLE_VIEW_MASK = 2;
    public static final int FLAG_WINDOW_FRAME_TOPMOST_WINDOW_MASK = 16777216;
    static final int POINT_INWINDOW_BOTTOM = 4;
    static final int POINT_INWINDOW_HEADER = 9;
    static final int POINT_INWINDOW_LEFT = 1;
    static final int POINT_INWINDOW_LEFTBOTTOM = 7;
    static final int POINT_INWINDOW_LEFTTOP = 5;
    static final int POINT_INWINDOW_MIDDLE = 0;
    static final int POINT_INWINDOW_RIGHT = 2;
    static final int POINT_INWINDOW_RIGHTBOTTOM = 8;
    static final int POINT_INWINDOW_RIGHTTOP = 6;
    static final int POINT_INWINDOW_TOP = 3;
    static final int POINT_OUTSIDE_WINDOW = -1;
    private static String TAG = "WindowFrameView";
    int mAdditionalCornerSize = POINT_INWINDOW_LEFTTOP;
    LinearLayout mAppCustomView;
    View mBackBtn;
    OnTouchListener mBackTouch = new OnTouchListener() {
        int mCode = WindowFrameView.POINT_INWINDOW_BOTTOM;
        private long mDownTime;

        void sendEvent(int action, int flags) {
            InputManager.getInstance().injectInputEvent(new KeyEvent(this.mDownTime, SystemClock.uptimeMillis(), action, this.mCode, (flags & 128) != 0 ? WindowFrameView.POINT_INWINDOW_LEFT : WindowFrameView.POINT_INWINDOW_MIDDLE, WindowFrameView.POINT_INWINDOW_MIDDLE, WindowFrameView.POINT_OUTSIDE_WINDOW, WindowFrameView.POINT_INWINDOW_MIDDLE, (flags | WindowFrameView.POINT_INWINDOW_RIGHTBOTTOM) | 64, 257), WindowFrameView.POINT_INWINDOW_MIDDLE);
        }

        public boolean onTouch(View v, MotionEvent ev) {
            switch (ev.getAction()) {
                case WindowFrameView.POINT_INWINDOW_MIDDLE /*0*/:
                    this.mDownTime = SystemClock.uptimeMillis();
                    WindowFrameView.this.setPressed(true);
                    sendEvent(WindowFrameView.POINT_INWINDOW_MIDDLE, WindowFrameView.POINT_INWINDOW_MIDDLE);
                    break;
                case WindowFrameView.POINT_INWINDOW_LEFT /*1*/:
                    boolean doIt = WindowFrameView.this.isPressed();
                    WindowFrameView.this.setPressed(false);
                    if (!doIt) {
                        sendEvent(WindowFrameView.POINT_INWINDOW_LEFT, WindowFrameView.FLAG_WINDOW_FRAME_HIDE_HEADER_MASK);
                        break;
                    }
                    sendEvent(WindowFrameView.POINT_INWINDOW_LEFT, WindowFrameView.POINT_INWINDOW_MIDDLE);
                    break;
                case WindowFrameView.POINT_INWINDOW_TOP /*3*/:
                    WindowFrameView.this.setPressed(false);
                    sendEvent(WindowFrameView.POINT_INWINDOW_LEFT, WindowFrameView.FLAG_WINDOW_FRAME_HIDE_HEADER_MASK);
                    break;
            }
            return true;
        }
    };
    Rect mBorderPadding;
    View mCloseBtn;
    boolean mClosed = false;
    private Configuration mConfiguration;
    boolean mCurrentOnFocus = false;
    View mDecor;
    FrameLayout mDecorContainer;
    Drawable mDefaultBackground;
    int mDockBarHeight = POINT_INWINDOW_MIDDLE;
    private Rect mDownFrame = new Rect();
    boolean mExpandable = true;
    int mFlags = POINT_INWINDOW_MIDDLE;
    Rect mFrame = new Rect();
    int mFrameHeaderHeight = POINT_INWINDOW_MIDDLE;
    ViewGroup mFrameView;
    LinearLayout mHeader;
    boolean mHideFrame = false;
    boolean mHideNaviBarSet = false;
    private boolean mLastTouchOnHeader = false;
    private int mLastX = POINT_INWINDOW_MIDDLE;
    private int mLastY = POINT_INWINDOW_MIDDLE;
    boolean mLayoutFullScreen = false;
    View mMaxBtn;
    boolean mMaximized = false;
    View mMinBtn;
    private int mPointPos = POINT_OUTSIDE_WINDOW;
    DisplayMetrics mRealDisplayMetrics;
    private int mResizeMinSize = POINT_INWINDOW_MIDDLE;
    private Rect mResizing = new Rect();
    int mResizingBorder = 11;
    View mSep1;
    View mSep2;
    View mSepLine;
    public int mStackBoxId = POINT_OUTSIDE_WINDOW;
    int mStatusBarHeight = POINT_INWINDOW_MIDDLE;
    int mStretchBorderSize = POINT_INWINDOW_LEFTTOP;
    View mStretchBtn;
    OnTouchListener mStretchTouch = new OnTouchListener() {
        int mDownX;
        int mDownY;
        Rect mResizing = new Rect();
        Rect mTmpRect = new Rect();

        public boolean onTouch(View v, MotionEvent event) {
            if (!WindowFrameView.this.mExpandable || WindowFrameView.this.mMaximized) {
                return false;
            }
            int rawX = (int) event.getRawX();
            int rawY = (int) event.getRawY();
            int what = event.getAction();
            if (WindowFrameView.DEBUG) {
                Log.i(WindowFrameView.TAG, "mStretchTouch action:" + what + " X:" + rawX + " Y:" + rawY);
                Log.i(WindowFrameView.TAG, "mStretchTouch windowFrame:" + WindowFrameView.this.mFrame.toString());
            }
            Rect r;
            switch (what) {
                case WindowFrameView.POINT_INWINDOW_MIDDLE /*0*/:
                    WindowFrameView.this.mDownFrame.set(WindowFrameView.this.mDecor.getViewRootImpl().mWinFrame);
                    this.mDownX = rawX;
                    this.mDownY = rawY;
                    this.mResizing.setEmpty();
                    try {
                        r = new Rect(WindowFrameView.this.mDownFrame);
                        r.left = (r.left + WindowFrameView.this.mBorderPadding.left) - WindowFrameView.this.mResizingBorder;
                        r.top = (r.top + WindowFrameView.this.mBorderPadding.top) - WindowFrameView.this.mResizingBorder;
                        r.right = (r.right - WindowFrameView.this.mBorderPadding.right) + WindowFrameView.this.mResizingBorder;
                        r.bottom = (r.bottom - WindowFrameView.this.mBorderPadding.bottom) + WindowFrameView.this.mResizingBorder;
                        ActivityManagerNative.getDefault().showResizingFrame(r);
                        break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        break;
                    }
                case WindowFrameView.POINT_INWINDOW_LEFT /*1*/:
                    if (this.mResizing.isEmpty()) {
                        Toast toast = Toast.makeText(WindowFrameView.this.mContext, 17041060, WindowFrameView.POINT_INWINDOW_MIDDLE);
                        toast.setGravity(49, WindowFrameView.POINT_INWINDOW_MIDDLE, 200);
                        toast.show();
                    } else {
                        WindowFrameView.this.onResize(this.mResizing);
                    }
                    try {
                        ActivityManagerNative.getDefault().hideResizingFrame();
                        break;
                    } catch (RemoteException e2) {
                        e2.printStackTrace();
                        break;
                    }
                case WindowFrameView.POINT_INWINDOW_RIGHT /*2*/:
                    if (this.mDownX != rawX || this.mDownY != rawY) {
                        this.mTmpRect.set(WindowFrameView.this.mDownFrame.left, (WindowFrameView.this.mDownFrame.top + rawY) - this.mDownY, (WindowFrameView.this.mDownFrame.right + rawX) - this.mDownX, WindowFrameView.this.mDownFrame.bottom);
                        if (WindowFrameView.this.fitWindowInScreen(this.mTmpRect)) {
                            this.mResizing.set(this.mTmpRect);
                            try {
                                r = new Rect(this.mResizing);
                                r.left = (r.left + WindowFrameView.this.mBorderPadding.left) - WindowFrameView.this.mResizingBorder;
                                r.top = (r.top + WindowFrameView.this.mBorderPadding.top) - WindowFrameView.this.mResizingBorder;
                                r.right = (r.right - WindowFrameView.this.mBorderPadding.right) + WindowFrameView.this.mResizingBorder;
                                r.bottom = (r.bottom - WindowFrameView.this.mBorderPadding.bottom) + WindowFrameView.this.mResizingBorder;
                                ActivityManagerNative.getDefault().showResizingFrame(r);
                                break;
                            } catch (RemoteException e22) {
                                e22.printStackTrace();
                                break;
                            }
                        }
                    }
                    return true;
                    break;
            }
            return true;
        }
    };
    TextView mTitle;
    Window mWindow;
    boolean mWindowFullScreen = false;
    WindowManager mWindowManager;
    int uiMode = POINT_INWINDOW_MIDDLE;

    public WindowFrameView(Context context, View decorView, int resId, int flags) {
        super(context);
        this.mDecor = decorView;
        LayoutInflater.from(context).inflate(resId, this);
        this.mFrameHeaderHeight = getContext().getResources().getDimensionPixelSize(17105158);
        this.mDockBarHeight = getContext().getResources().getDimensionPixelSize(17104915);
        this.mStatusBarHeight = POINT_INWINDOW_MIDDLE;
        this.mHeader = (LinearLayout) findViewById(16909031);
        this.mCloseBtn = findViewById(16909038);
        this.mBackBtn = findViewById(16909032);
        this.mMinBtn = findViewById(16909035);
        this.mMaxBtn = findViewById(16909036);
        this.mStretchBtn = findViewById(16909075);
        this.mStretchBtn.setFocusable(true);
        this.mStretchBtn.setOnTouchListener(this.mStretchTouch);
        this.mSepLine = findViewById(16909076);
        this.mSep1 = findViewById(16909033);
        this.mSep2 = findViewById(16909037);
        this.mBackBtn.setOnTouchListener(this.mBackTouch);
        this.mAppCustomView = (LinearLayout) findViewById(16909034);
        this.mRealDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(this.mRealDisplayMetrics);
        this.mConfiguration = new Configuration(context.getResources().getConfiguration());
        this.mFrameView = (ViewGroup) findViewById(16909072);
        this.mDefaultBackground = this.mFrameView.getBackground();
        this.mBorderPadding = new Rect(this.mFrameView.getPaddingLeft(), this.mFrameView.getPaddingTop(), this.mFrameView.getPaddingRight(), this.mFrameView.getPaddingBottom());
        this.mDecorContainer = (FrameLayout) findViewById(16909073);
        this.mStretchBorderSize = (int) (this.mRealDisplayMetrics.density * ((float) this.mStretchBorderSize));
        this.mAdditionalCornerSize = (int) ((this.mRealDisplayMetrics.density * ((float) this.mAdditionalCornerSize)) + 0.5f);
        this.mResizingBorder = (int) context.getResources().getDimension(17105164);
        this.mResizeMinSize = (int) context.getResources().getDimension(17105166);
        PackageManager pm = getContext().getPackageManager();
        ApplicationInfo ai = getContext().getApplicationInfo();
        this.mTitle = (TextView) findViewById(16909074);
        this.mTitle.setText(pm.getApplicationLabel(ai));
    }

    public Rect getFrameInsets() {
        Rect rect = new Rect(this.mBorderPadding);
        if (this.mHeader.getVisibility() == 0 && this.mHeader.getHeight() == 0) {
            rect.top += this.mFrameHeaderHeight;
        }
        return rect;
    }

    void updateFrameInsets() {
        Rect rect = getFrameInsets();
        setFrameInsets(rect);
        if (this.mWindow != null) {
            WindowManager.LayoutParams attr = this.mWindow.getAttributes();
            attr.frameInsets.set(rect);
            attr.headHeight = this.mFrameHeaderHeight;
            this.mWindow.setAttributes(attr);
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    public void setTitle(CharSequence title) {
        this.mTitle.setText(title);
    }

    public FrameLayout getDecorContainer() {
        return this.mDecorContainer;
    }

    public void addCustomFrameTitleView(View titleView) {
        this.mAppCustomView.addView(titleView, POINT_INWINDOW_MIDDLE);
        this.mSepLine.setVisibility(POINT_INWINDOW_RIGHTBOTTOM);
    }

    public void setFrameHeaderBgDrawable(Drawable background) {
        this.mHeader.setBackground(background);
    }

    public int getFlags() {
        return this.mFlags;
    }

    public int getSupportedFrameFlag() {
        return POINT_INWINDOW_MIDDLE;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mFrame.set(this.mDecor.getViewRootImpl().mWinFrame);
    }

    public void dispatchWindowMoved(int newX, int newY) {
        this.mFrame.set(this.mDecor.getViewRootImpl().mWinFrame);
    }

    public void setFlags(int flags) {
        this.mFlags = getSupportedFrameFlag() & flags;
        if ((this.mFlags & POINT_INWINDOW_LEFT) != 0) {
            this.mBackBtn.setVisibility(POINT_INWINDOW_MIDDLE);
            this.mSep1.setVisibility(POINT_INWINDOW_MIDDLE);
        } else {
            this.mBackBtn.setVisibility(POINT_INWINDOW_RIGHTBOTTOM);
            this.mSep1.setVisibility(POINT_INWINDOW_RIGHTBOTTOM);
        }
        if ((this.mFlags & FLAG_WINDOW_FRAME_EXPANDABLE_MASK) != 0) {
            this.mStretchBtn.setVisibility(POINT_INWINDOW_MIDDLE);
            this.mExpandable = true;
        } else {
            this.mStretchBtn.setVisibility(POINT_INWINDOW_RIGHTBOTTOM);
            this.mExpandable = false;
        }
        if ((this.mFlags & POINT_INWINDOW_BOTTOM) != 0) {
            this.mMinBtn.setVisibility(POINT_INWINDOW_MIDDLE);
        } else {
            this.mMinBtn.setVisibility(POINT_INWINDOW_RIGHTBOTTOM);
        }
        if ((this.mFlags & POINT_INWINDOW_RIGHTBOTTOM) != 0) {
            this.mMaxBtn.setVisibility(POINT_INWINDOW_MIDDLE);
        } else {
            this.mMaxBtn.setVisibility(POINT_INWINDOW_RIGHTBOTTOM);
        }
        if ((this.mFlags & FLAG_WINDOW_FRAME_HIDE_HEADER_MASK) != 0) {
            this.mHeader.setVisibility(POINT_INWINDOW_RIGHTBOTTOM);
        } else {
            this.mHeader.setVisibility(POINT_INWINDOW_MIDDLE);
        }
        if ((this.mFlags & FLAG_WINDOW_FRAME_BACK_BTN_DISABLE) == 0) {
            this.mBackBtn.setEnabled(true);
        } else {
            this.mBackBtn.setEnabled(false);
        }
        if ((this.mFlags & FLAG_WINDOW_FRAME_MIN_BTN_DISABLE) == 0) {
            this.mMinBtn.setEnabled(true);
        } else {
            this.mMinBtn.setEnabled(false);
        }
        if ((this.mFlags & FLAG_WINDOW_FRAME_MAX_BTN_DISABLE) == 0) {
            this.mMaxBtn.setEnabled(true);
        } else {
            this.mMaxBtn.setEnabled(false);
        }
        if ((this.mFlags & FLAG_WINDOW_FRAME_CLOSE_BTN_DISABLE) == 0) {
            this.mCloseBtn.setEnabled(true);
        } else {
            this.mCloseBtn.setEnabled(false);
        }
        if ((this.mFlags & POINT_INWINDOW_RIGHT) != 0) {
            this.mTitle.setVisibility(POINT_INWINDOW_MIDDLE);
        } else {
            this.mTitle.setVisibility(POINT_INWINDOW_RIGHTBOTTOM);
        }
        if ((this.mFlags & FLAG_WINDOW_FRAME_EXPANDABLE_MASK) == 0) {
            setWindowExpandable(false);
        } else if (!(this.uiMode == POINT_INWINDOW_LEFT || this.mMaximized)) {
            setWindowExpandable(true);
        }
        updateFrameInsets();
    }

    void setWindowExpandable(boolean expandable) {
        if ((this.mFlags & FLAG_WINDOW_FRAME_EXPANDABLE_MASK) != 0 && this.mExpandable != expandable) {
            this.mExpandable = expandable;
            this.mStretchBtn.setVisibility(this.mExpandable ? POINT_INWINDOW_MIDDLE : POINT_INWINDOW_RIGHTBOTTOM);
        }
    }

    int getStackBoxId() {
        return this.mStackBoxId;
    }

    public WindowManager getWindowManager() {
        if (this.mWindowManager == null) {
            this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        }
        return this.mWindowManager;
    }

    private void updatePointPos(int pos) {
        if (this.mClosed) {
            pos = POINT_OUTSIDE_WINDOW;
        }
        if (pos != this.mPointPos) {
            this.mPointPos = pos;
            if (this.mPointPos == POINT_INWINDOW_LEFT || this.mPointPos == POINT_INWINDOW_RIGHT) {
                InputManager.getInstance().updatePointerIcon(this.mContext, POINT_INWINDOW_LEFT);
            } else if (this.mPointPos == POINT_INWINDOW_TOP || this.mPointPos == POINT_INWINDOW_BOTTOM) {
                InputManager.getInstance().updatePointerIcon(this.mContext, POINT_INWINDOW_RIGHT);
            } else if (this.mPointPos == POINT_INWINDOW_LEFTTOP || this.mPointPos == POINT_INWINDOW_RIGHTBOTTOM) {
                InputManager.getInstance().updatePointerIcon(this.mContext, POINT_INWINDOW_TOP);
            } else if (this.mPointPos == POINT_INWINDOW_RIGHTTOP || this.mPointPos == POINT_INWINDOW_LEFTBOTTOM) {
                InputManager.getInstance().updatePointerIcon(this.mContext, POINT_INWINDOW_BOTTOM);
            } else {
                InputManager.getInstance().updatePointerIcon(this.mContext, POINT_INWINDOW_MIDDLE);
            }
        }
    }

    public void updateUiMode(int mode) {
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        try {
            ActivityManagerNative.getDefault().hideResizingFrame();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        int diff = this.mConfiguration.diff(newConfig);
        if ((diff & 128) != 0) {
            getWindowManager().getDefaultDisplay().getRealMetrics(this.mRealDisplayMetrics);
        } else if ((diff & 16384) != 0) {
            updateUiMode(newConfig.system_mode);
        }
        this.mConfiguration.setTo(newConfig);
    }

    private int pointInWindowEdagePos(float x, float y) {
        int pos = POINT_OUTSIDE_WINDOW;
        if (x >= ((float) ((this.mFrame.left + this.mBorderPadding.left) + this.mStretchBorderSize)) || x <= ((float) ((this.mFrame.left + this.mBorderPadding.left) - this.mStretchBorderSize))) {
            if (x <= ((float) ((this.mFrame.right - this.mBorderPadding.right) - this.mStretchBorderSize)) || x >= ((float) ((this.mFrame.right - this.mBorderPadding.right) + this.mStretchBorderSize))) {
                if (x <= ((float) (this.mFrame.left + this.mBorderPadding.left)) || x >= ((float) (this.mFrame.right - this.mBorderPadding.right))) {
                    pos = POINT_INWINDOW_MIDDLE;
                } else {
                    pos = (y >= ((float) ((this.mFrame.top + this.mBorderPadding.top) + this.mStretchBorderSize)) + (this.mCurrentOnFocus ? 0.0f : (float) (-this.mStretchBorderSize)) || y <= ((float) ((this.mFrame.top + this.mBorderPadding.top) - this.mStretchBorderSize))) ? (y <= ((float) ((this.mFrame.bottom - this.mBorderPadding.bottom) - this.mStretchBorderSize)) || y >= ((float) ((this.mFrame.bottom - this.mBorderPadding.bottom) + this.mStretchBorderSize))) ? (y >= ((float) ((this.mFrame.top + this.mBorderPadding.top) + this.mFrameHeaderHeight)) || y <= ((float) (this.mFrame.top + this.mBorderPadding.top))) ? POINT_INWINDOW_MIDDLE : POINT_INWINDOW_HEADER : POINT_INWINDOW_BOTTOM : POINT_INWINDOW_TOP;
                }
            } else if (y < ((float) ((this.mFrame.top + this.mBorderPadding.top) + this.mStretchBorderSize)) && y > ((float) ((this.mFrame.top + this.mBorderPadding.top) - this.mStretchBorderSize))) {
                pos = POINT_INWINDOW_RIGHTTOP;
            } else if (y > ((float) ((this.mFrame.bottom - this.mBorderPadding.bottom) - this.mStretchBorderSize)) && y < ((float) ((this.mFrame.bottom - this.mBorderPadding.bottom) + this.mStretchBorderSize))) {
                pos = POINT_INWINDOW_RIGHTBOTTOM;
            } else if (y < ((float) (this.mFrame.bottom - this.mBorderPadding.bottom)) && y > ((float) (this.mFrame.top + this.mBorderPadding.top))) {
                pos = POINT_INWINDOW_RIGHT;
            }
        } else if (y < ((float) ((this.mFrame.top + this.mBorderPadding.top) + this.mStretchBorderSize)) && y > ((float) ((this.mFrame.top + this.mBorderPadding.top) - this.mStretchBorderSize))) {
            pos = POINT_INWINDOW_LEFTTOP;
        } else if (y > ((float) ((this.mFrame.bottom - this.mBorderPadding.bottom) - this.mStretchBorderSize)) && y < ((float) ((this.mFrame.bottom - this.mBorderPadding.bottom) + this.mStretchBorderSize))) {
            pos = POINT_INWINDOW_LEFTBOTTOM;
        } else if (y < ((float) (this.mFrame.bottom - this.mBorderPadding.bottom)) && y > ((float) (this.mFrame.top + this.mBorderPadding.top))) {
            pos = POINT_INWINDOW_LEFT;
        }
        if (pos == POINT_INWINDOW_LEFTTOP || pos == POINT_INWINDOW_RIGHTTOP || pos == POINT_INWINDOW_LEFTBOTTOM || pos == POINT_INWINDOW_RIGHTBOTTOM) {
            return pos;
        }
        if (x >= ((float) (((this.mFrame.left + this.mBorderPadding.left) + this.mStretchBorderSize) + this.mAdditionalCornerSize)) || x <= ((float) ((this.mFrame.left + this.mBorderPadding.left) - this.mStretchBorderSize))) {
            if (x <= ((float) (((this.mFrame.right - this.mBorderPadding.right) - this.mStretchBorderSize) - this.mAdditionalCornerSize)) || x >= ((float) ((this.mFrame.right - this.mBorderPadding.right) + this.mStretchBorderSize))) {
                return pos;
            }
            if (y < ((float) (((this.mFrame.top + this.mBorderPadding.top) + this.mStretchBorderSize) + this.mAdditionalCornerSize)) && y > ((float) ((this.mFrame.top + this.mBorderPadding.top) - this.mStretchBorderSize))) {
                return POINT_INWINDOW_RIGHTTOP;
            }
            if (y <= ((float) (((this.mFrame.bottom - this.mBorderPadding.bottom) - this.mStretchBorderSize) - this.mAdditionalCornerSize)) || y >= ((float) ((this.mFrame.bottom - this.mBorderPadding.bottom) + this.mStretchBorderSize))) {
                return pos;
            }
            return POINT_INWINDOW_RIGHTBOTTOM;
        } else if (y < ((float) (((this.mFrame.top + this.mBorderPadding.top) + this.mStretchBorderSize) + this.mAdditionalCornerSize)) && y > ((float) ((this.mFrame.top + this.mBorderPadding.top) - this.mStretchBorderSize))) {
            return POINT_INWINDOW_LEFTTOP;
        } else {
            if (y <= ((float) (((this.mFrame.bottom - this.mBorderPadding.bottom) - this.mStretchBorderSize) - this.mAdditionalCornerSize)) || y >= ((float) ((this.mFrame.bottom - this.mBorderPadding.bottom) + this.mStretchBorderSize))) {
                return pos;
            }
            return POINT_INWINDOW_LEFTBOTTOM;
        }
    }

    Rect stretchResize(Rect frame, int dirction, int diffX, int diffY) {
        Rect mTmpFrame = new Rect(frame);
        if (dirction == POINT_INWINDOW_LEFTBOTTOM) {
            mTmpFrame.left += diffX;
            mTmpFrame.bottom += diffY;
        } else if (dirction == POINT_INWINDOW_RIGHTBOTTOM) {
            mTmpFrame.right += diffX;
            mTmpFrame.bottom += diffY;
        } else if (dirction == POINT_INWINDOW_BOTTOM) {
            mTmpFrame.bottom += diffY;
        } else if (dirction == POINT_INWINDOW_LEFT) {
            mTmpFrame.left += diffX;
        } else if (dirction == POINT_INWINDOW_RIGHT) {
            mTmpFrame.right += diffX;
        } else if (dirction == POINT_INWINDOW_TOP) {
            mTmpFrame.top += diffY;
        } else if (dirction == POINT_INWINDOW_LEFTTOP) {
            mTmpFrame.top += diffY;
            mTmpFrame.left += diffX;
        } else if (dirction == POINT_INWINDOW_RIGHTTOP) {
            mTmpFrame.top += diffY;
            mTmpFrame.right += diffX;
        }
        return mTmpFrame;
    }

    public boolean dispatchEarlyFrameEvent(InputEvent ev) {
        return false;
    }

    protected boolean handleKeyEvent(KeyEvent event) {
        return false;
    }

    protected boolean onResize(Rect frame) {
        return false;
    }

    public boolean setWindowPos(Rect r) {
        return false;
    }

    public boolean isMaximized() {
        return this.mMaximized;
    }

    boolean fitWindowInScreen(Rect pos) {
        float move_edage = 50.0f * this.mRealDisplayMetrics.density;
        if (pos.width() < (this.mResizeMinSize + this.mBorderPadding.left) + this.mBorderPadding.right || pos.height() < (this.mResizeMinSize + this.mBorderPadding.top) + this.mBorderPadding.bottom || ((double) pos.width()) > ((double) this.mRealDisplayMetrics.widthPixels) * 1.1d || ((double) pos.height()) > ((double) this.mRealDisplayMetrics.heightPixels) * 1.1d || pos.top < this.mStatusBarHeight - this.mBorderPadding.top || ((float) (pos.top + this.mBorderPadding.top)) + move_edage > ((float) (this.mRealDisplayMetrics.heightPixels - this.mDockBarHeight)) || ((float) pos.right) < move_edage || ((float) pos.left) > ((float) this.mRealDisplayMetrics.widthPixels) - move_edage) {
            return false;
        }
        return true;
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean ret = false;
        if (!this.mExpandable || this.mMaximized || this.mHideFrame) {
            return POINT_INWINDOW_MIDDLE;
        }
        int pos = POINT_OUTSIDE_WINDOW;
        int rawX = (int) ev.getRawX();
        int rawY = (int) ev.getRawY();
        int what = ev.getAction();
        if (DEBUG) {
            Log.i(TAG, "onInterceptTouchEvent action:" + what + " X:" + rawX + " Y:" + rawY);
            Log.i(TAG, "onInterceptTouchEvent windowFrame:" + this.mFrame.toString());
        }
        if (what == 0) {
            this.mLastX = rawX;
            this.mLastY = rawY;
            pos = pointInWindowEdagePos((float) rawX, (float) rawY);
        }
        if (!(pos == 0 || pos == POINT_OUTSIDE_WINDOW || pos == POINT_INWINDOW_HEADER)) {
            ret = true;
        }
        return ret;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = false;
        if (!this.mExpandable || this.mMaximized || this.mHideFrame) {
            return POINT_INWINDOW_MIDDLE;
        }
        int rawX = (int) event.getRawX();
        int rawY = (int) event.getRawY();
        int what = event.getAction();
        if (DEBUG) {
            Log.i(TAG, "onTouchEvent action:" + what + " X:" + rawX + " Y:" + rawY);
            Log.i(TAG, "onTouchEvent windowFrame:" + this.mFrame.toString());
        }
        switch (what) {
            case POINT_INWINDOW_MIDDLE /*0*/:
                this.mDownFrame.set(this.mDecor.getViewRootImpl().mWinFrame);
                this.mLastX = rawX;
                this.mLastY = rawY;
                updatePointPos(pointInWindowEdagePos((float) rawX, (float) rawY));
                this.mResizing.setEmpty();
                break;
            case POINT_INWINDOW_LEFT /*1*/:
                updatePointPos(pointInWindowEdagePos((float) rawX, (float) rawY));
                if (!this.mResizing.isEmpty()) {
                    onResize(this.mResizing);
                    try {
                        ActivityManagerNative.getDefault().hideResizingFrame();
                        break;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                break;
            case POINT_INWINDOW_RIGHT /*2*/:
                if (!(this.mPointPos == 0 || this.mPointPos == POINT_OUTSIDE_WINDOW || this.mPointPos == POINT_INWINDOW_HEADER)) {
                    Rect rect = stretchResize(this.mDownFrame, this.mPointPos, rawX - this.mLastX, rawY - this.mLastY);
                    if (fitWindowInScreen(rect)) {
                        this.mResizing.set(rect);
                        try {
                            Rect r = new Rect(this.mResizing);
                            r.left = (r.left + this.mBorderPadding.left) - this.mResizingBorder;
                            r.top = (r.top + this.mBorderPadding.top) - this.mResizingBorder;
                            r.right = (r.right - this.mBorderPadding.right) + this.mResizingBorder;
                            r.bottom = (r.bottom - this.mBorderPadding.bottom) + this.mResizingBorder;
                            ActivityManagerNative.getDefault().showResizingFrame(r);
                            break;
                        } catch (RemoteException e2) {
                            e2.printStackTrace();
                            break;
                        }
                    }
                }
                break;
        }
        if (!(this.mPointPos == 0 || this.mPointPos == POINT_OUTSIDE_WINDOW || this.mPointPos == POINT_INWINDOW_HEADER)) {
            ret = true;
        }
        return ret;
    }

    public boolean onInterceptHoverEvent(MotionEvent event) {
        if (this.mExpandable && !this.mMaximized) {
            float rawX = event.getRawX();
            float rawY = event.getRawY();
            int what = event.getAction();
            if (DEBUG) {
                Log.i(TAG, "onInterceptHoverEvent action:" + what + " X:" + rawX + " Y:" + rawY);
                Log.i(TAG, "onInterceptHoverEvent windowFrame:" + this.mFrame.toString());
            }
            switch (what) {
                case POINT_INWINDOW_LEFTBOTTOM /*7*/:
                    updatePointPos(pointInWindowEdagePos(rawX, rawY));
                    break;
                case POINT_INWINDOW_HEADER /*9*/:
                    break;
                case 10:
                    updatePointPos(pointInWindowEdagePos(rawX, rawY));
                    break;
                default:
                    break;
            }
        }
        return false;
    }

    public void hideWindowFrame(boolean hide) {
    }

    void setHasShadowBackground(boolean shadow) {
        if (shadow) {
            this.mFrameView.setBackground(this.mDefaultBackground);
            this.mDefaultBackground.getPadding(this.mBorderPadding);
        } else {
            this.mFrameView.setBackground(null);
            this.mFrameView.setPadding(POINT_INWINDOW_MIDDLE, POINT_INWINDOW_MIDDLE, POINT_INWINDOW_MIDDLE, POINT_INWINDOW_MIDDLE);
            this.mBorderPadding.set(POINT_INWINDOW_MIDDLE, POINT_INWINDOW_MIDDLE, POINT_INWINDOW_MIDDLE, POINT_INWINDOW_MIDDLE);
        }
        updateFrameInsets();
    }
}

