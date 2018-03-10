package com.iven.musicplayergo.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.view.View;

import com.iven.musicplayergo.R;

public class SettingsUtils {

    private static final String THEME_PREF_DARK = "com.iven.musicplayergo.pref_theme_dark";
    private static final String ACCENT_PREF = "com.iven.musicplayergo.pref_theme";
    private static final String ACCENT_VALUE = "com.iven.musicplayergo.pref_value";
    private static final String THEME_VALUE_DARK = "com.iven.musicplayergo.pref_value_dark";

    public static void setThemeDark(Activity activity) {
        boolean isDark = isThemeDark(activity);
        boolean value = !isDark;
        SharedPreferences preferences = activity.getSharedPreferences(THEME_PREF_DARK, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(THEME_VALUE_DARK, value).apply();
        activity.recreate();
    }

    public static void setTheme(Activity activity, boolean isThemeDark, int accent) {
        int theme = SettingsUtils.resolveTheme(isThemeDark, accent);
        activity.setTheme(theme);
        if (AndroidVersion.isMarshmallow()) {
            enableLightStatusBar(activity, ContextCompat.getColor(activity, accent));
        }
    }

    //enable light status bar only for light colors according to
    //https://material.io/guidelines/style/color.html#color-color-palette
    @TargetApi(23)
    public static void enableLightStatusBar(Activity activity, int accent) {

        View decorView = activity.getWindow().getDecorView();
        int oldSystemUiFlags = decorView.getSystemUiVisibility();
        int newSystemUiFlags = oldSystemUiFlags;

        boolean isColorDark = ColorUtils.calculateLuminance(accent) < 0.35;
        if (isColorDark) {
            newSystemUiFlags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            newSystemUiFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }

        //just to avoid to set light status bar if it already enabled and viceversa
        if (newSystemUiFlags != oldSystemUiFlags) {
            decorView.setSystemUiVisibility(newSystemUiFlags);
        }
    }

    public static boolean isThemeDark(Activity activity) {
        return activity.getSharedPreferences(THEME_PREF_DARK, Context.MODE_PRIVATE).getBoolean(THEME_VALUE_DARK, false);
    }

    public static void setThemeAccent(Activity activity, int accent) {
        SharedPreferences preferences = activity.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE);
        preferences.edit().putInt(ACCENT_VALUE, accent).apply();
        activity.recreate();
    }

    public static int getAccent(Activity activity) {
        return activity.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE).getInt(ACCENT_VALUE, R.color.blue_A400);
    }

    //get theme
    private static int resolveTheme(boolean isThemeDark, int accent) {

        int selectedTheme;

        switch (accent) {

            case R.color.red_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeRedDark : R.style.AppThemeRed;
                break;

            case R.color.pink_A400:
                selectedTheme = isThemeDark ? R.style.AppThemePinkDark : R.style.AppThemePink;
                break;

            case R.color.purple_A400:
                selectedTheme = isThemeDark ? R.style.AppThemePurpleDark : R.style.AppThemePurple;
                break;

            case R.color.deep_purple_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeDeepPurpleDark : R.style.AppThemeDeepPurple;
                break;

            case R.color.indigo_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeIndigoDark : R.style.AppThemeIndigo;
                break;

            case R.color.blue_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeBlueDark : R.style.AppThemeBlue;
                break;

            default:
            case R.color.light_blue_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeLightBlueDark : R.style.AppThemeLightBlue;
                break;

            case R.color.cyan_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeCyanDark : R.style.AppThemeCyan;
                break;

            case R.color.teal_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeTealDark : R.style.AppThemeTeal;
                break;

            case R.color.green_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeGreenDark : R.style.AppThemeGreen;
                break;

            case R.color.amber_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeAmberDark : R.style.AppThemeAmber;
                break;

            case R.color.orange_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeOrangeDark : R.style.AppThemeOrange;
                break;

            case R.color.deep_orange_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeDeepOrangeDark : R.style.AppThemeDeepOrange;
                break;
        }
        return selectedTheme;
    }
}
