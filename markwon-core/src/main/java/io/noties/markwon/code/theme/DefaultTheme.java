package io.noties.markwon.code.theme;

import android.content.Context;

import androidx.annotation.ColorRes;
import io.noties.markwon.code.languages.LanguageElement;

/**
 * @author qwerty287
 * @author M M Arif
 */
public interface DefaultTheme {

    FiveColorsTheme FIVE_COLORS = new FiveColorsTheme();
    FiveColorsDarkTheme FIVE_COLORS_DARK = new FiveColorsDarkTheme();
    BlueMoonTheme BLUE_MOON_THEME = new BlueMoonTheme();
    BlueMoonDarkTheme BLUE_MOON_DARK_THEME = new BlueMoonDarkTheme();
    DayOrDarkTheme DAY_OR_DARK_THEME = new DayOrDarkTheme();
    OneMonokaiTheme ONE_MONOKAI_THEME = new OneMonokaiTheme();

    static DefaultTheme getDefaultTheme(Context context) {

        return ONE_MONOKAI_THEME;
    }

    @ColorRes
    int getColor(LanguageElement element);

    @ColorRes
    int getDefaultColor();

    @ColorRes
    int getBackgroundColor();

    @ColorRes
    int getBackgroundDarkColor();
}

