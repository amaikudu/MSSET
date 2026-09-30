import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class CodeCleaner {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java CodeCleaner <folder> [more folders...]");
            return;
        }

        List<Path> files = new ArrayList<>();
        for (String arg : args) {
            Path root = Paths.get(arg);
            if (!Files.isDirectory(root)) {
                System.out.println("[SKIP] not a folder: " + arg);
                continue;
            }
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(files::add);
            }
        }
        files.sort(Comparator.naturalOrder());

        int totalRemoved = 0;
        int changedFiles = 0;

        for (Path file : files) {
            String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Result result = strip(source);
            if (result.removed == 0) {
                System.out.println("[keep] " + file);
                continue;
            }
            String cleaned = tidy(result.text);
            Files.write(file, cleaned.getBytes(StandardCharsets.UTF_8));
            totalRemoved += result.removed;
            changedFiles++;
            System.out.println("[done] " + file + " (" + result.removed + " comment(s) removed)");
        }

        System.out.println();
        System.out.println("Comments removed : " + totalRemoved);
        System.out.println("Files changed    : " + changedFiles + " of " + files.size());
    }

    private static class Result {
        String text;
        int removed;
    }

    private static Result strip(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int removed = 0;
        int i = 0;
        int length = source.length();

        while (i < length) {
            char c = source.charAt(i);

            if (c == '"') {
                out.append(c);
                i++;
                while (i < length) {
                    char s = source.charAt(i);
                    out.append(s);
                    i++;
                    if (s == '\\' && i < length) {
                        out.append(source.charAt(i));
                        i++;
                    } else if (s == '"') {
                        break;
                    }
                }
                continue;
            }

            if (c == '\'') {
                out.append(c);
                i++;
                while (i < length) {
                    char s = source.charAt(i);
                    out.append(s);
                    i++;
                    if (s == '\\' && i < length) {
                        out.append(source.charAt(i));
                        i++;
                    } else if (s == '\'') {
                        break;
                    }
                }
                continue;
            }

            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '/') {
                while (i < length && source.charAt(i) != '\n') {
                    i++;
                }
                removed++;
                continue;
            }

            if (c == '/' && i + 1 < length && source.charAt(i + 1) == '*') {
                i += 2;
                while (i < length) {
                    if (source.charAt(i) == '*' && i + 1 < length && source.charAt(i + 1) == '/') {
                        i += 2;
                        break;
                    }
                    i++;
                }
                removed++;
                continue;
            }

            out.append(c);
            i++;
        }

        Result result = new Result();
        result.text = out.toString();
        result.removed = removed;
        return result;
    }

    private static String tidy(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder();
        int blankRun = 0;
        for (String line : lines) {
            String trimmed = stripTrailing(line);
            if (trimmed.trim().isEmpty()) {
                blankRun++;
                if (blankRun > 1) {
                    continue;
                }
            } else {
                blankRun = 0;
            }
            out.append(trimmed).append('\n');
        }
        String result = out.toString();
        while (result.startsWith("\n")) {
            result = result.substring(1);
        }
        while (result.endsWith("\n\n")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static String stripTrailing(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }
}
