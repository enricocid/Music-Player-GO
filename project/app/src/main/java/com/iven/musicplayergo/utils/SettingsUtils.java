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

    public static final int THEME_LIGHT = 0;
    private static final int THEME_DARK = 1;
    public static final int THEME_NIGHT = 2;
    private static final String ACCENT_PREF = "com.iven.musicplayergo.pref_accent";
    private static final String ACCENT_VALUE = "com.iven.musicplayergo.pref_accent_value";
    private static final String THEME_PREF = "com.iven.musicplayergo.pref_theme";
    private static final String THEME_VALUE = "com.iven.musicplayergo.pref_theme_value";

    public static void setNightTheme(Activity activity) {
        SharedPreferences preferences = activity.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE);
        preferences.edit().putInt(THEME_VALUE, THEME_NIGHT).apply();
        activity.recreate();
    }

    public static void setTheme(Activity activity) {
        int contrast = getContrast(activity);
        boolean isThemeNight = isThemeNight(contrast);
        int value = isThemeNight ? THEME_LIGHT : contrast != THEME_DARK ? THEME_DARK : THEME_NIGHT;
        SharedPreferences preferences = activity.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE);
        preferences.edit().putInt(THEME_VALUE, value).apply();
        activity.recreate();
    }

    public static void retrieveTheme(Activity activity, int contrast, int accent) {
        int theme = SettingsUtils.resolveTheme(contrast, accent);
        activity.setTheme(theme);
        if (AndroidVersion.isMarshmallow()) {
            enableLightStatusBar(activity, ContextCompat.getColor(activity, accent));
        }
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

    public static int getContrast(Activity activity) {
        return activity.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE).getInt(THEME_VALUE, THEME_LIGHT);
    }

    private static boolean isThemeNight(int contrast) {
        return contrast == THEME_NIGHT;
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
    private static int resolveTheme(int contrast, int accent) {

        boolean isThemeNight = contrast == THEME_NIGHT;
        boolean isThemeDark = contrast == THEME_DARK;
        int selectedTheme;

        switch (accent) {

            case R.color.red_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeRedNight : isThemeDark ? R.style.AppThemeRedDark : R.style.AppThemeRed;
                break;

            case R.color.pink_A400:
                selectedTheme = isThemeNight ? R.style.AppThemePinkNight : isThemeDark ? R.style.AppThemePinkDark : R.style.AppThemePink;
                break;

            case R.color.purple_A400:
                selectedTheme = isThemeNight ? R.style.AppThemePurpleNight : isThemeDark ? R.style.AppThemePurpleDark : R.style.AppThemePurple;
                break;

            case R.color.deep_purple_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeDeepPurpleNight : isThemeDark ? R.style.AppThemeDeepPurpleDark : R.style.AppThemeDeepPurple;
                break;

            case R.color.indigo_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeIndigoNight : isThemeDark ? R.style.AppThemeIndigoDark : R.style.AppThemeIndigo;
                break;

            case R.color.blue_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeBlueNight : isThemeDark ? R.style.AppThemeBlueDark : R.style.AppThemeBlue;
                break;

            default:
            case R.color.light_blue_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeLightBlueNight : isThemeDark ? R.style.AppThemeLightBlueDark : R.style.AppThemeLightBlue;
                break;

            case R.color.cyan_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeCyanNight : isThemeDark ? R.style.AppThemeCyanDark : R.style.AppThemeCyan;
                break;

            case R.color.teal_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeTealNight : isThemeDark ? R.style.AppThemeTealDark : R.style.AppThemeTeal;
                break;

            case R.color.green_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeGreenNight : isThemeDark ? R.style.AppThemeGreenDark : R.style.AppThemeGreen;
                break;

            case R.color.amber_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeAmberNight : isThemeDark ? R.style.AppThemeAmberDark : R.style.AppThemeAmber;
                break;

            case R.color.orange_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeOrangeNight : isThemeDark ? R.style.AppThemeOrangeDark : R.style.AppThemeOrange;
                break;

            case R.color.deep_orange_A400:
                selectedTheme = isThemeNight ? R.style.AppThemeDeepOrangeNight : isThemeDark ? R.style.AppThemeDeepOrangeDark : R.style.AppThemeDeepOrange;
                break;
        }
        return selectedTheme;
    }
}
