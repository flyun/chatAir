package io.noties.markwon.utils;

/**
 * Created by flyun on 2023/8/1.
 */
public class JavaUtils {

    public static StringBuilder join(CharSequence delimiter, CharSequence... elements) {
        if (null == delimiter || null == elements) throw new NullPointerException();

        StringBuilder sb = new StringBuilder(String.valueOf(elements[0]));
        for (int i = 1; i < elements.length; i++) sb.append(delimiter).append(elements[i]);

        return sb;
    }

}
