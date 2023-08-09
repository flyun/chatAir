package io.noties.markwon.core.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;

import androidx.annotation.NonNull;
import io.noties.markwon.RenderProps;
import io.noties.markwon.core.CoreProps;
import io.noties.markwon.core.MarkwonTheme;

public class ThematicBreakSpan implements LeadingMarginSpan {

    private final MarkwonTheme theme;
    private final Rect rect = ObjectsPool.rect();
    private final Paint paint = ObjectsPool.paint();
    private final RenderProps props;

    public ThematicBreakSpan(@NonNull MarkwonTheme theme, @NonNull RenderProps props) {
        this.theme = theme;
        this.props = props;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return 0;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {

        final int middle = top + ((bottom - top) / 2);

        paint.set(p);
        theme.applyThematicBreakStyle(paint);

        final int height = (int) (paint.getStrokeWidth() + .5F);
        final int halfHeight = (int) (height / 2.F + .5F);

        final int left;
        final int right;
        final int maxWidth = props != null ? CoreProps.MAX_WIDTH_SIZE.require(props) : 0;
        if (dir > 0) {
            left = x;
            right = maxWidth != 0 ? maxWidth : c.getWidth();
        } else {
            left = maxWidth != 0 ? x - maxWidth : x - c.getWidth();
            right = x;
        }

        rect.set(left, middle - halfHeight, right, middle + halfHeight);
        c.drawRect(rect, paint);
    }
}
