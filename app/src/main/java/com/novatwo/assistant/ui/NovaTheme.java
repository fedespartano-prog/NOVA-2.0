
package com.novatwo.assistant.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

public final class NovaTheme {

    private NovaTheme() {
    }

    public static void applyBackground(View view) {
        if (view == null) {
            return;
        }

        view.setBackgroundColor(NovaColors.BACKGROUND);
    }

    public static GradientDrawable createPanelBackground() {
        GradientDrawable background = new GradientDrawable();

        background.setColor(NovaColors.SURFACE);
        background.setCornerRadius(dp(20));

        background.setStroke(
                dp(1),
                NovaColors.SURFACE_LIGHT
        );

        return background;
    }

    public static GradientDrawable createButtonBackground(
            Context context
    ) {
        GradientDrawable background = new GradientDrawable();

        background.setColor(NovaColors.PRIMARY);
        background.setCornerRadius(dp(28));

        return background;
    }

    public static GradientDrawable createGlowBackground() {
        GradientDrawable background = new GradientDrawable();

        background.setShape(GradientDrawable.OVAL);
        background.setColor(NovaColors.SURFACE);
        background.setStroke(
                dp(2),
                NovaColors.PRIMARY
        );

        return background;
    }

    private static int dp(float value) {
        return Math.round(
                value * android.content.res.Resources
                        .getSystem()
                        .getDisplayMetrics().density
        );
    }
}
