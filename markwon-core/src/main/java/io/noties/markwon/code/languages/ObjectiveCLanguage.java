package io.noties.markwon.code.languages;

/**
 * Coded by GPT4
 * Created by flyun on 2024/1/23.
 */

import com.amrdeveloper.codeview.Code;
import com.amrdeveloper.codeview.Keyword;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ObjectiveCLanguage extends Language {

    private static final Pattern PATTERN_BUILTINS = Pattern.compile("[\\[\\]()]");
    private static final Pattern PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern PATTERN_MULTI_LINE_COMMENT = Pattern.compile("/\\*[^*]*\\*+" +
            "(?:[^/*][^*]*\\*+)*/");
    private static final Pattern PATTERN_ATTRIBUTE = Pattern.compile("@\"[a-zA-Z0-9_]*\"");
    private static final Pattern PATTERN_OPERATION = Pattern.compile(
            ":|==|>|<|!=|>=|<=|-|=|>|<|%|-|-|\\+|\\-|^|&|::|\\?|\\*");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("\\b(\\d*[.]?\\d+)\\b");
    private static final Pattern PATTERN_CHAR = Pattern.compile("@'[a-zA-Z0-9]'");
    private static final Pattern PATTERN_STRING = Pattern.compile("@\".*?\"");
    private static final Pattern PATTERN_HEX = Pattern.compile("0x[0-9a-fA-F]+");

    @Override
    public Pattern getPattern(LanguageElement element) {
        switch (element) {
            case KEYWORD:
                return Pattern.compile("\\b(" + String.join("|", getKeywords()) + ")\\b");
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
                "@interface", "@end", "@implementation", "@protocol", "@class",
                "enum", "struct", "typedef", "union",
                "if", "else", "switch", "case", "default",
                "for", "while", "do", "break", "continue", "goto",
                "new", "delete", "try", "catch", "throw",
                "void", "char", "short", "int", "long", "float", "double", "signed", "unsigned",
                "const", "volatile", "extern", "static",
                "return", "@private", "@public", "@protected", "@property", "@synthesize",
                "@dynamic", "@try", "@catch", "@finally", "@throw", "@synchronized"
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
        return "objectiveC";
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

