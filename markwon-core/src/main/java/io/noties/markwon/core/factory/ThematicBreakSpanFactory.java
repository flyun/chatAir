package io.noties.markwon.core.factory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.RenderProps;
import io.noties.markwon.SpanFactory;
import io.noties.markwon.core.spans.ThematicBreakSpan;

public class ThematicBreakSpanFactory implements SpanFactory {
    @Nullable
    @Override
    public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps props) {
        return new ThematicBreakSpan(configuration.theme(), props);
    }
}
