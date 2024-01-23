package io.noties.markwon.code.languages;

import com.amrdeveloper.codeview.Code;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author qwerty287
 */
public abstract class Language {
    private static HashMap<String, Language> languages = null;

    private static void initializeMap() {
        if (languages == null) {
            languages = new HashMap<>();

            Language[] languagesArray =
                    new Language[] {
                            new JavaLanguage(),
                            new PythonLanguage(),
                            new GoLanguage(),
                            new PhpLanguage(),
                            new XmlLanguage(),
                            new HtmlLanguage(),
                            new JavaScriptLanguage(),
                            new TypeScriptLanguage(),
                            new JsonLanguage(),
                            new CppLanguage(),
                            new CLanguage(),
                            new CSharpLanguage(),
                            new ObjectiveCLanguage(),
                            new SwiftLanguage(),
                            new SQLLanguage(),
                            new LispLanguage()
                    };
            for (Language l : languagesArray) {
                languages.put(l.getName().toUpperCase(), l);
            }
        }
    }

    public static Language fromName(String name) {
        initializeMap();

        return isValid(name) ? languages.get(name.toUpperCase()) : new UnknownLanguage();
    }

    public static boolean isValid(String name) {
        initializeMap();

        if (name != null) {
            return languages.containsKey(name.toUpperCase());
        } else {
            return languages.containsKey(null);
        }
    }

    public abstract Pattern getPattern(LanguageElement element);

    public abstract Set<Character> getIndentationStarts();

    public abstract Set<Character> getIndentationEnds();

    public abstract String[] getKeywords();

    public abstract List<Code> getCodeList();

    public abstract String getName();

//    public void applyTheme(Context context, CodeView codeView, Theme theme) {
//        codeView.resetSyntaxPatternList();
//        codeView.resetHighlighter();
//
//        Resources resources = context.getResources();
//
//        // View Background
//        codeView.setBackgroundColor(resources.getColor(theme.getBackgroundColor(), null));
//
//        // Syntax Colors
//        for (LanguageElement e : Objects.requireNonNull(LanguageElement.class.getEnumConstants())) {
//            Pattern p = getPattern(e);
//            if (p != null) {
//                codeView.addSyntaxPattern(p, resources.getColor(theme.getColor(e), null));
//            }
//        }
//
//        // Default Color
//        codeView.setTextColor(resources.getColor(theme.getDefaultColor(), null));
//
//        codeView.reHighlightSyntax();
//    }
}
