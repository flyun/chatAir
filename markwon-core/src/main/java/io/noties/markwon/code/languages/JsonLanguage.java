package io.noties.markwon.code.languages;

import com.amrdeveloper.codeview.Code;
import com.amrdeveloper.codeview.Keyword;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import io.noties.markwon.utils.JavaUtils;

/**
 * @author qwerty287
 */
public class JsonLanguage extends Language {

    private static final Pattern PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern PATTERN_MULTI_LINE_COMMENT =
            Pattern.compile("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/");
    private static final Pattern PATTERN_ATTRIBUTE = Pattern.compile("\\.[a-zA-Z0-9_]+");
    private static final Pattern PATTERN_TODO_COMMENT =
            Pattern.compile("//\\s?(TODO|todo)\\s[^\n]*");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("\\b(\\d*[.]?\\d+)\\b");
    private static final Pattern PATTERN_CHAR = Pattern.compile("['](.*?)[']");
    private static final Pattern PATTERN_STRING = Pattern.compile("[\"](.*?)[\"]");

    public static String getCommentStart() {
        return "//";
    }

    public static String getCommentEnd() {
        return "";
    }

    @Override
    public Pattern getPattern(LanguageElement element) {
        switch (element) {
            case KEYWORD:
                return Pattern.compile("\\b(" + JavaUtils.join("|", getKeywords()) + ")\\b");
            case NUMBER:
                return PATTERN_NUMBERS;
            case CHAR:
                return PATTERN_CHAR;
            case STRING:
                return PATTERN_STRING;
            case SINGLE_LINE_COMMENT:
                return PATTERN_SINGLE_LINE_COMMENT;
            case MULTI_LINE_COMMENT:
                return PATTERN_MULTI_LINE_COMMENT;
            case ATTRIBUTE:
                return PATTERN_ATTRIBUTE;
            case TODO_COMMENT:
                return PATTERN_TODO_COMMENT;
            case BUILTIN:
            case OPERATION:
            case ANNOTATION:
            case HEX:
            case GENERIC:
            default:
                return null;
        }
    }

    @Override
    public String[] getKeywords() {
        return new String[] {"false", "true", "null"};
    }

    @Override
    public List<Code> getCodeList() {
        List<Code> codeList = new ArrayList<>();
        String[] keywords = getKeywords();
        for (String keyword : keywords) {
            codeList.add(new Keyword(keyword));
        }
        return codeList;
    }

    @Override
    public String getName() {
        return "JSON";
    }

    @Override
    public Set<Character> getIndentationStarts() {
        Set<Character> characterSet = new HashSet<>();
        characterSet.add('{');
        characterSet.add('[');
        return characterSet;
    }

    @Override
    public Set<Character> getIndentationEnds() {
        Set<Character> characterSet = new HashSet<>();
        characterSet.add('}');
        characterSet.add(']');
        return characterSet;
    }
}
