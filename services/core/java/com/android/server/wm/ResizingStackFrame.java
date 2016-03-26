package com.android.server.wm;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.util.Slog;
import android.view.Display;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceControl;
import android.view.SurfaceSession;

class ResizingStackFrame {
    private static final float ALPHA = 1.0f;
    private static final String TAG = "ResizingStackFrame";
    private int THICKNESS = 11;
    final Rect mBounds = new Rect();
    Display mDisplay;
    private final Rect mLastBounds = new Rect();
    private final Surface mSurface = new Surface();
    private final SurfaceControl mSurfaceControl;
    private final Rect mTmpDrawRect = new Rect();

    public ResizingStackFrame(Context context, Display display, SurfaceSession session) {
        SurfaceControl ctrl;
        this.mDisplay = display;
        try {
            ctrl = new SurfaceControl(session, TAG, 1, 1, -3, 4);
            try {
                ctrl.setLayerStack(display.getLayerStack());
                ctrl.setAlpha(ALPHA);
                Point size = new Point();
                this.mDisplay.getSize(size);
                ctrl.setSize(size.x, size.y);
                this.mSurface.copyFrom(ctrl);
            } catch (OutOfResourcesException e) {
            }
        } catch (OutOfResourcesException e2) {
            ctrl = null;
        }
        this.mSurfaceControl = ctrl;
        this.THICKNESS = ((int) context.getResources().getDimension(17105164)) - 2;
    }

    public void onOrientationChanged() {
        Point size = new Point();
        this.mDisplay.getSize(size);
        this.mSurfaceControl.setSize(size.x, size.y);
    }

    private void draw(int color) {
        this.mTmpDrawRect.set(this.mBounds);
        this.mTmpDrawRect.union(this.mLastBounds);
        Canvas c = null;
        try {
            c = this.mSurface.lockCanvas(this.mTmpDrawRect);
        } catch (IllegalArgumentException e) {
        } catch (OutOfResourcesException e2) {
        }
        if (c != null) {
            Slog.i(TAG, "draw: on= mLastBounds=" + this.mLastBounds.toShortString() + " mBounds=" + this.mBounds.toShortString() + " mTmpDrawRect=" + this.mTmpDrawRect.toShortString());
            int w = this.mBounds.width();
            int h = this.mBounds.height();
            c.drawColor(0, Mode.CLEAR);
            this.mTmpDrawRect.set(this.mBounds.left, this.mBounds.top, this.mBounds.right, this.mBounds.top + this.THICKNESS);
            c.clipRect(this.mTmpDrawRect, Op.REPLACE);
            c.drawColor(color);
            this.mTmpDrawRect.set(this.mBounds.left, this.mBounds.top + this.THICKNESS, this.mBounds.left + this.THICKNESS, this.mBounds.bottom - this.THICKNESS);
            c.clipRect(this.mTmpDrawRect, Op.REPLACE);
            c.drawColor(color);
            this.mTmpDrawRect.set(this.mBounds.right - this.THICKNESS, this.mBounds.top + this.THICKNESS, this.mBounds.right, this.mBounds.bottom - this.THICKNESS);
            c.clipRect(this.mTmpDrawRect, Op.REPLACE);
            c.drawColor(color);
            this.mTmpDrawRect.set(this.mBounds.left, this.mBounds.bottom - this.THICKNESS, this.mBounds.right, this.mBounds.bottom);
            c.clipRect(this.mTmpDrawRect, Op.REPLACE);
            c.drawColor(color);
            this.mSurface.unlockCanvasAndPost(c);
        }
    }

    private void positionSurface(Rect bounds) {
    }

    public void setVisibility(boolean on) {
        if (this.mSurfaceControl != null) {
            if (on) {
                if (!this.mLastBounds.equals(this.mBounds)) {
                    draw(-1426063361);
                    positionSurface(this.mBounds);
                    this.mLastBounds.set(this.mBounds);
                }
                this.mSurfaceControl.show();
                return;
            }
            this.mSurfaceControl.hide();
        }
    }

    public void setBounds(Rect rect) {
        this.mBounds.set(rect);
    }

    public void setLayer(int layer) {
        this.mSurfaceControl.setLayer(layer);
    }
}
