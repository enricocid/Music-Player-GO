package com.iven.musicplayergo.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.view.View;

import com.iven.musicplayergo.R;

public class SettingsUtils {

    private static final String ACCENT_PREF = "com.iven.musicplayergo.pref_accent";
    private static final String ACCENT_VALUE = "com.iven.musicplayergo.pref_accent_value";
    private static final String THEME_PREF = "com.iven.musicplayergo.pref_theme";
    private static final String THEME_VALUE = "com.iven.musicplayergo.pref_theme_value";

    public static void showSettings(@NonNull final View controlsContainer, @NonNull View settingsContainer, boolean show) {

        int settingsVisibility = show ? View.VISIBLE : View.GONE;
        int controlsVisibility = show ? View.GONE : View.VISIBLE;
        controlsContainer.setVisibility(controlsVisibility);
        settingsContainer.setVisibility(settingsVisibility);
    }

    public static void invertTheme(@NonNull final Activity activity) {
        boolean isDark = isThemeInverted(activity);
        boolean value = !isDark;
        SharedPreferences preferences = activity.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(THEME_VALUE, value).apply();
        activity.recreate();
    }

    public static boolean isThemeInverted(@NonNull final Context context) {
        boolean isThemeInverted;
        try {
            isThemeInverted = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE).getBoolean(THEME_VALUE, false);
        } catch (Exception e) {
            isThemeInverted = false;
        }
        return isThemeInverted;
    }

    public static void setTheme(@NonNull final Activity activity, boolean isThemeInverted, int accent) {
        int theme = SettingsUtils.resolveTheme(isThemeInverted, accent);
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

    //get theme
    private static int resolveTheme(boolean isThemeDark, int accent) {

        int selectedTheme;

        switch (accent) {

            case R.color.red_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeRedInverted : R.style.AppThemeRed;
                break;

            case R.color.pink_A400:
                selectedTheme = isThemeDark ? R.style.AppThemePinkInverted : R.style.AppThemePink;
                break;

            case R.color.purple_A400:
                selectedTheme = isThemeDark ? R.style.AppThemePurpleInverted : R.style.AppThemePurple;
                break;

            case R.color.deep_purple_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeDeepPurpleInverted : R.style.AppThemeDeepPurple;
                break;

            case R.color.indigo_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeIndigoInverted : R.style.AppThemeIndigo;
                break;

            case R.color.blue_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeBlueInverted : R.style.AppThemeBlue;
                break;

            default:
            case R.color.light_blue_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeLightBlueInverted : R.style.AppThemeLightBlue;
                break;

            case R.color.cyan_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeCyanInverted : R.style.AppThemeCyan;
                break;

            case R.color.teal_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeTealInverted : R.style.AppThemeTeal;
                break;

            case R.color.green_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeGreenInverted : R.style.AppThemeGreen;
                break;

            case R.color.amber_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeAmberInverted : R.style.AppThemeAmber;
                break;

            case R.color.orange_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeOrangeInverted : R.style.AppThemeOrange;
                break;

            case R.color.deep_orange_A400:
                selectedTheme = isThemeDark ? R.style.AppThemeDeepOrangeInverted : R.style.AppThemeDeepOrange;
                break;

            case R.color.brown_400:
                selectedTheme = isThemeDark ? R.style.AppThemeBrownInverted : R.style.AppThemeBrown;
                break;

            case R.color.gray_400:
                selectedTheme = isThemeDark ? R.style.AppThemeGrayLightInverted : R.style.AppThemeGrayLight;
                break;

            case R.color.gray_800:
                selectedTheme = isThemeDark ? R.style.AppThemeGrayDarkInverted : R.style.AppThemeGrayDark;
                break;

            case R.color.blue_gray_400:
                selectedTheme = isThemeDark ? R.style.AppThemeBlueGrayInverted : R.style.AppThemeBlueGray;
                break;
        }
        return selectedTheme;
    }

    public static void setThemeAccent(@NonNull final Activity activity, int accent) {
        SharedPreferences preferences = activity.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE);
        preferences.edit().putInt(ACCENT_VALUE, accent).apply();
        activity.recreate();
    }

    public static int getAccent(@NonNull final Context context) {
        int accent;
        try {
            accent = context.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE).getInt(ACCENT_VALUE, R.color.blue_A400);
        } catch (Exception e) {
            accent = R.color.blue_A400;
        }
        return accent;
    }
}
