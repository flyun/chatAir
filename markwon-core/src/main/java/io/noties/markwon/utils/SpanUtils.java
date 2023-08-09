package io.noties.markwon.utils;

import android.graphics.Canvas;
import android.text.Layout;
import android.text.Spanned;
import android.widget.TextView;

import androidx.annotation.NonNull;
import io.noties.markwon.core.spans.TextLayoutSpan;
import io.noties.markwon.core.spans.TextViewSpan;

/**
 * @since 4.4.0
 */
public abstract class SpanUtils {

    //根据传入的textView获取宽度，如果没有，则获取canvas宽度
    public static int width(@NonNull Canvas canvas, @NonNull CharSequence cs) {
        // Layout
        // TextView
        // canvas

        if (cs instanceof Spanned) {
            final Spanned spanned = (Spanned) cs;

            // if we are displayed with layout information -> use it
            final Layout layout = TextLayoutSpan.layoutOf(spanned);
            if (layout != null) {
                return layout.getWidth();
            }

            // if we have TextView -> obtain width from it (exclude padding)
            final TextView textView = TextViewSpan.textViewOf(spanned);
            if (textView != null) {
                return textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
            }
        }

        // else just use canvas width
        return canvas.getWidth();
    }
}
