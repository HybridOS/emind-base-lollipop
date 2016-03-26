package android.content.res;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.view.SurfaceControl;

public class MultiWindowCompatibility implements Parcelable {
    public static final Creator<MultiWindowCompatibility> CREATOR = new Creator<MultiWindowCompatibility>() {
        public MultiWindowCompatibility createFromParcel(Parcel source) {
            return new MultiWindowCompatibility(source);
        }

        public MultiWindowCompatibility[] newArray(int size) {
            return new MultiWindowCompatibility[size];
        }
    };
    public static final MultiWindowCompatibility DEFAULT_COMPATIBILITY_INFO = new MultiWindowCompatibility(0) {
    };
    public static final int WINDOW_COMPAT_LANDSCAPE = 1;
    public static final int WINDOW_COMPAT_MAXIMIZE = 3;
    public static final int WINDOW_COMPAT_PHONE = 0;
    public static final int WINDOW_COMPAT_TABLET = 2;
    public final int mCompatMode;

    public MultiWindowCompatibility(int mode) {
        this.mCompatMode = mode;
    }

    private MultiWindowCompatibility(Parcel source) {
        this.mCompatMode = source.readInt();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(SurfaceControl.SECURE);
        sb.append("{");
        sb.append("mode:");
        sb.append(this.mCompatMode);
        sb.append("}");
        return sb.toString();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mCompatMode);
    }

    public int describeContents() {
        return WINDOW_COMPAT_PHONE;
    }
}
