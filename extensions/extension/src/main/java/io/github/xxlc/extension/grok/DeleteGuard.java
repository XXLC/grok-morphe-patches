package io.github.xxlc.extension.grok;

import android.os.SystemClock;

/** Chặn việc xác nhận xoá chạy lặp khi nhóm Compose của hộp thoại bị recompose trước lúc đóng. */
public final class DeleteGuard {
    private static final long WINDOW_MS = 500;
    private static long lastAcquired = -WINDOW_MS;

    private DeleteGuard() {
    }

    /** True cho lần gọi đầu; các lần gọi trong WINDOW_MS tiếp theo bị từ chối. */
    public static synchronized boolean tryAcquire() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastAcquired < WINDOW_MS) {
            return false;
        }
        lastAcquired = now;
        return true;
    }
}
