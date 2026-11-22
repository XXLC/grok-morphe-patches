package io.github.xxlc.extension.grok;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/** Tên media có mili giây và số thứ tự chung cho các luồng trong tiến trình. */
public final class DownloadNameHelper {
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private DownloadNameHelper() {
    }

    public static String appendSequence(String timestamp) {
        return timestamp + "_" + SEQUENCE.incrementAndGet();
    }

    public static String renameMedia(String guessed, String mime) {
        if (guessed == null || !DownloadFolderHelper.isMedia(mime, guessed)) {
            return guessed;
        }
        int dot = guessed.lastIndexOf('.');
        String extension = dot >= 0 ? guessed.substring(dot) : "";
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US)
                .format(new Date());
        return "Grok_" + appendSequence(timestamp) + extension;
    }
}
