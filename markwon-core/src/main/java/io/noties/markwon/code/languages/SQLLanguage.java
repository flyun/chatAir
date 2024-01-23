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
public class SQLLanguage extends Language {

   private static final Pattern PATTERN_BUILTINS = Pattern.compile("\\b(SELECT|FROM|WHERE|GROUP BY|ORDER BY|HAVING|COUNT|SUM|AVG|MIN|MAX|AS|ON|INNER|LEFT|RIGHT|JOIN|UNION|INTERSECT|EXCEPT|DISTINCT)\\b", Pattern.CASE_INSENSITIVE);
   private static final Pattern PATTERN_SINGLE_LINE_COMMENT = Pattern.compile("--[^\\n]*");
   private static final Pattern PATTERN_MULTI_LINE_COMMENT = Pattern.compile("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/");
   private static final Pattern PATTERN_STRING = Pattern.compile("'(.*?)'");
   private static final Pattern PATTERN_NUMBER = Pattern.compile("\\b\\d+\\b");

   public static String getCommentStart() {
      return "--";
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
         case SINGLE_LINE_COMMENT:
            return PATTERN_SINGLE_LINE_COMMENT;
         case MULTI_LINE_COMMENT:
            return PATTERN_MULTI_LINE_COMMENT;
         case STRING:
            return PATTERN_STRING;
         case NUMBER:
            return PATTERN_NUMBER;
         default:
            return null;
      }
   }

   @Override
   public String[] getKeywords() {
      return new String[] {
              "SELECT", "FROM", "WHERE", "GROUP BY", "ORDER BY", "HAVING", "COUNT", "SUM", "AVG", "MIN", "MAX", "AS", "ON", "INNER", "LEFT", "RIGHT", "JOIN", "UNION", "INTERSECT", "EXCEPT", "DISTINCT"
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
      return "sql";
   }

   @Override
   public Set<Character> getIndentationStarts() {
      return new HashSet<>();
   }

   @Override
   public Set<Character> getIndentationEnds() {
      return new HashSet<>();
   }
}

