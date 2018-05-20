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

    private static final String ACCENT_PREF = "com.iven.musicplayergo.pref_accent";
    private static final String ACCENT_VALUE = "com.iven.musicplayergo.pref_accent_value";
    private static final String THEME_PREF = "com.iven.musicplayergo.pref_theme";
    private static final String THEME_VALUE = "com.iven.musicplayergo.pref_theme_value";


    public static void invertTheme(Activity activity) {
        boolean isDark = isThemeInverted(activity);
        boolean value = !isDark;
        SharedPreferences preferences = activity.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(THEME_VALUE, value).apply();
        activity.recreate();
    }

    public static boolean isThemeInverted(Activity activity) {
        return activity.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE).getBoolean(THEME_VALUE, false);
    }

    public static void setTheme(Activity activity, boolean isThemeInverted, int accent) {
        int theme = SettingsUtils.resolveTheme(isThemeInverted, accent);
        activity.setTheme(theme);
        if (AndroidVersion.isMarshmallow()) {
            enableLightStatusBar(activity, ContextCompat.getColor(activity, accent));
        }
    }

    //get theme
    private static int resolveTheme(boolean isThemeDark, int accent) {

        int selectedTheme;

        switch (accent) {

            case R.color.red_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeRedNight : R.style.AppThemeRed;
                break;

            case R.color.pink_A400:
                selectedTheme = isThemeDark ? R.style.AppThemePinkNight : R.style.AppThemePink;
                break;

            case R.color.purple_A400:
                selectedTheme = isThemeDark ? R.style.AppThemePurpleNight : R.style.AppThemePurple;
                break;

            case R.color.deep_purple_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeDeepPurpleNight : R.style.AppThemeDeepPurple;
                break;

            case R.color.indigo_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeIndigoNight : R.style.AppThemeIndigo;
                break;

            case R.color.blue_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeBlueNight : R.style.AppThemeBlue;
                break;

            default:
            case R.color.light_blue_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeLightBlueNight : R.style.AppThemeLightBlue;
                break;

            case R.color.cyan_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeCyanNight : R.style.AppThemeCyan;
                break;

            case R.color.teal_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeTealNight : R.style.AppThemeTeal;
                break;

            case R.color.green_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeGreenNight : R.style.AppThemeGreen;
                break;

            case R.color.amber_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeAmberNight : R.style.AppThemeAmber;
                break;

            case R.color.orange_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeOrangeNight : R.style.AppThemeOrange;
                break;

            case R.color.deep_orange_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeDeepOrangeNight : R.style.AppThemeDeepOrange;
                break;
        }
        return selectedTheme;
    }

    //enable light status bar only for light colors according to
    //https://material.io/guidelines/style/color.html#color-color-palette
    @TargetApi(23)
    private static void enableLightStatusBar(Activity activity, int accent) {

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

    public static void setThemeAccent(Activity activity, int accent) {
        SharedPreferences preferences = activity.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE);
        preferences.edit().putInt(ACCENT_VALUE, accent).apply();
        activity.recreate();
    }

    public static int getAccent(Activity activity) {
        return activity.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE).getInt(ACCENT_VALUE, R.color.blue_A400);
    }
}
