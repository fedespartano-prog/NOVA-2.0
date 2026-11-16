
package com.novatwo.assistant.ui;

import android.graphics.Typeface;

public final class NovaTypography {

    private NovaTypography() {
    }

    // Dimensioni dei testi in sp
    public static final float TEXT_SMALL = 12f;
    public static final float TEXT_NORMAL = 16f;
    public static final float TEXT_MEDIUM = 18f;
    public static final float TEXT_LARGE = 22f;
    public static final float TEXT_TITLE = 28f;
    public static final float TEXT_DISPLAY = 36f;

    // Stili dei caratteri
    public static final Typeface REGULAR =
            Typeface.create("sans-serif", Typeface.NORMAL);

    public static final Typeface MEDIUM =
            Typeface.create("sans-serif-medium", Typeface.NORMAL);

    public static final Typeface BOLD =
            Typeface.create("sans-serif", Typeface.BOLD);

    // Nomi degli stili
    public static final String STYLE_REGULAR = "regular";
    public static final String STYLE_MEDIUM = "medium";
    public static final String STYLE_BOLD = "bold";

}
