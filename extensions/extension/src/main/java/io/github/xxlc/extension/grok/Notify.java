package io.github.xxlc.extension.grok;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.Locale;

/** Thông báo ngắn cho số yêu cầu tải đã được chuẩn bị trong thư viện. */
public final class Notify {
    private Notify() {
    }

    public static void queued(Context suppliedContext, int count) {
        try {
            Context context = suppliedContext;
            if (context == null) {
                context = application("android.app.ActivityThread", "currentApplication");
            }
            if (context == null) {
                context = application("android.app.AppGlobals", "getInitialApplication");
            }
            if (context == null) {
                return;
            }
            Context toastContext = context;

            // ID tài nguyên được cấp lúc vá chưa được truyền vào tiện ích này.
            Locale locale = Locale.getDefault();
            String format = "vi".equals(locale.getLanguage())
                    ? "Đã đưa %1$d tệp vào hàng đợi tải"
                    : "Queued %1$d files for download";
            String message = String.format(locale, format, count);
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(toastContext, message, Toast.LENGTH_SHORT).show();
                } catch (Exception ignored) {
                    // Không để lỗi thông báo làm gián đoạn thao tác tải.
                }
            });
        } catch (Exception ignored) {
            // Reflection hoặc main looper không khả dụng thì bỏ qua thông báo.
        }
    }

    private static Context application(String className, String methodName) {
        try {
            return (Context) Class.forName(className).getMethod(methodName).invoke(null);
        } catch (Exception ignored) {
            // Bước này không khả dụng thì để bên gọi thử phương án tiếp theo.
            return null;
        }
    }
}
