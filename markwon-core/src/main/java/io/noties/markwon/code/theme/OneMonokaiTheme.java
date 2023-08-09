package io.noties.markwon.code.theme;

import androidx.annotation.ColorRes;
import io.noties.markwon.R;
import io.noties.markwon.code.languages.LanguageElement;
import io.noties.markwon.core.CorePlugin;

/**
 * Created by flyun on 2023/8/5.
 */
class OneMonokaiTheme implements DefaultTheme {

    @Override
    @ColorRes
    public int getColor(LanguageElement element) {
        switch (element) {
            case HEX:
                return R.color.monokai_hex;
            case NUMBER:
                return R.color.monokai_number;
            case KEYWORD:
                return R.color.monokai_keyword;
            case OPERATION:
                return R.color.monokai_operation;
            case GENERIC:
                return R.color.monokai_generic;
            case CHAR:
                return R.color.monokai_char;
            case STRING:
                return R.color.monokai_string;
            case SINGLE_LINE_COMMENT:
            case MULTI_LINE_COMMENT:
                return CorePlugin.IS_DARK ? R.color.monokai_grey : R.color.monokai_white;
            case ATTRIBUTE:
            case TODO_COMMENT:
            case ANNOTATION:
                return R.color.monokai_attribute;
            default:
                //todo 因为代码高亮实时渲染刷新有问题，所以只能在每次重新创建时改变颜色 SyntaxHighlighter.highlight
                return !CorePlugin.IS_DARK ? R.color.monokai_grey : R.color.monokai_white;
        }
    }

    @Override
    @ColorRes
    public int getDefaultColor() {
        //与上面保持同步
        return !CorePlugin.IS_DARK ? R.color.monokai_grey : R.color.monokai_white;
    }

    @Override
    @ColorRes
    public int getBackgroundColor() {
        return R.color.monokai_background;
    }

    @Override
    public int getBackgroundDarkColor() {
        return R.color.monokai_background_dark;
    }
}