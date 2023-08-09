package io.noties.markwon.code.languages;

import com.amrdeveloper.codeview.Code;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author qwerty287
 */
public class UnknownLanguage extends Language {

    @Override
    public Pattern getPattern(LanguageElement element) {
        return null;
    }

    @Override
    public Set<Character> getIndentationStarts() {
        return new HashSet<>();
    }

    @Override
    public Set<Character> getIndentationEnds() {
        return new HashSet<>();
    }

    @Override
    public String[] getKeywords() {
        return new String[0];
    }

    @Override
    public List<Code> getCodeList() {
        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "Unknown";
    }
}

