import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class PreflightCheck {

    private static int problems = 0;
    private static int filesChecked = 0;
    private static int totalLines = 0;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            args = new String[] { "src", "tools" };
        }

        List<Path> files = new ArrayList<>();
        for (String arg : args) {
            Path root = Paths.get(arg);
            if (!Files.isDirectory(root)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            }
        }
        files.sort(Comparator.naturalOrder());

        for (Path file : files) {
            check(file);
        }

        System.out.println();
        System.out.println("Files checked : " + filesChecked);
        System.out.println("Total lines   : " + totalLines);
        System.out.println("Problems      : " + problems);
        System.out.println(problems == 0
                ? "RESULT: READY TO SUBMIT"
                : "RESULT: FIX THE PROBLEMS ABOVE");
    }

    private static void check(Path file) throws IOException {
        filesChecked++;
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        String normalised = text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
        String[] lines = normalised.split("\n", -1);
        totalLines += lines.length;
        List<String> issues = new ArrayList<>();

        if (text.trim().isEmpty()) {
            issues.add("file is empty");
        }

        checkCommentsAndPlaceholders(file, lines, issues);
        checkBalance(file, text, issues);
        checkStubs(file, lines, issues);
        checkEncoding(file, text, issues);

        if (issues.isEmpty()) {
            System.out.println("[OK]   " + file + " (" + lines.length + " lines)");
        } else {
            problems += issues.size();
            System.out.println("[WARN] " + file);
            for (String issue : issues) {
                System.out.println("         - " + issue);
            }
        }
    }

    private static void checkCommentsAndPlaceholders(Path file, String[] lines, List<String> issues) {
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                issues.add("line " + (i + 1) + ": leftover comment: " + trimmed);
            }
            String upper = trimmed.toUpperCase();
            boolean commentLine = trimmed.startsWith("//") || trimmed.startsWith("/*")
                    || trimmed.startsWith("*");
            boolean marker = commentLine
                    && (upper.contains("TODO") || upper.contains("FIXME")
                        || upper.contains("XXX") || upper.contains("NOT IMPLEMENTED"));
            if (marker) {
                issues.add("line " + (i + 1) + ": unfinished marker: " + trimmed);
            }
        }
    }

    private static void checkBalance(Path file, String text, List<String> issues) {
        int braces = 0;
        int parens = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (inChar) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '\'') {
                inChar = true;
            } else if (c == '{') {
                braces++;
            } else if (c == '}') {
                braces--;
            } else if (c == '(') {
                parens++;
            } else if (c == ')') {
                parens--;
            }
        }
        if (braces != 0) {
            issues.add("unbalanced braces (" + braces + ")");
        }
        if (parens != 0) {
            issues.add("unbalanced parentheses (" + parens + ")");
        }
    }

    private static void checkStubs(Path file, String[] lines, List<String> issues) {
        List<String> blockStack = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            boolean loneReturn = trimmed.equals("return;") || trimmed.equals("return null;")
                    || trimmed.equals("return 0;") || trimmed.equals("return false;")
                    || trimmed.equals("return true;") || trimmed.equals("return \"\";");
            if (loneReturn && i + 1 < lines.length && lines[i + 1].trim().equals("}")) {
                boolean insideMethod = blockStack.size() == 1
                        && blockStack.get(0).equals("method");
                if (insideMethod) {
                    issues.add("line " + (i + 1) + ": method body is only \"" + trimmed + "\"");
                }
            }

            String opens = line;
            String closes = line;
            int openCount = count(opens, '{');
            int closeCount = count(closes, '}');

            for (int b = 0; b < openCount; b++) {
                blockStack.add(classifyBlock(trimmed));
            }
            for (int b = 0; b < closeCount; b++) {
                if (!blockStack.isEmpty()) {
                    blockStack.remove(blockStack.size() - 1);
                }
            }
        }
    }

    private static int count(String text, char c) {
        int total = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (inString) {
                if (ch == '\\') {
                    i++;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (inChar) {
                if (ch == '\\') {
                    i++;
                } else if (ch == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '\'') {
                inChar = true;
            } else if (ch == c) {
                total++;
            }
        }
        return total;
    }

    private static String classifyBlock(String trimmed) {
        if (trimmed.startsWith("if ") || trimmed.startsWith("if(")
                || trimmed.startsWith("} else") || trimmed.startsWith("else")
                || trimmed.startsWith("for ") || trimmed.startsWith("for(")
                || trimmed.startsWith("while ") || trimmed.startsWith("while(")
                || trimmed.startsWith("switch") || trimmed.startsWith("try")
                || trimmed.startsWith("catch") || trimmed.startsWith("finally")
                || trimmed.startsWith("case ") || trimmed.startsWith("default")
                || trimmed.startsWith("do ")) {
            return "control";
        }
        if (trimmed.contains(" class ") || trimmed.startsWith("class ")
                || trimmed.contains(" interface ") || trimmed.startsWith("interface ")
                || trimmed.contains(" enum ") || trimmed.startsWith("enum ")) {
            return "type";
        }
        if (trimmed.contains("(") && trimmed.endsWith("{")) {
            return "method";
        }
        return "block";
    }

    private static void checkEncoding(Path file, String text, List<String> issues) {
        int replacements = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\uFFFD') {
                replacements++;
            }
        }
        if (replacements > 0) {
            issues.add(replacements + " replacement character(s) - file may be corrupt");
        }
    }
}
