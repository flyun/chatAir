package io.noties.markwon.code.languages;

/**
 * @author qwerty287
 */
public class TypeScriptLanguage extends JavaScriptLanguage {
    @Override
    public String[] getKeywords() {
        String[] js = super.getKeywords();
        String[] tsAdditions = {"declare", "module"};
        String[] ts = new String[js.length + tsAdditions.length];
        System.arraycopy(js, 0, ts, 0, js.length);
        System.arraycopy(tsAdditions, 0, ts, js.length, tsAdditions.length);
        return ts;
    }

    @Override
    public String getName() {
        return "TypeScript";
    }
}
