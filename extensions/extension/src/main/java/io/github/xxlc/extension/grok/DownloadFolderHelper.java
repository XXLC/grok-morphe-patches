package io.github.xxlc.extension.grok;

import java.util.Locale;

/** Phân loại thư mục tải xuống theo MIME hoặc phần mở rộng tệp. */
public final class DownloadFolderHelper {
    private static final String FOLDER_ROOT = "Download/Grok";
    private static final String FOLDER_PICTURES = "Download/Grok/Pictures";
    private static final String FOLDER_VIDEOS = "Download/Grok/Videos";

    private DownloadFolderHelper() {
    }

    public static String resolveFolder(String mime, String fileName) {
        try {
            if (mime != null) {
                String normalizedMime = mime.trim().toLowerCase(Locale.ROOT);
                int semi = normalizedMime.indexOf(';');
                if (semi >= 0) {
                    normalizedMime = normalizedMime.substring(0, semi).trim();
                }
                if (normalizedMime.startsWith("image/")) {
                    return FOLDER_PICTURES;
                }
                if (normalizedMime.startsWith("video/")) {
                    return FOLDER_VIDEOS;
                }
            }
            if (fileName != null) {
                String clean = fileName.trim();
                int q = clean.indexOf('?');
                if (q >= 0) {
                    clean = clean.substring(0, q);
                }
                int hash = clean.indexOf('#');
                if (hash >= 0) {
                    clean = clean.substring(0, hash);
                }
                int dot = clean.lastIndexOf('.');
                if (dot >= 0 && dot + 1 < clean.length()) {
                    String ext = clean.substring(dot + 1).toLowerCase(Locale.ROOT);
                    if (isImageExtension(ext)) {
                        return FOLDER_PICTURES;
                    }
                    if (isVideoExtension(ext)) {
                        return FOLDER_VIDEOS;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Không ném ngoại lệ ra luồng tải của ứng dụng.
        }
        return FOLDER_ROOT;
    }

    public static String resolveDownloadManagerSubPath(String mime, String fileName) {
        String folder = resolveFolder(mime, fileName);
        String subFolder = folder.startsWith("Download/") ? folder.substring("Download/".length()) : "Grok";
        String safeName = (fileName == null || fileName.trim().isEmpty()) ? "DownloadedFile" : fileName;
        return subFolder + "/" + safeName;
    }

    private static boolean isImageExtension(String ext) {
        return "jpg".equals(ext)
                || "jpeg".equals(ext)
                || "png".equals(ext)
                || "webp".equals(ext)
                || "gif".equals(ext)
                || "heic".equals(ext)
                || "heif".equals(ext);
    }

    private static boolean isVideoExtension(String ext) {
        return "mp4".equals(ext)
                || "mov".equals(ext)
                || "webm".equals(ext)
                || "mkv".equals(ext)
                || "3gp".equals(ext);
    }
}
