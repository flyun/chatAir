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
 * @author M M Arif
 */
public class HtmlLanguage extends Language {

    // Brackets and Colons
    private static final Pattern PATTERN_BUILTINS = Pattern.compile("[,:;[->]{}()]");

    // Data
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("\\b(\\d*[.]?\\d+)\\b");
    private static final Pattern PATTERN_CHAR = Pattern.compile("['](.*?)[']");
    private static final Pattern PATTERN_STRING = Pattern.compile("[\"](.*?)[\"]");
    private static final Pattern PATTERN_HEX = Pattern.compile("0x[0-9a-fA-F]+");
    private static final Pattern PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("<!--.*-->");
    private static final Pattern PATTERN_ATTRIBUTE = Pattern.compile("\\.[a-zA-Z0-9_]+");
    private static final Pattern PATTERN_OPERATION =
            Pattern.compile(
                    ":|==|>|<|!=|>=|<=|->|=|>|<|%|-|-=|%=|\\+|\\-|\\-=|\\+=|\\^|\\&|\\|::|\\?|\\*");

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
            case BUILTIN:
                return PATTERN_BUILTINS;
            case NUMBER:
                return PATTERN_NUMBERS;
            case CHAR:
                return PATTERN_CHAR;
            case STRING:
                return PATTERN_STRING;
            case HEX:
                return PATTERN_HEX;
            case SINGLE_LINE_COMMENT:
            case MULTI_LINE_COMMENT:
                return PATTERN_SINGLE_LINE_COMMENT;
            case ATTRIBUTE:
                return PATTERN_ATTRIBUTE;
            case OPERATION:
                return PATTERN_OPERATION;
            case GENERIC:
            case TODO_COMMENT:
            case ANNOTATION:
            default:
                return null;
        }
    }

    @Override
    public String[] getKeywords() {
        return new String[] {
                "<html",
                "<DOCTYPE",
                "<head",
                "<title",
                "<body",
                "<h1",
                "<h2",
                "<h3",
                "<h4",
                "<h5",
                "<h6",
                "<br",
                "<hr",
                "<section",
                "<header",
                "<footer",
                "<select",
                "<img",
                "<embed",
                "<iframe",
                "<style",
                "<script",
                "<div",
                "<p",
                "code",
                "strong",
                "small",
                "template",
                "form",
                "input",
                "textarea",
                "button",
                "option",
                "label",
                "fieldset",
                "legend",
                "datalist",
                "frame",
                "map",
                "area",
                "canvas",
                "picture",
                "svg",
                "audio",
                "source",
                "track",
                "video",
                "link",
                "nav",
                "ul",
                "ol",
                "li",
                "table",
                "caption",
                "th",
                "tr",
                "td",
                "thead",
                "tbody",
                "tfooter",
                "col",
                "span",
                "main",
                "article",
                "aside",
                "meta",
                "base",
                "noscript",
                "object",
                "param",
                "src",
                "href"
        };
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
        return "HTML";
    }

    @Override
    public Set<Character> getIndentationStarts() {
        Set<Character> characterSet = new HashSet<>();
        characterSet.add('{');
        return characterSet;
    }

    @Override
    public Set<Character> getIndentationEnds() {
        Set<Character> characterSet = new HashSet<>();
        characterSet.add('}');
        return characterSet;
    }
}
