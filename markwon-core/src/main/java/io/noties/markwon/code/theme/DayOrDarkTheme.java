package io.noties.markwon.code.theme;

import androidx.annotation.ColorRes;
import io.noties.markwon.R;
import io.noties.markwon.code.languages.LanguageElement;
import io.noties.markwon.core.CorePlugin;

/**
 * Created by flyun on 2023/8/3.
 */
public class DayOrDarkTheme implements DefaultTheme {

    @Override
    @ColorRes
    public int getColor(LanguageElement element) {
        switch (element) {
            case HEX:
            case NUMBER:
            case KEYWORD:
            case OPERATION:
            case GENERIC:
                if (!CorePlugin.IS_DARK) {
                    return R.color.five_dark_purple;
                } else {
                    return R.color.five_dark_purple;
                }
            case CHAR:
            case STRING:
                if (!CorePlugin.IS_DARK) {
                    return R.color.five_yellow;
                } else {
                    return R.color.five_dark_yellow;
                }
            case SINGLE_LINE_COMMENT:
            case MULTI_LINE_COMMENT:
                if (!CorePlugin.IS_DARK) {
                    return R.color.five_dark_grey;
                } else {
                    return R.color.five_dark_grey;
                }
            case ATTRIBUTE:
            case TODO_COMMENT:
            case ANNOTATION:
                if (!CorePlugin.IS_DARK) {
                    return R.color.five_dark_blue;
                } else {
                    return R.color.five_dark_blue;
                }
            default:
                if (!CorePlugin.IS_DARK) {
                    return R.color.five_dark_black;
                } else {
                    return R.color.five_dark_white;
                }
        }
    }

    @Override
    @ColorRes
    public int getDefaultColor() {
        //暂不配置，参见MarkwonHighlighter configureTheme
        if (!CorePlugin.IS_DARK) {
            return R.color.five_dark_black;
        } else {
            return R.color.five_dark_white;
        }
    }

    @Override
    @ColorRes
    public int getBackgroundColor() {
        //暂不配置，参见MarkwonHighlighter configureTheme
        if (!CorePlugin.IS_DARK) {
            return R.color.five_background_grey;
        } else {
            return R.color.five_dark_black;
        }
    }

    @Override
    public int getBackgroundDarkColor() {
        return R.color.five_dark_black;
    }
}
