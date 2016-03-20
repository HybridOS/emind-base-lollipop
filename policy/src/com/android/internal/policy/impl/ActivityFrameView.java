package com.android.internal.policy.impl;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.DisplayMetrics;
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
import android.view.Window;
import android.view.Window.Callback;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class ActivityFrameView
  extends WindowFrameView
{
  private static int ACTIVITY_FRAME_DEFAULT_FLAGS = 31;
  private static int ACTIVITY_FRAME_DEFAULT_SUPPORTED_FLAGS = -2130702529;
  private static int ACTIVITY_FRAME_TABLET_SUPPORTED_FLAGS = -2130702537;
  private static final boolean DEBUG = false;
  private static final String TAG = "ActivityFrameView";
  private boolean firstLayout = true;
  View.OnClickListener mClick = new View.OnClickListener()
  {
    public void onClick(View paramAnonymousView)
    {
      if (paramAnonymousView == ActivityFrameView.this.mMaxBtn) {
        ActivityFrameView.this.maxOrRestoreWindow();
      }
      while (paramAnonymousView != ActivityFrameView.this.mMinBtn) {
        return;
      }
      try
      {
        ActivityManagerNative.getDefault().moveActivityTaskToBack(ActivityFrameView.this.mWindow.mAppToken, true);
        return;
      }
      catch (RemoteException paramAnonymousView) {}
    }
  };
  GestureDetector.SimpleOnGestureListener mCloseListener = new GestureDetector.SimpleOnGestureListener()
  {
    public boolean onMouseRightSingleTapUp(MotionEvent paramAnonymousMotionEvent)
    {
      paramAnonymousMotionEvent = ActivityFrameView.this.mWindow.getCallback();
      if (paramAnonymousMotionEvent != null) {
        try
        {
          boolean bool = paramAnonymousMotionEvent.dispatchWindowCloseClicked(true);
          if (bool) {
            return true;
          }
        }
        catch (Throwable paramAnonymousMotionEvent) {}
      }
      try
      {
        ActivityFrameView.this.mClosed = true;
        ActivityManagerNative.getDefault().closeActivityTask(ActivityFrameView.this.mWindow.mAppToken);
        return true;
      }
      catch (Exception paramAnonymousMotionEvent) {}
      return true;
    }
    
    public boolean onSingleTapUp(MotionEvent paramAnonymousMotionEvent)
    {
      paramAnonymousMotionEvent = ActivityFrameView.this.mWindow.getCallback();
      if (paramAnonymousMotionEvent != null) {
        try
        {
          boolean bool = paramAnonymousMotionEvent.dispatchWindowCloseClicked(false);
          if (bool) {
            return true;
          }
        }
        catch (Throwable paramAnonymousMotionEvent) {}
      }
      try
      {
        ActivityFrameView.this.mClosed = true;
        ActivityManagerNative.getDefault().closeActivityTask(ActivityFrameView.this.mWindow.mAppToken);
        return true;
      }
      catch (Exception paramAnonymousMotionEvent) {}
      return true;
    }
  };
  SurfaceSession mFxSession;
  GestureDetector mGestureDetector = new GestureDetector(getContext(), this.mCloseListener);
  View.OnTouchListener mHeaderTouch = new View.OnTouchListener()
  {
    private static final long sDoubleClickInterval = 500L;
    private Rect mDownFrame = new Rect();
    private int mDownX = 0;
    private int mDownY = 0;
    private Rect mHalfResize = new Rect();
    private Rect mHalfRestoreFrame = new Rect();
    private long mLastTimeClick = 0L;
    private int mLastX = 0;
    private int mLastY = 0;
    private boolean mTouchOnHeader = false;
    private Rect r = new Rect();
    
    private boolean inResizingArea(int paramAnonymousInt1, int paramAnonymousInt2)
    {
      paramAnonymousInt2 = ActivityFrameView.this.mStretchBorderSize * 2;
      if (paramAnonymousInt1 < paramAnonymousInt2)
      {
        this.mHalfResize.set(0, 0, ActivityFrameView.this.mRealDisplayMetrics.widthPixels / 2, ActivityFrameView.this.mRealDisplayMetrics.heightPixels - ActivityFrameView.this.mDockBarHeight);
        return true;
      }
      if (paramAnonymousInt1 > ActivityFrameView.this.mRealDisplayMetrics.widthPixels - paramAnonymousInt2)
      {
        this.mHalfResize.set(ActivityFrameView.this.mRealDisplayMetrics.widthPixels / 2, 0, ActivityFrameView.this.mRealDisplayMetrics.widthPixels, ActivityFrameView.this.mRealDisplayMetrics.heightPixels - ActivityFrameView.this.mDockBarHeight);
        return true;
      }
      return false;
    }
    
    public boolean onTouch(View paramAnonymousView, MotionEvent paramAnonymousMotionEvent)
    {
      int i = (int)paramAnonymousMotionEvent.getRawX();
      int j = (int)paramAnonymousMotionEvent.getRawY();
      if (ActivityFrameView.this.uiMode == 1) {
        return true;
      }
      long l;
      if (paramAnonymousMotionEvent.getAction() == 0)
      {
        if (paramAnonymousMotionEvent.getToolType(0) == 3) {}
        for (float f = 0.0F; (paramAnonymousMotionEvent.getY() > ActivityFrameView.this.mFrameHeaderHeight) || (j < f); f = ActivityFrameView.this.mFrameHeaderHeight / 3)
        {
          Log.e("ActivityFrameView", "mHeader OnTouch Down outOf Frame:  X:" + i + " Y" + j + " X:" + paramAnonymousMotionEvent.getX() + "  WindowFrame:" + ActivityFrameView.this.mFrame.toString());
          return false;
        }
        this.mDownFrame.set(ActivityFrameView.this.mDecor.getViewRootImpl().mWinFrame);
        this.mTouchOnHeader = true;
        this.mDownX = ((int)paramAnonymousMotionEvent.getRawX());
        this.mDownY = ((int)paramAnonymousMotionEvent.getRawY());
        this.mLastX = this.mDownX;
        this.mLastY = this.mDownY;
        l = System.currentTimeMillis();
        if (l - this.mLastTimeClick < 500L)
        {
          ActivityFrameView.this.maxOrRestoreWindow();
          this.mLastTimeClick = 0L;
        }
      }
      for (;;)
      {
        return true;
        this.mLastTimeClick = l;
        continue;
        if (2 == paramAnonymousMotionEvent.getAction())
        {
          if (!this.mTouchOnHeader) {
            return false;
          }
          if ((this.mDownX == i) && (this.mDownY == j)) {
            return true;
          }
          try
          {
            if (!ActivityFrameView.this.mMaximized)
            {
              int k = this.mDownX;
              int m = this.mDownY;
              this.r.set(this.mDownFrame);
              this.r.offset(i - k, j - m);
              if (inResizingArea(i, j)) {
                ActivityManagerNative.getDefault().showResizingFrame(this.mHalfResize);
              }
              for (;;)
              {
                this.mLastTimeClick = 0L;
                break;
                if (!this.mHalfResize.isEmpty())
                {
                  this.mHalfResize.setEmpty();
                  ActivityManagerNative.getDefault().hideResizingFrame();
                }
                if (!ActivityFrameView.this.mInHalfSize) {
                  break label666;
                }
                this.r.set(this.mHalfRestoreFrame);
                this.r.left = (i - this.r.width() / 2);
                if (this.r.left < -ActivityFrameView.this.mBorderPadding.left) {
                  this.r.left = (-ActivityFrameView.this.mBorderPadding.left);
                }
                this.r.right = (this.r.left + this.mHalfRestoreFrame.width());
                if (this.r.right > ActivityFrameView.this.mRealDisplayMetrics.widthPixels + ActivityFrameView.this.mBorderPadding.right)
                {
                  this.r.right = (ActivityFrameView.this.mRealDisplayMetrics.widthPixels + ActivityFrameView.this.mBorderPadding.right);
                  this.r.left = (this.r.right - this.mHalfRestoreFrame.width());
                }
                this.r.top = 0;
                this.r.bottom = this.mHalfRestoreFrame.height();
                ActivityFrameView.this.setWindowPos(this.r);
                this.mDownFrame.set(this.r);
                this.mDownX = i;
                this.mDownY = j;
                this.mLastX = i;
                this.mLastY = j;
                ActivityFrameView.access$102(ActivityFrameView.this, false);
              }
            }
          }
          catch (RemoteException paramAnonymousView)
          {
            for (;;)
            {
              paramAnonymousView.printStackTrace();
              continue;
              label666:
              if (ActivityFrameView.this.fitWindowInScreen(this.r))
              {
                ActivityManagerNative.getDefault().moveAppWindow(ActivityFrameView.this.mWindow.mAppToken, i - this.mLastX, j - this.mLastY);
                this.mLastX = i;
                this.mLastY = j;
                continue;
                if (this.mLastTimeClick != 0L)
                {
                  paramAnonymousView = ActivityManagerNative.getDefault().getRestoredAppWindowSize(ActivityFrameView.this.mContext.getPackageName());
                  this.r.left = (i - paramAnonymousView.width() / 2);
                  if (this.r.left < -ActivityFrameView.this.mBorderPadding.left) {
                    this.r.left = (-ActivityFrameView.this.mBorderPadding.left);
                  }
                  this.r.right = (this.r.left + paramAnonymousView.width());
                  if (this.r.right > ActivityFrameView.this.mRealDisplayMetrics.widthPixels + ActivityFrameView.this.mBorderPadding.right)
                  {
                    this.r.right = (ActivityFrameView.this.mRealDisplayMetrics.widthPixels + ActivityFrameView.this.mBorderPadding.right);
                    this.r.left = (this.r.right - paramAnonymousView.width());
                  }
                  this.r.top = 0;
                  this.r.bottom = paramAnonymousView.height();
                  if ((ActivityFrameView.this.fitWindowInScreen(this.r)) && (ActivityFrameView.this.setWindowPos(this.r)))
                  {
                    ActivityFrameView.this.setMaximized(false);
                    this.mDownFrame.set(this.r);
                    this.mDownX = i;
                    this.mDownY = j;
                    this.mLastX = i;
                    this.mLastY = j;
                  }
                }
              }
            }
          }
        }
        else if ((3 == paramAnonymousMotionEvent.getAction()) || (1 == paramAnonymousMotionEvent.getAction()))
        {
          this.mTouchOnHeader = false;
          if (!this.mHalfResize.isEmpty())
          {
            paramAnonymousView = new Rect(this.mHalfResize.left - ActivityFrameView.this.mBorderPadding.left, this.mHalfResize.top - ActivityFrameView.this.mBorderPadding.top, this.mHalfResize.right + ActivityFrameView.this.mBorderPadding.right, this.mHalfResize.bottom + ActivityFrameView.this.mBorderPadding.bottom);
            this.mHalfRestoreFrame.set(this.mDownFrame);
            ActivityFrameView.this.setWindowPos(paramAnonymousView);
            ActivityFrameView.access$102(ActivityFrameView.this, true);
            this.mHalfResize.setEmpty();
          }
        }
      }
    }
  };
  private boolean mInHalfSize = false;
  private int mSupportedFrameFlag = ACTIVITY_FRAME_DEFAULT_SUPPORTED_FLAGS;
  
  public ActivityFrameView(Context paramContext, View paramView, int paramInt)
  {
    super(paramContext, paramView, 17367286, paramInt);
    this.mHeader.setOnTouchListener(this.mHeaderTouch);
    if ((getContext().getPackageName().equalsIgnoreCase("com.chaozhuo.browser.x86")) || (getContext().getPackageName().equalsIgnoreCase("com.chaozhuo.browser")))
    {
      float f = getContext().getResources().getDisplayMetrics().density;
      paramView = (LinearLayout.LayoutParams)this.mStretchBtn.getLayoutParams();
      paramView.bottomMargin = ((int)(f + 0.5F));
      this.mStretchBtn.setLayoutParams(paramView);
      paramView = (LinearLayout.LayoutParams)this.mMinBtn.getLayoutParams();
      paramView.bottomMargin = ((int)(f + 0.5F));
      this.mMinBtn.setLayoutParams(paramView);
      paramView = (LinearLayout.LayoutParams)this.mMaxBtn.getLayoutParams();
      paramView.bottomMargin = ((int)(f + 0.5F));
      this.mMaxBtn.setLayoutParams(paramView);
      paramView = (LinearLayout.LayoutParams)this.mCloseBtn.getLayoutParams();
      paramView.bottomMargin = ((int)(f + 0.5F));
      this.mCloseBtn.setLayoutParams(paramView);
    }
    this.mMinBtn.setOnClickListener(this.mClick);
    this.mCloseBtn.setOnTouchListener(new View.OnTouchListener()
    {
      public boolean onTouch(View paramAnonymousView, MotionEvent paramAnonymousMotionEvent)
      {
        ActivityFrameView.this.mGestureDetector.onTouchEvent(paramAnonymousMotionEvent);
        return false;
      }
    });
    this.mMaxBtn.setOnClickListener(this.mClick);
    updateUiMode(paramContext.getResources().getConfiguration().system_mode);
    int i = paramInt;
    if (paramInt == 0) {
      i = ACTIVITY_FRAME_DEFAULT_FLAGS;
    }
    setFlags(i);
  }
  
  private void maxOrRestoreWindow()
  {
    boolean bool1 = true;
    boolean bool2 = true;
    for (;;)
    {
      try
      {
        if (!this.mMaximized)
        {
          IActivityManager localIActivityManager = ActivityManagerNative.getDefault();
          IBinder localIBinder = this.mWindow.mAppToken;
          if (this.mMaximized) {
            break label103;
          }
          bool1 = true;
          if (!localIActivityManager.maximizeOrRestoreAppWindow(localIBinder, bool1)) {
            break label102;
          }
          if (this.mMaximized) {
            break label108;
          }
          bool1 = bool2;
          setMaximized(bool1);
          return;
        }
        if (!setWindowPos(ActivityManagerNative.getDefault().getRestoredAppWindowSize(this.mContext.getPackageName()))) {
          break label102;
        }
        if (!this.mMaximized)
        {
          setMaximized(bool1);
          return;
        }
      }
      catch (Exception localException)
      {
        return;
      }
      bool1 = false;
      continue;
      label102:
      return;
      label103:
      bool1 = false;
      continue;
      label108:
      bool1 = false;
    }
  }
  
  private void setMaximized(boolean paramBoolean)
  {
    if (this.mMaximized != paramBoolean)
    {
      this.mMaximized = paramBoolean;
      if (!this.mMaximized) {
        break label57;
      }
      ((ImageView)this.mMaxBtn).setImageResource(17303435);
      setWindowExpandable(false);
      if (this.mHideNaviBarSet) {
        this.mDecor.getViewRootImpl().forceFullScreenAttr(0);
      }
    }
    label57:
    do
    {
      return;
      ((ImageView)this.mMaxBtn).setImageResource(17303429);
    } while (this.uiMode == 1);
    setWindowExpandable(true);
  }
  
  public int getSupportedFrameFlag()
  {
    return this.mSupportedFrameFlag;
  }
  
  protected boolean handleKeyEvent(KeyEvent paramKeyEvent)
  {
    boolean bool2 = true;
    boolean bool3 = false;
    int j = paramKeyEvent.getKeyCode();
    int i;
    if (paramKeyEvent.getAction() == 0) {
      i = 1;
    }
    for (;;)
    {
      int k = paramKeyEvent.getModifiers() & 0xFF3E;
      boolean bool1;
      if (!KeyEvent.metaStateHasModifiers(k, 2))
      {
        bool1 = bool3;
        if (!KeyEvent.metaStateHasModifiers(k, 65536)) {}
      }
      else
      {
        bool1 = bool3;
        if (j == 111) {
          if ((i != 0) && (this.mDecor.getViewRootImpl().forceFullScreenAttr(0)))
          {
            if (this.mWindowFullScreen) {
              break label137;
            }
            bool1 = bool2;
            this.mWindowFullScreen = bool1;
            hideWindowFrame(this.mWindowFullScreen);
          }
        }
      }
      try
      {
        ActivityManagerNative.getDefault().setAppFullScreen(this.mWindow.mAppToken, this.mWindowFullScreen);
        bool1 = true;
        return bool1;
        i = 0;
        continue;
        label137:
        bool1 = false;
      }
      catch (Exception paramKeyEvent)
      {
        for (;;) {}
      }
    }
  }
  
  public void hideWindowFrame(boolean paramBoolean)
  {
    if (paramBoolean == this.mHideFrame) {
      return;
    }
    this.mHideFrame = paramBoolean;
    if (this.mHideFrame == true)
    {
      this.mHeader.setVisibility(8);
      setHasShadowBackground(false);
      return;
    }
    this.mHeader.setVisibility(0);
    setHasShadowBackground(true);
  }
  
  protected void onAttachedToWindow()
  {
    super.onAttachedToWindow();
    if ((this.mHideNaviBarSet) && (!this.mMaximized)) {
      this.mDecor.getViewRootImpl().forceFullScreenAttr(ViewRootImpl.FORCE_UN_FULL_SCREEN);
    }
  }
  
  protected void onLayout(boolean paramBoolean, int paramInt1, int paramInt2, int paramInt3, int paramInt4)
  {
    if (this.firstLayout)
    {
      this.mFrame.set(this.mDecor.getViewRootImpl().mWinFrame);
      this.mFrameHeaderHeight = this.mHeader.getMeasuredHeight();
      updateFrameInsets();
      Rect localRect = new Rect(-this.mBorderPadding.left, -this.mBorderPadding.top, this.mRealDisplayMetrics.widthPixels + this.mBorderPadding.left, this.mRealDisplayMetrics.heightPixels - this.mDockBarHeight + this.mBorderPadding.bottom);
      if (this.mDecor.getViewRootImpl().mWinFrame.equals(localRect)) {
        setMaximized(true);
      }
      this.firstLayout = false;
    }
    super.onLayout(paramBoolean, paramInt1, paramInt2, paramInt3, paramInt4);
  }
  
  protected boolean onResize(Rect paramRect)
  {
    if (fitWindowInScreen(paramRect))
    {
      this.mInHalfSize = false;
      setWindowPos(paramRect);
    }
    return true;
  }
  
  public void onWindowSystemUiVisibilityChanged(int paramInt)
  {
    if ((paramInt & 0x2) != 0)
    {
      this.mHideNaviBarSet = true;
      if (!this.mHideNaviBarSet) {
        break label88;
      }
      if (!this.mMaximized) {
        break label63;
      }
      this.mWindowFullScreen = true;
      hideWindowFrame(true);
    }
    for (;;)
    {
      label63:
      label88:
      try
      {
        ActivityManagerNative.getDefault().setAppFullScreen(this.mWindow.mAppToken, this.mWindowFullScreen);
        return;
      }
      catch (Exception localException2) {}
      this.mHideNaviBarSet = false;
      break;
      if (this.mDecor.getViewRootImpl() != null)
      {
        this.mDecor.getViewRootImpl().forceFullScreenAttr(ViewRootImpl.FORCE_UN_FULL_SCREEN);
        return;
        if (this.mWindowFullScreen)
        {
          this.mWindowFullScreen = false;
          hideWindowFrame(false);
          try
          {
            ActivityManagerNative.getDefault().setAppFullScreen(this.mWindow.mAppToken, this.mWindowFullScreen);
            return;
          }
          catch (Exception localException1) {}
        }
      }
    }
  }
  
  public boolean setWindowPos(Rect paramRect)
  {
    try
    {
      boolean bool = ActivityManagerNative.getDefault().resizeAppWindow(this.mWindow.mAppToken, paramRect);
      return bool;
    }
    catch (RemoteException paramRect)
    {
      paramRect.printStackTrace();
    }
    return false;
  }
  
  public void updateUiMode(int paramInt)
  {
    boolean bool = true;
    if ((paramInt == this.uiMode) || (paramInt == 0)) {
      return;
    }
    this.uiMode = paramInt;
    if (this.uiMode == 1)
    {
      this.mSupportedFrameFlag = ACTIVITY_FRAME_TABLET_SUPPORTED_FLAGS;
      this.mMaxBtn.setVisibility(8);
      setWindowExpandable(false);
      if (this.uiMode == 1) {
        break label93;
      }
    }
    for (;;)
    {
      setHasShadowBackground(bool);
      return;
      this.mSupportedFrameFlag = ACTIVITY_FRAME_DEFAULT_SUPPORTED_FLAGS;
      this.mMaxBtn.setVisibility(0);
      if (this.mMaximized) {
        break;
      }
      setWindowExpandable(true);
      break;
      label93:
      bool = false;
    }
  }
}


/* Location:              /home/zhongtian/workdir/multi-window/reverse/dex2jar-2.0/android.policy-dex2jar.jar!/com/android/internal/policy/impl/ActivityFrameView.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */