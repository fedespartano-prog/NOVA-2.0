
package com.novatwo.assistant.network;

import android.content.Context;

public final class RequestSizeManager {

    private static final int LARGE_REQUEST_WARNING = 2000;

    private RequestSizeManager() {
    }

    public static int getCharacterCount(String text) {
        if (text == null) {
            return 0;
        }

        return text.length();
    }

    public static boolean isLargeRequest(String text) {
        return getCharacterCount(text) > LARGE_REQUEST_WARNING;
    }

    public static boolean isRequestAllowed(
            Context context,
            String text
    ) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        if (!DataUsagePolicy.canUseAI(context)) {
            return false;
        }

        int maximum = DataUsagePolicy.getMaxPromptCharacters(context);

        return text.trim().length() <= maximum;
    }

    public static String prepareRequest(
            Context context,
            String text
    ) {
        return DataUsagePolicy.preparePrompt(context, text);
    }

    public static String getRequestWarning(String text) {
        if (isLargeRequest(text)) {
            return "Richiesta lunga: potrebbe consumare più dati mobili.";
        }

        return "";
    }
}
