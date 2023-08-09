package io.noties.markwon.core.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.NonNull;
import io.noties.markwon.RenderProps;
import io.noties.markwon.core.CorePlugin;
import io.noties.markwon.core.CoreProps;
import io.noties.markwon.core.MarkwonTheme;

/**
 * @since 3.0.0 split inline and block spans
 */
public class CodeBlockSpan extends MetricAffectingSpan implements LeadingMarginSpan {

    private final MarkwonTheme theme;
    private final Rect rect = ObjectsPool.rect();
    private final Paint paint = ObjectsPool.paint();
    private final RenderProps props;

    public CodeBlockSpan(@NonNull MarkwonTheme theme, RenderProps props) {
        this.theme = theme;
        this.props = props;
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        apply(p);
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        apply(ds);
    }

    private void apply(TextPaint p) {
        theme.applyCodeBlockTextStyle(p);
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return theme.getCodeBlockMargin();
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(CorePlugin.IS_DARK ? theme.getCodeBlockBackgroundDarkColor(p) : theme.getCodeBlockBackgroundColor(p));

        final int left;
        final int right;
        //获取每个markdown实例本身的尺寸大小
        final int maxWidth = props != null ? CoreProps.MAX_WIDTH_SIZE.require(props) : 0;
        if (dir > 0) {
            left = x;
            right = maxWidth != 0 ? maxWidth : c.getWidth();
        } else {
            left =  maxWidth != 0 ? x - maxWidth : x - c.getWidth();
            right = x;
        }
        rect.set(left, top, right, bottom);

        c.drawRect(rect, paint);
    }
}
