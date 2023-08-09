package io.noties.markwon.code.markwon;

/**
 * Created by flyun on 2023/8/1.
 */

import android.content.Context;
import android.os.Build;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.noties.markwon.code.MainGrammarLocator;
import io.noties.markwon.code.languages.Language;
import io.noties.markwon.code.languages.LanguageElement;
import io.noties.markwon.code.theme.DefaultTheme;
import io.noties.markwon.syntax.SyntaxHighlight;

/**
 * @author qwerty287
 */
public class SyntaxHighlighter implements SyntaxHighlight {

    private final DefaultTheme defaultTheme;
    private final Context context;
    private final String fallback;

    protected SyntaxHighlighter(Context context, @NonNull DefaultTheme DefaultTheme, @Nullable String fallback) {
        this.context = context;
        this.defaultTheme = DefaultTheme;
        this.fallback = fallback;
    }

    @NonNull public static SyntaxHighlighter create(Context context, @NonNull DefaultTheme DefaultTheme) {
        return new SyntaxHighlighter(context, DefaultTheme, null);
    }

    @NonNull public static SyntaxHighlighter create(
            Context context, @NonNull DefaultTheme DefaultTheme, @Nullable String fallback) {
        return new SyntaxHighlighter(context, DefaultTheme, fallback);
    }

    @NonNull @Override
    public CharSequence highlight(@Nullable String info, @NonNull String code) {
        if (code.isEmpty()) {
            return code;
        }

        if (info == null) {
            info = fallback;
        }

        if (info != null) {
            info = MainGrammarLocator.fromExtension(info);
        }

        Editable highlightedCode = new SpannableStringBuilder(code);

        Language l = Language.fromName(info);

        for (LanguageElement e : Objects.requireNonNull(LanguageElement.class.getEnumConstants())) {
            Pattern p = l.getPattern(e);
            if (p != null) {
                Matcher matcher = p.matcher(highlightedCode);
                while (matcher.find()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        highlightedCode.setSpan(
                                new ForegroundColorSpan(
                                        context.getResources().getColor(defaultTheme.getColor(e), null)),
                                matcher.start(),
                                matcher.end(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        highlightedCode.setSpan(
                                new ForegroundColorSpan(
                                        context.getResources().getColor(defaultTheme.getColor(e))),
                                matcher.start(),
                                matcher.end(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }
        return highlightedCode;
    }
}
