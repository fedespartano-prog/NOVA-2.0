
package com.novatwo.assistant.network;

import java.util.Locale;

public final class DataUsageFormatter {

    private DataUsageFormatter() {
    }

    public static String formatBytes(long bytes) {

        if (bytes < 0) {
            bytes = 0;
        }

        if (bytes < 1024) {
            return bytes + " B";
        }

        double kilobytes = bytes / 1024.0;

        if (kilobytes < 1024) {
            return String.format(
                    Locale.ITALIAN,
                    "%.2f KB",
                    kilobytes
            );
        }

        double megabytes = kilobytes / 1024.0;

        if (megabytes < 1024) {
            return String.format(
                    Locale.ITALIAN,
                    "%.2f MB",
                    megabytes
            );
        }

        double gigabytes = megabytes / 1024.0;

        return String.format(
                Locale.ITALIAN,
                "%.2f GB",
                gigabytes
        );
    }

    public static String formatMegabytes(double megabytes) {
        return String.format(
                Locale.ITALIAN,
                "%.2f MB",
                Math.max(0, megabytes)
        );
    }
}
