package com.iven.musicplayergo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.iven.musicplayergo.adapters.ArtistsAdapter;
import com.iven.musicplayergo.models.Album;
import com.iven.musicplayergo.models.Artist;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

public class Utils {

    private static final String ACCENT_PREF = "com.iven.musicplayergo.pref_accent";
    private static final String ACCENT_VALUE = "com.iven.musicplayergo.pref_accent_value";
    private static final String THEME_PREF = "com.iven.musicplayergo.pref_theme";
    private static final String THEME_VALUE = "com.iven.musicplayergo.pref_theme_value";
    private static final String SEARCH_BAR_PREF = "com.iven.musicplayergo.pref_search_bar";
    private static final String SEARCH_BAR_VALUE = "com.iven.musicplayergo.pref_search_bar_value";

    static boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    public static void setCardStroke(@NonNull final Context context, @NonNull final MaterialCardView container, final int color) {
        container.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.selected_card_stroke_width));
        container.setStrokeColor(color);
    }

    static void indexArtistAlbums(@NonNull final List<Album> albums) {
        try {
            for (int i = 0; i < albums.size(); i++) {
                albums.get(i).setAlbumPosition(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Spanned buildSpanned(@NonNull final String res) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                Html.fromHtml(res, Html.FROM_HTML_MODE_LEGACY) :
                Html.fromHtml(res);
    }

    static void invertTheme(@NonNull final Activity activity) {
        final boolean isDark = isThemeInverted(activity);
        final boolean value = !isDark;
        final SharedPreferences preferences = activity.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(THEME_VALUE, value).apply();
        activity.recreate();
    }

    static boolean isThemeInverted(@NonNull final Context context) {
        boolean isThemeInverted;
        try {
            isThemeInverted = context.getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE).getBoolean(THEME_VALUE, false);
        } catch (Exception e) {
            e.printStackTrace();
            isThemeInverted = false;
        }
        return isThemeInverted;
    }

    static void setTheme(@NonNull final Activity activity, final boolean isThemeInverted, final int accent) {
        final int theme = resolveTheme(isThemeInverted, accent);
        activity.setTheme(theme);
    }

    public static int getColorFromResource(@NonNull final Context context, final int resource, final int emergencyColor) {
        int color;
        try {
            color = ContextCompat.getColor(context, resource);
        } catch (Exception e) {
            color = ContextCompat.getColor(context, emergencyColor);
        }
        return color;
    }

    //get theme
    private static int resolveTheme(final boolean isThemeDark, final int accent) {

        int selectedTheme;

        switch (accent) {

            case R.color.red:
                selectedTheme = isThemeDark ? R.style.AppThemeRedInverted : R.style.AppThemeRed;
                break;

            case R.color.pink:
                selectedTheme = isThemeDark ? R.style.AppThemePinkInverted : R.style.AppThemePink;
                break;

            case R.color.purple:
                selectedTheme = isThemeDark ? R.style.AppThemePurpleInverted : R.style.AppThemePurple;
                break;

            case R.color.deep_purple:
                selectedTheme = isThemeDark ? R.style.AppThemeDeepPurpleInverted : R.style.AppThemeDeepPurple;
                break;

            case R.color.indigo:
                selectedTheme = isThemeDark ? R.style.AppThemeIndigoInverted : R.style.AppThemeIndigo;
                break;

            case R.color.blue:
                selectedTheme = isThemeDark ? R.style.AppThemeBlueInverted : R.style.AppThemeBlue;
                break;

            default:
            case R.color.light_blue:
                selectedTheme = isThemeDark ? R.style.AppThemeLightBlueInverted : R.style.AppThemeLightBlue;
                break;

            case R.color.cyan:
                selectedTheme = isThemeDark ? R.style.AppThemeCyanInverted : R.style.AppThemeCyan;
                break;

            case R.color.teal:
                selectedTheme = isThemeDark ? R.style.AppThemeTealInverted : R.style.AppThemeTeal;
                break;

            case R.color.green:
                selectedTheme = isThemeDark ? R.style.AppThemeGreenInverted : R.style.AppThemeGreen;
                break;

            case R.color.amber:
                selectedTheme = isThemeDark ? R.style.AppThemeAmberInverted : R.style.AppThemeAmber;
                break;

            case R.color.orange:
                selectedTheme = isThemeDark ? R.style.AppThemeOrangeInverted : R.style.AppThemeOrange;
                break;

            case R.color.deep_orange:
                selectedTheme = isThemeDark ? R.style.AppThemeDeepOrangeInverted : R.style.AppThemeDeepOrange;
                break;

            case R.color.brown:
                selectedTheme = isThemeDark ? R.style.AppThemeBrownInverted : R.style.AppThemeBrown;
                break;

            case R.color.gray:
                selectedTheme = isThemeDark ? R.style.AppThemeGrayLightInverted : R.style.AppThemeGrayLight;
                break;

            case R.color.blue_gray:
                selectedTheme = isThemeDark ? R.style.AppThemeBlueGrayInverted : R.style.AppThemeBlueGray;
                break;
        }
        return selectedTheme;
    }

    static void setThemeAccent(@NonNull final Activity activity, final int accent) {
        final SharedPreferences preferences = activity.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE);
        preferences.edit().putInt(ACCENT_VALUE, accent).apply();
        activity.recreate();
    }

    static int getAccent(@NonNull final Context context) {
        int accent;
        try {
            accent = context.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE).getInt(ACCENT_VALUE, R.color.blue);
        } catch (Exception e) {
            e.printStackTrace();
            accent = R.color.blue;
            //if resource is not found, it means the developer changed resources names
            //when updating the app. This way, we will fix the preference
            final SharedPreferences preferences = context.getSharedPreferences(ACCENT_PREF, Context.MODE_PRIVATE);
            preferences.edit().putInt(ACCENT_VALUE, accent).apply();
        }
        return accent;
    }

    static void hideSearchToolbar(@NonNull final Activity activity, @NonNull final Toolbar toolbar, @NonNull final ImageView searchPrefButton, final boolean isThemeInverted) {
        final boolean isVisible = isSearchBarVisible(activity);
        toolbar.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        searchPrefButton.setColorFilter(isVisible ? isThemeInverted ? Color.WHITE : Color.BLACK : Color.GRAY, PorterDuff.Mode.SRC_IN);
        final boolean newVisibility = !isVisible;
        final SharedPreferences preferences = activity.getSharedPreferences(SEARCH_BAR_PREF, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(SEARCH_BAR_VALUE, newVisibility).apply();
    }

    static boolean isSearchBarVisible(@NonNull final Context context) {
        boolean isSearchBarHidden;
        try {
            isSearchBarHidden = context.getSharedPreferences(SEARCH_BAR_PREF, Context.MODE_PRIVATE).getBoolean(SEARCH_BAR_VALUE, true);
        } catch (Exception e) {
            e.printStackTrace();
            isSearchBarHidden = false;
        }
        return isSearchBarHidden;
    }

    static void updateTextView(@NonNull final TextView textView, @NonNull final String text) {
        textView.post(() -> textView.setText(text));
    }

    static void setupSearch(@NonNull final SearchView searchView, @NonNull final ArtistsAdapter artistsAdapter, @NonNull final List<Artist> artists) {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                processQuery(newText, artistsAdapter, artists);
                return false;
            }
        });
    }

    private static void processQuery(@NonNull final String query, @NonNull final ArtistsAdapter artistsAdapter, @NonNull final List<Artist> artists) {
        // in real app you'd have it instantiated just once
        final List<Artist> result = new ArrayList<>();

        try {
            // case insensitive search
            for (Artist artist : artists) {
                if (artist.getName().toLowerCase().startsWith(query.toLowerCase())) {
                    result.add(artist);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result.size() > 0) {
            artistsAdapter.setArtists(result);
        }
    }
}
