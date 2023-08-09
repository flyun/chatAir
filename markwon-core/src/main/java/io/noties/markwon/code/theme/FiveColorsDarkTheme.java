package io.noties.markwon.code.theme;


import androidx.annotation.ColorRes;
import io.noties.markwon.R;
import io.noties.markwon.code.languages.LanguageElement;

/**
 * @author qwerty287
 * @author M M Arif
 */
public class FiveColorsDarkTheme implements DefaultTheme {

    @Override
    @ColorRes
    public int getColor(LanguageElement element) {
        switch (element) {
            case HEX:
            case NUMBER:
            case KEYWORD:
            case OPERATION:
            case GENERIC:
                return R.color.five_dark_purple;
            case CHAR:
            case STRING:
                return R.color.five_dark_yellow;
            case SINGLE_LINE_COMMENT:
            case MULTI_LINE_COMMENT:
                return R.color.five_dark_grey;
            case ATTRIBUTE:
            case TODO_COMMENT:
            case ANNOTATION:
                return R.color.five_dark_blue;
            default:
                return R.color.five_dark_white;
        }
    }

    @Override
    @ColorRes
    public int getDefaultColor() {
        return R.color.five_dark_white;
    }

    @Override
    @ColorRes
    public int getBackgroundColor() {
        return R.color.five_dark_black;
    }

    @Override
    public int getBackgroundDarkColor() {
        return R.color.five_dark_black;
    }
}

