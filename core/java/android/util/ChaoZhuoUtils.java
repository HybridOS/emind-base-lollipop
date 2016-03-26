package android.util;

import android.os.Build;
import java.util.Locale;

public class ChaoZhuoUtils {
    public static boolean isPhoenixOSX86() {
        return "android_x86".equals(Build.PRODUCT.toLowerCase(Locale.ENGLISH));
    }
}
