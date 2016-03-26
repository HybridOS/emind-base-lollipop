package com.android.server.am;

import android.app.ActivityManager.StackInfo;
import android.app.IApplicationThread;
import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.AtomicFile;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.view.DisplayInfo;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.wm.WindowManagerService;
/*
import com.chaozhuo.onlineconfig.CZOnlineConfigInfo;
import com.chaozhuo.onlineconfig.CZOnlineConfigManager;
import com.chaozhuo.onlineconfig.CZOnlineConfigManager.ConfigUpdateListener;
*/
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class MultiWindowManager {
    private static final boolean DEBUG = true;
    private static final String DEFAULT_FILE_PATH = "/system/etc/compat_app.conf";
    private static final int MAXMIZE_MODE = 1;
    private static final int NOMAL_MODE = 0;
    private static final String TAG = "MultiWindowManager";
    public static final int WINDOW_MODE_LANDSCAPE = 1;
    public static final int WINDOW_MODE_PHONE = 0;
    public static final int WINDOW_MODE_TABLET = 2;
    static final long WRITE_DELAY = 5000;
    private static int sBigScreenHeight = 700;
    private static int sBigScreenWidth = 1200;
    private static int sBigScreenWindowHeight = 620;
    private static int sBigScreenWindowWidth = 1100;
    private static Rect sMaximizeRect = null;
    private static int sMinWeiXinWidth = WINDOW_MODE_PHONE;
    private static int sNavigationBarHeight = WINDOW_MODE_PHONE;
    private static int sSmallestWindowSize = 100;
    private static int sStatusBarHeight = WINDOW_MODE_PHONE;
    public static Rect sWindowFrameShadowInsets = new Rect(11, 11, 11, 20);
    private static int sWindowFrameTopHeight = WINDOW_MODE_PHONE;
    private static int sWindowMargin = WINDOW_MODE_PHONE;
    boolean mBigScreenInHeight;
    boolean mBigScreenInWidth;
    DisplayInfo mDisplayInfo;
    private final HashMap<String, Entry> mEntries = new HashMap();
    final AtomicFile mFile;
    final Handler mHandler;
    DisplayMetrics mRealDisplayMetrics = null;
    ActivityManagerService mService;
    int mSystemUiMode = WINDOW_MODE_TABLET;
    final Runnable mWriteRunner = new Runnable() {
        public void run() {
            synchronized (MultiWindowManager.this) {
                MultiWindowManager.this.mWriteScheduled = false;
                new AsyncTask<Void, Void, Void>() {
                    protected Void doInBackground(Void... params) {
                        MultiWindowManager.this.writeSettingsLocked();
                        return null;
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            }
        }
    };
    boolean mWriteScheduled;
    private HashMap<String, Integer> writeList = null;

    public static class Entry {
        public Rect landscapeRect = new Rect();
        public int mode;
        public final String packageName;
        public Rect portraitRect = new Rect();

        public Entry(String _name) {
            this.packageName = _name;
        }
    }

    public MultiWindowManager(ActivityManagerService service) {
        this.mService = service;
        this.mHandler = new Handler();
        this.mFile = new AtomicFile(new File(new File(Environment.getDataDirectory(), "system"), "app_window_settings.xml"));
        loadAppWindowRects();
        /**这里主要和窗口模式切换相关*/
        /*
        CZOnlineConfigManager.getInstance().addConfigUpdateListener("window_open_mode", new ConfigUpdateListener() {
            public void onConfigUpdated(String name, int version, String filePath) {
                Log.d(MultiWindowManager.TAG, "onConfigUpdated, reloading...");
                MultiWindowManager.this.loadWriteList();
            }
        });
       */
    }

    public boolean moveAppWindow(ActivityRecord r, int dx, int dy) {
        int stackId = r.task.stack.mStackId;
        String processName = r.processName;
        Rect rect = this.mService.getStackInfo(stackId).bounds;
        rect.offset(dx, dy);
        storeWindowPos(processName, rect);
        this.mService.mWindowManager.moveStack(stackId, rect.left, rect.top);
        return DEBUG;
    }

    public boolean maximizeAppWindow(ActivityRecord r, boolean max) {
        int stackId = r.task.stack.mStackId;
        String processName = r.processName;
        Rect rect = getMaximizeWindowRect(this.mService.mWindowManager);
        storeWindowPos(processName, rect);
        updateAppPrefSize(r.app.thread, getAppSize(processName, rect));
        this.mService.resizeStack(stackId, rect);
        return DEBUG;
    }

    public boolean resizeAppWindow(ActivityRecord record, Rect r) {
        if (!isRectPosValid(r)) {
            return false;
        }
        String processName = record.processName;
        int stackId = record.task.stack.mStackId;
        storeWindowPos(processName, r);
        updateAppPrefSize(record.app.thread, getAppSize(processName, r));
        this.mService.resizeStack(stackId, r);
        return DEBUG;
    }

    public Rect getNewStackRect(ActivityRecord r) {
        String name = r.processName;
        Rect focusAppWindow = null;
        ActivityStack focusStack = this.mService.getFocusedStack();
        StackInfo info = null;
        if (!(focusStack == null || focusStack.isHomeStack())) {
            info = this.mService.mStackSupervisor.getStackInfo(this.mService.getFocusedStack());
        }
        if (info != null) {
            focusAppWindow = info.bounds;
        }
        Rect outRect = getStoredWindowPos(name, false);
        if (outRect == null) {
            outRect = getAppDefaultWindowPos(name, focusAppWindow);
        } else if (outRect.equals(getMaximizeWindowRect(this.mService.mWindowManager))) {
            return outRect;
        } else {
            int index = name.indexOf(58);
            String packageName = name;
            if (index != -1) {
                packageName = name.substring(WINDOW_MODE_PHONE, index);
            }
            Rect last = getLastAppWindowPos(packageName, -1);
            if (last != null) {
                outRect.set(last);
                outRect.offset(sWindowMargin, sWindowMargin);
            }
        }
        adjustWindowPos(outRect, focusAppWindow);
        return outRect;
    }

    public void updateProcessPrefSize(ActivityRecord record) {
        updateAppPrefSize(record.app.thread, getAppSize(record.processName, this.mService.getStackInfo(record.task.stack.mStackId).bounds));
    }

    public void handleOrientationChanged() {
        DisplayInfo displayInfo = this.mService.mWindowManager.getDefaultDisplayInfoLocked();
        this.mRealDisplayMetrics = new DisplayMetrics();
        displayInfo.getAppMetrics(this.mRealDisplayMetrics);
        ActivityStack mainStack = this.mService.mStackSupervisor.getFocusedStack();
        Rect lastRect = null;
        if (mainStack != null && mainStack.mStacks != null) {
            Iterator i$ = mainStack.mStacks.iterator();
            while (i$.hasNext()) {
                ActivityStack stack = (ActivityStack) i$.next();
                if (!stack.isHomeStack()) {
                    ActivityRecord targetRecord = stack.mResumedActivity;
                    if (targetRecord == null) {
                        targetRecord = stack.mLastPausedActivity;
                    }
                    if (!(targetRecord == null || targetRecord.app == null || targetRecord.app.thread == null)) {
                        String processName = targetRecord.processName;
                        Rect rect = getWindowPosForRotationChange(processName, stack.mStackId, lastRect);
                        lastRect = rect;
                        updateAppPrefSize(targetRecord.app.thread, getAppSize(processName, rect));
                        this.mService.mWindowManager.resizeStackForRotation(stack.mStackId, rect);
                    }
                }
            }
        }
    }

    private void updateAppPrefSize(IApplicationThread thread, Rect rect) {
        try {
            thread.scheduleUpdateAppPrefSize(rect);
        } catch (Exception e) {
        }
    }

    private void loadDimens() {
        Context context = this.mService.mContext;
        try {
            sBigScreenWidth = (int) context.getResources().getDimension(17105159);
            sWindowFrameTopHeight = (int) context.getResources().getDimension(17105158);
            sBigScreenWindowWidth = (int) context.getResources().getDimension(17105161);
            sBigScreenHeight = (int) context.getResources().getDimension(17105160);
            sBigScreenWindowHeight = (int) context.getResources().getDimension(17105162);
            sWindowMargin = (int) context.getResources().getDimension(17105163);
            sNavigationBarHeight = context.getResources().getDimensionPixelSize(17104915);
            context.getResources().getDrawable(17303409).getPadding(sWindowFrameShadowInsets);
            DisplayInfo displayInfo = this.mService.mWindowManager.getDefaultDisplayInfoLocked();
            this.mRealDisplayMetrics = new DisplayMetrics();
            displayInfo.getAppMetrics(this.mRealDisplayMetrics);
            sMinWeiXinWidth = (int) (this.mRealDisplayMetrics.density * 580.0f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    int getSystemAppWindowMode() {
        return this.mSystemUiMode;
    }

    void setSystemAppWindowMode(int mode) {
        this.mSystemUiMode = mode;
        this.mService.mWindowManager.getDefaultDisplayInfoLocked().getAppMetrics(this.mRealDisplayMetrics);
        scheduleWriteLocked();
    }

    public Rect getWindowPosForRotationChange(String name, int stackId, Rect lastRect) {
        Log.d(TAG, "getWindowPosForRotationChange Orientation:" + this.mService.mConfiguration.orientation + "  Name:" + name);
        Rect outRect = getStoredWindowPos(name, false);
        if (outRect == null) {
            outRect = getAppDefaultWindowPos(name, null);
        } else if (outRect.equals(getMaximizeWindowRect(this.mService.mWindowManager))) {
            return outRect;
        } else {
            int index = name.indexOf(58);
            String packageName = name;
            if (index != -1) {
                packageName = name.substring(WINDOW_MODE_PHONE, index);
            }
            Rect last = getLastAppWindowPos(packageName, stackId);
            if (last != null) {
                outRect.offsetTo(last.left + sWindowMargin, last.top + sWindowMargin);
            }
        }
        adjustWindowPos(outRect, lastRect);
        return outRect;
    }

    private void adjustWindowPos(Rect rect, Rect focsedWindow) {
        DisplayInfo displayInfo = this.mService.mWindowManager.getDefaultDisplayInfoLocked();
        int width = displayInfo.appWidth;
        int height = displayInfo.appHeight;
        if (rect.right > sWindowFrameShadowInsets.right + width || rect.left < (-sWindowFrameShadowInsets.left) || rect.bottom > sWindowFrameShadowInsets.bottom + height || rect.top < sStatusBarHeight - sWindowFrameShadowInsets.top) {
            rect.offsetTo(-sWindowFrameShadowInsets.left, sStatusBarHeight - sWindowFrameShadowInsets.top);
        }
        if (focsedWindow != null && rect.left == focsedWindow.left && rect.top == focsedWindow.top) {
            rect.offset(sWindowMargin, sWindowMargin);
        }
    }

    private boolean isRectPosValid(Rect rect) {
        if (rect.width() < sSmallestWindowSize || rect.height() < sSmallestWindowSize) {
            return false;
        }
        DisplayInfo displayInfo = this.mService.mWindowManager.getDefaultDisplayInfoLocked();
        int width = displayInfo.appWidth;
        int height = displayInfo.appHeight;
        if (rect.top < (-sWindowFrameShadowInsets.top) || rect.right < 20 || rect.left > width - 20 || rect.top > height - 10) {
            return false;
        }
        return DEBUG;
    }

    private Rect getLastAppWindowPos(String name, int currentStackId) {
        Rect rect = null;
        ActivityStack focusStack = this.mService.getFocusedStack();
        if (focusStack.mStacks == null) {
            return null;
        }
        for (int ndx = focusStack.mStacks.size() - 1; ndx >= 0; ndx--) {
            ActivityStack stack = (ActivityStack) focusStack.mStacks.get(ndx);
            if (!(stack.mStackId == 0 || !stack.mIsFrontedStack || stack.mStackId == currentStackId)) {
                String packageName = null;
                if (stack.topActivity() != null) {
                    packageName = stack.topActivity().packageName;
                }
                if (packageName != null && packageName.equalsIgnoreCase(name)) {
                    rect = this.mService.getStackInfo(stack.mStackId).bounds;
                    break;
                }
            }
        }
        return rect;
    }

    private Rect getAppDefaultWindowPos(String name, Rect focusAppWindow) {
        Rect outRect = new Rect();
        DisplayInfo displayInfo = this.mService.mWindowManager.getDefaultDisplayInfoLocked();
        int mode = getAppCompatMode(name);
        int left;
        int right;
        int top;
        int bottom;
        if (mode == WINDOW_MODE_LANDSCAPE) {
            if (displayInfo.appWidth > sBigScreenWidth) {
                left = ((displayInfo.appWidth - sBigScreenWindowWidth) / WINDOW_MODE_TABLET) - sWindowFrameShadowInsets.left;
                right = (sBigScreenWindowWidth + left) + sWindowFrameShadowInsets.right;
            } else {
                left = sWindowMargin - sWindowFrameShadowInsets.left;
                right = (displayInfo.appWidth - sWindowMargin) + sWindowFrameShadowInsets.right;
            }
            if (displayInfo.appHeight > sBigScreenHeight) {
                top = ((displayInfo.appHeight - sBigScreenWindowHeight) / WINDOW_MODE_TABLET) - sWindowFrameShadowInsets.top;
                bottom = (sBigScreenWindowHeight + top) + sWindowFrameShadowInsets.bottom;
            } else {
                top = sWindowMargin - sWindowFrameShadowInsets.top;
                bottom = (displayInfo.appHeight - sWindowMargin) + sWindowFrameShadowInsets.bottom;
            }
            outRect.set(left, top, right, bottom);
            if (focusAppWindow != null) {
                if (focusAppWindow.left > displayInfo.appWidth - (sWindowMargin * 4)) {
                    outRect.offsetTo(sWindowMargin, sWindowMargin);
                } else {
                    outRect.offsetTo(focusAppWindow.left + sWindowMargin, focusAppWindow.top + sWindowMargin);
                }
            }
        } else if (mode == 0) {
            int compatWindowMargin = sWindowMargin;
            top = compatWindowMargin - sWindowFrameShadowInsets.top;
            left = compatWindowMargin - sWindowFrameShadowInsets.left;
            bottom = (displayInfo.appHeight - compatWindowMargin) + sWindowFrameShadowInsets.bottom;
            right = (((displayInfo.appHeight - (compatWindowMargin * WINDOW_MODE_TABLET)) * 3) / 4) + left;
            if (right > displayInfo.appWidth) {
                right = (displayInfo.appWidth - compatWindowMargin) + sWindowFrameShadowInsets.right;
                bottom = (((displayInfo.appWidth - (compatWindowMargin * WINDOW_MODE_TABLET)) * 4) / 3) + top;
            }
            outRect.set(left, top, right, bottom);
            if ("com.tencent.mm".equals(name) && outRect.width() < sMinWeiXinWidth) {
                outRect.right = sMinWeiXinWidth + left;
            }
            if (focusAppWindow != null) {
                if ((focusAppWindow.right + outRect.width()) + sWindowMargin > displayInfo.appWidth) {
                    outRect.offsetTo(sWindowMargin, sWindowMargin);
                } else {
                    outRect.offsetTo(focusAppWindow.right + sWindowMargin, sWindowMargin);
                }
            }
        } else if (mode == WINDOW_MODE_TABLET || mode == 3) {
            outRect = getMaximizeWindowRect(this.mService.mWindowManager);
        }
        if (!(mode == WINDOW_MODE_TABLET || focusAppWindow == null || !windowOverlapped(outRect))) {
            outRect.offset(sWindowMargin / WINDOW_MODE_TABLET, (-sWindowMargin) / WINDOW_MODE_TABLET);
            if (windowOverlapped(outRect)) {
                outRect.offset(-sWindowMargin, sWindowMargin);
            }
        }
        return outRect;
    }

    private boolean windowOverlapped(Rect rect) {
        for (StackInfo info : this.mService.getAllStackInfos()) {
            if (rect.equals(info.bounds)) {
                return DEBUG;
            }
        }
        return false;
    }

    public int getAppCompatMode(String name) {
        int app_compat_mode = WINDOW_MODE_PHONE;
        int pos = name.indexOf(58);
        if (pos == -1) {
            pos = name.length();
        }
        String packageName = name.substring(WINDOW_MODE_PHONE, pos);
        if (packageName.contains("com.android.cts")) {
            return WINDOW_MODE_TABLET;
        }
        if (this.writeList == null) {
            loadWriteList();
        }
        Integer in = (Integer) this.writeList.get(packageName);
        if (in != null) {
            if (in.intValue() == WINDOW_MODE_TABLET) {
                app_compat_mode = WINDOW_MODE_TABLET;
            } else if (in.intValue() == 3) {
                app_compat_mode = 3;
            } else {
                app_compat_mode = WINDOW_MODE_LANDSCAPE;
            }
        }
        return app_compat_mode;
    }

    private void loadWriteList() {
/*        File file;
        FileNotFoundException ex;
        Throwable th;
        IOException ex2;
        this.writeList = new HashMap();
        CZOnlineConfigInfo oci = CZOnlineConfigInfo.getStoredConfigInfo("window_open_mode");
        if (oci != null) {
            file = new File(oci.mFilePath);
        } else {
            file = new File(DEFAULT_FILE_PATH);
        }
        if (file.exists()) {
            FileInputStream fis = null;
            BufferedReader reader = null;
            try {
                FileInputStream fis2 = new FileInputStream(file);
                try {
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(fis2));
                    try {
                        String line = reader2.readLine();
                        while (line != null) {
                            int index = line.indexOf(32);
                            if (index != -1) {
                                this.writeList.put(line.substring(WINDOW_MODE_PHONE, index), Integer.valueOf(Integer.valueOf(line.substring(index + WINDOW_MODE_LANDSCAPE)).intValue()));
                                line = reader2.readLine();
                            }
                        }
                        try {
                            reader2.close();
                            fis2.close();
                            return;
                        } catch (IOException e) {
                            return;
                        }
                    } catch (FileNotFoundException e2) {
                        ex = e2;
                        reader = reader2;
                        fis = fis2;
                        try {
                            ex.printStackTrace();
                            try {
                                reader.close();
                                fis.close();
                                return;
                            } catch (IOException e3) {
                                return;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            try {
                                reader.close();
                                fis.close();
                            } catch (IOException e4) {
                            }
                            throw th;
                        }
                    } catch (IOException e5) {
                        ex2 = e5;
                        reader = reader2;
                        fis = fis2;
                        ex2.printStackTrace();
                        try {
                            reader.close();
                            fis.close();
                            return;
                        } catch (IOException e6) {
                            return;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        reader = reader2;
                        fis = fis2;
                        reader.close();
                        fis.close();
                        throw th;
                    }
                } catch (FileNotFoundException e7) {
                    ex = e7;
                    fis = fis2;
                    ex.printStackTrace();
                    reader.close();
                    fis.close();
                    return;
                } catch (IOException e8) {
                    ex2 = e8;
                    fis = fis2;
                    ex2.printStackTrace();
                    reader.close();
                    fis.close();
                    return;
                } catch (Throwable th4) {
                    th = th4;
                    fis = fis2;
                    reader.close();
                    fis.close();
                    //throw th;
                    return;
                }
            } catch (FileNotFoundException e9) {
                ex = e9;
                ex.printStackTrace();
                reader.close();
                fis.close();
                return;
            } catch (IOException e10) {
                ex2 = e10;
                ex2.printStackTrace();
                reader.close();
                fis.close();
                return;
            }
        }
        this.writeList.put("com.android.settings", Integer.valueOf(WINDOW_MODE_LANDSCAPE));
        this.writeList.put(ActivityManagerService.CZHomePackageName, Integer.valueOf(WINDOW_MODE_LANDSCAPE));
        this.writeList.put("com.chaozhuo.browser", Integer.valueOf(WINDOW_MODE_LANDSCAPE));
        this.writeList.put("com.chaozhuo.permission.controller", Integer.valueOf(WINDOW_MODE_LANDSCAPE));
        this.writeList.put("com.chaozhuo.texteditor", Integer.valueOf(WINDOW_MODE_LANDSCAPE));
*/
    }

    public Rect getRestoredAppWindowPos(String name) {
        Rect outRect = getStoredWindowPos(name, DEBUG);
        if (outRect == null) {
            return getAppDefaultWindowPos(name, null);
        }
        return outRect;
    }

    public void storeWindowPos(String name, Rect r) {
        if (r.left == 0 && r.top == 0 && r.right == 0 && r.bottom == 0) {
            this.mEntries.remove(name);
            return;
        }
        Entry entry = (Entry) this.mEntries.get(name);
        if (entry == null) {
            entry = new Entry(name);
            this.mEntries.put(name, entry);
        }
        int orientation = this.mService.mConfiguration.orientation;
        Log.d(TAG, "StoreWindowPos Orientation:" + this.mService.mConfiguration.orientation + "  Name:" + name + " Rect:" + r.toString());
        if (r.equals(getMaximizeWindowRect(this.mService.mWindowManager))) {
            entry.mode = WINDOW_MODE_LANDSCAPE;
        } else {
            if (orientation == WINDOW_MODE_LANDSCAPE) {
                entry.portraitRect.set(r);
            } else {
                entry.landscapeRect.set(r);
            }
            entry.mode = WINDOW_MODE_PHONE;
        }
        scheduleWriteLocked();
    }

    public Rect getAppSize(String name, Rect windowPos) {
        if (name.equals("com.android.systemui") || name.equals("system_process")) {
            return new Rect();
        }
        if (sWindowMargin == 0) {
            loadDimens();
        }
        if (windowPos == null) {
            windowPos = getStoredWindowPos(name, false);
            if (windowPos == null) {
                windowPos = getAppDefaultWindowPos(name, null);
            }
        }
        Rect outRect = new Rect();
        outRect.left = windowPos.left + sWindowFrameShadowInsets.left;
        outRect.top = (windowPos.top + sWindowFrameShadowInsets.top) + sWindowFrameTopHeight;
        outRect.bottom = windowPos.bottom - sWindowFrameShadowInsets.bottom;
        outRect.right = windowPos.right - sWindowFrameShadowInsets.right;
        Log.d(TAG, "getAppSize Orientation:" + this.mService.mConfiguration.orientation + "  Name:" + name + " Rect:" + windowPos.toString());
        return outRect;
    }

    private Rect getStoredWindowPos(String name, boolean restored) {
        Rect windowPos = null;
        Entry entry = (Entry) this.mEntries.get(name);
        if (entry != null) {
            windowPos = new Rect();
            if (entry.mode != WINDOW_MODE_LANDSCAPE || restored) {
                if (this.mService.mConfiguration.orientation == WINDOW_MODE_LANDSCAPE) {
                    windowPos.set(entry.portraitRect);
                } else {
                    windowPos.set(entry.landscapeRect);
                }
                if (windowPos.right <= windowPos.left || windowPos.bottom <= windowPos.top || ((double) windowPos.width()) > ((double) this.mRealDisplayMetrics.widthPixels) * 1.1d || ((double) windowPos.height()) > ((double) this.mRealDisplayMetrics.heightPixels) * 1.1d) {
                    windowPos = null;
                }
                if (getAppCompatMode(name) == WINDOW_MODE_TABLET) {
                    windowPos = null;
                }
            } else {
                windowPos.set(getMaximizeWindowRect(this.mService.mWindowManager));
            }
        }
        Log.d(TAG, "getStoredWindowPos Orientation:" + this.mService.mConfiguration.orientation + "  Name:" + name + " Rect:" + windowPos);
        return windowPos;
    }

    public static Rect getMaximizeWindowRect(WindowManagerService service) {
        DisplayInfo displayInfo = service.getDefaultDisplayInfoLocked();
        return new Rect(-sWindowFrameShadowInsets.left, -sWindowFrameShadowInsets.top, displayInfo.appWidth + sWindowFrameShadowInsets.right, (displayInfo.logicalHeight - sNavigationBarHeight) + sWindowFrameShadowInsets.bottom);
    }

    private void scheduleWriteLocked() {
        if (!this.mWriteScheduled) {
            this.mWriteScheduled = DEBUG;
            this.mHandler.postDelayed(this.mWriteRunner, WRITE_DELAY);
        }
    }

    private void loadAppWindowRects() {
        try {
            FileInputStream stream = this.mFile.openRead();
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, "utf-8");
            Entry entry = null;
            String entryKey = null;
            for (int event = parser.getEventType(); event != WINDOW_MODE_LANDSCAPE; event = parser.next()) {
                switch (event) {
                    case WINDOW_MODE_TABLET:
                        try {
                            if (!"system_app_window_mode".equals(parser.getName())) {
                                if (!"pkg".equals(parser.getName())) {
                                    if ("orientation".equals(parser.getName())) {
                                        String ori = parser.getAttributeValue(null, "value");
                                        if (entry != null) {
                                            if (!ori.equalsIgnoreCase("portrait")) {
                                                entry.landscapeRect.left = getIntAttribute(parser, "left");
                                                entry.landscapeRect.top = getIntAttribute(parser, "top");
                                                entry.landscapeRect.right = getIntAttribute(parser, "right");
                                                entry.landscapeRect.bottom = getIntAttribute(parser, "bottom");
                                                break;
                                            }
                                            entry.portraitRect.left = getIntAttribute(parser, "left");
                                            entry.portraitRect.top = getIntAttribute(parser, "top");
                                            entry.portraitRect.right = getIntAttribute(parser, "right");
                                            entry.portraitRect.bottom = getIntAttribute(parser, "bottom");
                                            break;
                                        }
                                    }
                                }
                                String name = parser.getAttributeValue(null, "name");
                                if (name != null) {
                                    entryKey = name;
                                    entry = new Entry(name);
                                    entry.mode = getIntAttribute(parser, "mode");
                                    break;
                                }
                            }
                            this.mSystemUiMode = getIntAttribute(parser, "system_mode");
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                            //return;
                        }
                        break;
                    case 3:
                        if ("pkg".equals(parser.getName())) {
                            this.mEntries.put(entryKey, entry);
                            entryKey = null;
                            entry = null;
                            break;
                        }
                        break;
                }
            }
        } catch (FileNotFoundException e2) {
            Slog.i(TAG, "No existing display settings " + this.mFile.getBaseFile() + "; starting empty");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private int getIntAttribute(XmlPullParser parser, String name) {
        int i = WINDOW_MODE_PHONE;
        try {
            String str = parser.getAttributeValue(null, name);
            if (str != null) {
                i = Integer.parseInt(str);
            }
        } catch (NumberFormatException e) {
        }
        return i;
    }

    private void writeSettingsLocked() {
        try {
            FileOutputStream stream = this.mFile.startWrite();
            try {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, "utf-8");
                out.startDocument(null, Boolean.valueOf(DEBUG));
                out.startTag(null, "app_window_settings");
                out.startTag(null, "system_app_window_mode");
                out.attribute(null, "system_mode", Integer.toString(this.mSystemUiMode));
                out.endTag(null, "system_app_window_mode");
                for (Entry entry : this.mEntries.values()) {
                    if (!(entry.landscapeRect == null || entry.portraitRect == null)) {
                        out.startTag(null, "pkg");
                        out.attribute(null, "name", entry.packageName);
                        out.attribute(null, "mode", Integer.toString(entry.mode));
                        out.startTag(null, "orientation");
                        out.attribute(null, "value", "portrait");
                        out.attribute(null, "left", Integer.toString(entry.portraitRect.left));
                        out.attribute(null, "top", Integer.toString(entry.portraitRect.top));
                        out.attribute(null, "right", Integer.toString(entry.portraitRect.right));
                        out.attribute(null, "bottom", Integer.toString(entry.portraitRect.bottom));
                        out.endTag(null, "orientation");
                        out.startTag(null, "orientation");
                        out.attribute(null, "value", "landscape");
                        out.attribute(null, "left", Integer.toString(entry.landscapeRect.left));
                        out.attribute(null, "top", Integer.toString(entry.landscapeRect.top));
                        out.attribute(null, "right", Integer.toString(entry.landscapeRect.right));
                        out.attribute(null, "bottom", Integer.toString(entry.landscapeRect.bottom));
                        out.endTag(null, "orientation");
                        out.endTag(null, "pkg");
                    }
                }
                out.endTag(null, "app_window_settings");
                out.endDocument();
                this.mFile.finishWrite(stream);
            } catch (IOException e) {
                Slog.w(TAG, "Failed to write display settings, restoring backup.", e);
                this.mFile.failWrite(stream);
            }
        } catch (IOException e2) {
            Slog.w(TAG, "Failed to write display settings: " + e2);
        }
    }
}
