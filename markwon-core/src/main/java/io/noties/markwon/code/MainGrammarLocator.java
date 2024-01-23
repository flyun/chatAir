package io.noties.markwon.code;

/**
 * @author opyale
 */
public class MainGrammarLocator {

    public static final String DEFAULT_FALLBACK_LANGUAGE = null; // "clike";

    public static String fromExtension(String extension) {

        switch (extension.toLowerCase()) {
            case "b":
            case "bf":
                return "brainfuck";

            case "c":
            case "h":
            case "hdl":
                return "c";

            case "clj":
            case "cljs":
            case "cljc":
            case "edn":
                return "clojure";

            case "cc":
            case "cpp":
            case "cxx":
            case "c++":
            case "hh":
            case "hpp":
            case "hxx":
            case "h++":
                return "cpp";

            case "c#":
            case "cs":
            case "csx":
                return "csharp";

            case "groovy":
            case "gradle":
            case "gvy":
            case "gy":
            case "gsh":
                return "groovy";

            case "js":
            case "cjs":
            case "mjs":
                return "javascript";

            case "kt":
            case "kts":
            case "ktm":
                return "kotlin";

            case "md":
                return "markdown";

            // case "xml":
            // case "html":
            // case "htm":
            case "mathml":
            case "svg":
                return "markup";

            case "objective-c":
                return "objectiveC";
            case "swift":
                return "swift";

            case "py":
            case "pyi":
            case "pyc":
            case "pyd":
            case "pyo":
            case "pyw":
            case "pyz":
                return "python";

            case "scala":
            case "sc":
                return "scala";

            case "sql":
                return "sql";

            case "json":
                return "json";

            case "el":
            case "lisp":
                return "lisp";

            case "yaml":
            case "yml":
            case "properties": // This extension doesn't correspond to YAML, but it's the next best
                // option
                return "yaml";
        }

        return extension;
    }
}

