package com.example.halstead;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HalsteadAnalyzer {

    private static final Set<String> SIMPLE_KEYWORD_OPERATORS = Set.of(
            "def", "val", "var", "class", "object", "return", "new",
            "extends", "with", "override", "package", "import",
            "trait", "this", "throw", "implicit", "private", "public", "protected"
    );

    private static final Set<String> LITERAL_CONSTANTS = Set.of("true", "false", "null");

    private static final Set<String> COMPOUND_KEYWORDS = Set.of(
            "if", "else", "while", "do", "for", "yield",
            "match", "case", "try", "catch", "finally"
    );

    private static final Map<String, String> BRACKET_PAIRS = Map.of(
            "(", "( )", "{", "{ }", "[", "[ ]"
    );
    private static final Map<String, String> CLOSING_TO_OPEN = Map.of(
            ")", "(", "}", "{", "]", "["
    );

    private static final String TOKEN_REGEX =
            "\"\"\".*?\"\"\"|" +                              // triple-quoted strings
                    "[sf]?\"(?:[^\"\\\\]|\\\\.)*\"|" +                 // s"...", f"...", обычные "..."
                    "raw\"(?:[^\"\\\\]|\\\\.)*\"|" +                   // raw"..."
                    ">>>=|<<=|>>=|" +                                  // тройные/двойные сдвиг+присваивание
                    "<-|->|<:|>:|<%|::|:::|" +                         // стрелки и границы типов
                    "=>|<=|>=|==|!=|&&|\\|\\||\\+\\+|--|" +
                    "\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=|" +                // составные присваивания
                    ">>>|<<|>>|" +                                     // битовые сдвиги
                    "[+\\-*/%<>=!&|^~]|" +
                    "[(){}\\[\\];,.]|" +
                    "_\\*|" +                                          // vararg expansion
                    "[a-zA-Z_][a-zA-Z0-9_]*|" +
                    "\\d+\\.\\d+|\\d+";

    private static final Pattern PATTERN = Pattern.compile(TOKEN_REGEX);


    public HalsteadResult analyze(String sourceCode) {
        String cleanCode = removeComments(sourceCode);
        List<String> tokens = tokenize(cleanCode);
        List<String> merged = mergeCompoundKeywords(tokens);

        Map<String, Integer> operators = new LinkedHashMap<>();
        Map<String, Integer> operands = new LinkedHashMap<>();
        Deque<String> bracketStack = new ArrayDeque<>();

        for (String token : merged) {
            if (token.isEmpty()) {
                continue; // токен уже учтён как часть составного оператора
            }

            if (BRACKET_PAIRS.containsKey(token)) {
                bracketStack.push(token);
                continue;
            }

            if (CLOSING_TO_OPEN.containsKey(token)) {
                String expectedOpen = CLOSING_TO_OPEN.get(token);
                if (!bracketStack.isEmpty() && bracketStack.peek().equals(expectedOpen)) {
                    bracketStack.pop();
                    operators.merge(BRACKET_PAIRS.get(expectedOpen), 1, Integer::sum);
                } else {
                    operators.merge(token, 1, Integer::sum); // несбалансированная скобка
                }
                continue;
            }

            if (isOperator(token)) {
                operators.merge(token, 1, Integer::sum);
            } else {
                operands.merge(token, 1, Integer::sum);
            }
        }

        return buildResult(operators, operands);
    }

    /**
     * Объединяет составные ключевые слова (if...else, while, do...while,
     * match...case, try...catch...finally, for...yield) в единый оператор,
     * НЕ пропуская никакие токены между ними. Всё содержимое тела блока
     * (переменные, операции, вложенные конструкции) продолжает попадать
     * в общий список токенов и корректно учитывается далее.
     */
    private List<String> mergeCompoundKeywords(List<String> tokens) {
        List<String> result = new ArrayList<>();
        Deque<String> pending = new ArrayDeque<>();
        int n = tokens.size();

        for (int i = 0; i < n; i++) {
            String t = tokens.get(i);

            switch (t) {
                case "if" -> {
                    boolean hasElse = lookAheadAtSameDepth(tokens, i, "else");
                    result.add(hasElse ? "if...else" : "if");
                    if (hasElse) pending.push("else");
                }
                case "else" -> {
                    if (!pending.isEmpty() && pending.peek().equals("else")) {
                        pending.pop();
                        result.add("");
                    } else {
                        result.add("else");
                    }
                }
                case "do" -> {
                    boolean hasWhile = lookAheadAtSameDepth(tokens, i, "while");
                    result.add(hasWhile ? "do...while" : "do");
                    if (hasWhile) pending.push("while");
                }
                case "while" -> {
                    if (!pending.isEmpty() && pending.peek().equals("while")) {
                        pending.pop();
                        result.add("");
                    } else {
                        result.add("while"); // самостоятельный while(cond){...}
                    }
                }
                case "try" -> {
                    boolean hasCatch = lookAheadAtSameDepth(tokens, i, "catch");
                    boolean hasFinally = lookAheadAtSameDepth(tokens, i, "finally");
                    if (hasCatch && hasFinally) result.add("try...catch...finally");
                    else if (hasCatch) result.add("try...catch");
                    else if (hasFinally) result.add("try...finally");
                    else result.add("try");
                    if (hasCatch) pending.push("catch");
                    if (hasFinally) pending.push("finally");
                }
                case "catch" -> {
                    if (!pending.isEmpty() && pending.peek().equals("catch")) {
                        pending.pop();
                        result.add("");
                    } else {
                        result.add("catch");
                    }
                }
                case "finally" -> {
                    if (!pending.isEmpty() && pending.peek().equals("finally")) {
                        pending.pop();
                        result.add("");
                    } else {
                        result.add("finally");
                    }
                }
                case "match" -> result.add("match...case");
                case "case" -> result.add(""); // все ветки одного match уже учтены
                case "for" -> {
                    boolean hasYield = lookAheadAtSameDepth(tokens, i, "yield");
                    result.add(hasYield ? "for...yield" : "for");
                }
                case "yield" -> result.add(""); // учтено как часть for...yield
                default -> result.add(t);
            }
        }
        return result;
    }

    /**
     * Ищет ключевое слово-партнёр (else/while/catch/finally/yield) строго
     * на том же уровне вложенности скобок, БЕЗ изменения основного индекса
     * обхода — поэтому содержимое блока никогда не выбрасывается.
     */
    private boolean lookAheadAtSameDepth(List<String> tokens, int fromIndex, String keyword) {
        int depth = 0;
        for (int j = fromIndex + 1; j < tokens.size(); j++) {
            String tok = tokens.get(j);
            if (tok.equals("{") || tok.equals("(") || tok.equals("[")) {
                depth++;
            } else if (tok.equals("}") || tok.equals(")") || tok.equals("]")) {
                depth--;
                if (depth < 0) return false; // вышли за пределы текущего блока
            } else if (depth == 0 && tok.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String removeComments(String code) {
        String noBlockComments = code.replaceAll("(?s)/\\*.*?\\*/", " ");
        return noBlockComments.replaceAll("//.*", " ");
    }

    private List<String> tokenize(String code) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = PATTERN.matcher(code);
        while (matcher.find()) {
            String token = matcher.group();
            if (token != null && !token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean isOperator(String token) {
        if (LITERAL_CONSTANTS.contains(token)) return false;
        if (SIMPLE_KEYWORD_OPERATORS.contains(token)) return true;
        if (token.contains("...")) return true;
        if (COMPOUND_KEYWORDS.contains(token)) return true;
        if (token.matches("\"\"\".*\"\"\"") || token.matches("[sf]?\".*\"") || token.matches("raw\".*\"")) {
            return false; // строковый литерал — всегда операнд
        }
        return token.matches(
                ">>>=|<<=|>>=|<-|->|<:|>:|<%|::|:::|" +
                        "=>|<=|>=|==|!=|&&|\\|\\||\\+\\+|--|" +
                        "\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^=|" +
                        ">>>|<<|>>|" +
                        "[+\\-*/%<>=!&|^~]|[;,.]|_\\*"
        );
    }

    private HalsteadResult buildResult(Map<String, Integer> ops, Map<String, Integer> nds) {
        HalsteadResult r = new HalsteadResult();

        List<TokenCount> opList = ops.entrySet().stream()
                .map(e -> new TokenCount(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getFrequency() - a.getFrequency())
                .collect(Collectors.toList());

        List<TokenCount> ndList = nds.entrySet().stream()
                .map(e -> new TokenCount(e.getKey(), e.getValue()))
                .sorted((a, b) -> b.getFrequency() - a.getFrequency())
                .collect(Collectors.toList());

        r.setOperators(opList);
        r.setOperands(ndList);

        int eta1 = opList.size();
        int eta2 = ndList.size();
        int N1 = opList.stream().mapToInt(TokenCount::getFrequency).sum();
        int N2 = ndList.stream().mapToInt(TokenCount::getFrequency).sum();

        r.setEta1(eta1);
        r.setEta2(eta2);
        r.setN1(N1);
        r.setN2(N2);

        int vocabulary = eta1 + eta2;
        int length = N1 + N2;
        double volume = length * (Math.log(vocabulary) / Math.log(2));

        r.setVocabulary(vocabulary);
        r.setLength(length);
        r.setVolume(volume);

        r.setTableRows(buildTableRows(opList, ndList));
        return r;
    }

    private List<TableRow> buildTableRows(List<TokenCount> ops, List<TokenCount> nds) {
        List<TableRow> rows = new ArrayList<>();
        int maxRows = Math.max(ops.size(), nds.size());
        for (int idx = 0; idx < maxRows; idx++) {
            boolean hasOp = idx < ops.size();
            boolean hasNd = idx < nds.size();
            rows.add(new TableRow(
                    hasOp ? idx + 1 : 0,
                    hasOp ? ops.get(idx).getToken() : "",
                    hasOp ? ops.get(idx).getFrequency() : 0,
                    hasNd ? idx + 1 : 0,
                    hasNd ? nds.get(idx).getToken() : "",
                    hasNd ? nds.get(idx).getFrequency() : 0
            ));
        }
        return rows;
    }
}