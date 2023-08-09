package io.noties.markwon.code.markwon;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.code.theme.DefaultTheme;
import io.noties.markwon.core.MarkwonTheme;

/**
 * @author qwerty287
 */
public class MarkwonHighlighter extends AbstractMarkwonPlugin {

    private DefaultTheme defaultTheme;
    private final Context context;
    private final String fallbackLanguage;
    public boolean isDark = false;

    public void setDark(boolean isDark) {
        this.isDark = isDark;
        if (isDark) {
            defaultTheme = DefaultTheme.FIVE_COLORS_DARK;
        } else {
            defaultTheme = DefaultTheme.FIVE_COLORS;
        }


    }

    public MarkwonHighlighter(
            Context context, @NonNull DefaultTheme DefaultTheme, @Nullable String fallbackLanguage) {
        this.defaultTheme = DefaultTheme;
        this.context = context;
        this.fallbackLanguage = fallbackLanguage;
    }

    @NonNull public static MarkwonHighlighter create(Context context, @NonNull DefaultTheme DefaultTheme) {
        return create(context, DefaultTheme, null);
    }

    @NonNull public static MarkwonHighlighter create(
            Context context, @NonNull DefaultTheme DefaultTheme, @Nullable String fallbackLanguage) {
        return new MarkwonHighlighter(context, DefaultTheme, fallbackLanguage);
    }

    @Override
    public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
        //todo 问题，只在第一次初始化，不是SyntaxHighlighter这种实时渲染，需要修改MarkwonTheme实例类

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder
                    .codeBlockTextColor(context.getResources().getColor(defaultTheme.getDefaultColor(), null))
                    .codeBlockBackgroundColor(
                            context.getResources().getColor(defaultTheme.getBackgroundColor(), null))
                    .codeBlockBackgroundDarkColor(
                            context.getResources().getColor(defaultTheme.getBackgroundDarkColor(), null))
            ;
        } else {
            builder
                    .codeBlockTextColor(context.getResources().getColor(defaultTheme.getDefaultColor()))
                    .codeBlockBackgroundColor(
                            context.getResources().getColor(defaultTheme.getBackgroundColor()))
                    .codeBlockBackgroundDarkColor(
                            context.getResources().getColor(defaultTheme.getBackgroundDarkColor()))
            ;
        }
    }

    @Override
    public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
        builder.syntaxHighlight(SyntaxHighlighter.create(context, defaultTheme, fallbackLanguage));
    }
}
