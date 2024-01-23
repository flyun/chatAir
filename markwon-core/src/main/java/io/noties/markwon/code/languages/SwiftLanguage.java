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
 * Coded by GPT4
 * Created by flyun on 2024/1/23.
 */
public class SwiftLanguage extends Language {

    private static final Pattern PATTERN_BUILTINS = Pattern.compile("[,:;[->]{}()]");
    private static final Pattern PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern PATTERN_MULTI_LINE_COMMENT = Pattern.compile("/\\*[^*]*\\*+" +
            "(?:[^/*][^*]*\\*+)*/");
    private static final Pattern PATTERN_ATTRIBUTE = Pattern.compile("\\.[a-zA-Z0-9_]+");
    private static final Pattern PATTERN_OPERATION = Pattern.compile(
            "\\+|-|\\*|/|<|>|=|!|&|\\||\\^|%|~");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("\\b(\\d*[.]?\\d+)\\b");
    private static final Pattern PATTERN_STRING = Pattern.compile("[\"](.*?)[\"]");

    @Override
    public Pattern getPattern(LanguageElement element) {
        switch (element) {
            case KEYWORD:
                return Pattern.compile("\\b(" + JavaUtils.join("|", getKeywords()) + ")\\b");
            case BUILTIN:
                return PATTERN_BUILTINS;
            case NUMBER:
                return PATTERN_NUMBERS;
            case STRING:
                return PATTERN_STRING;
            case SINGLE_LINE_COMMENT:
                return PATTERN_SINGLE_LINE_COMMENT;
            case MULTI_LINE_COMMENT:
                return PATTERN_MULTI_LINE_COMMENT;
            case ATTRIBUTE:
                return PATTERN_ATTRIBUTE;
            case OPERATION:
                return PATTERN_OPERATION;
            default:
                return null;
        }
    }

    @Override
    public String[] getKeywords() {
        return new String[]{
                "class", "deinit", "enum", "extension", "func", "import", "init", "inout", "let",
                "operator",
                "private", "protocol", "public", "static", "struct", "subscript", "typealias",
                "var", "break",
                "case", "continue", "default", "do", "else", "fallthrough", "if", "in", "for",
                "return", "switch",
                "where", "while", "as", "dynamicType", "is", "new", "super", "self", "Self",
                "Type", "__COLUMN__",
                "__FILE__", "__FUNCTION__", "__LINE__", "associativity", "didSet", "get", "infix"
                , "inout", "left",
                "mutating", "none", "nonmutating", "optional", "override", "postfix", "precedence"
                , "prefix", "Protocol",
                "required", "right", "set", "Type", "unowned", "weak", "willSet"
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
        return "swift";
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

    public static String getCommentStart() {
        return "//";
    }

    public static String getCommentEnd() {
        return "";
    }
}

