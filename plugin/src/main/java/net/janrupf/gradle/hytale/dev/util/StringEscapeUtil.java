package net.janrupf.gradle.hytale.dev.util;

import java.util.List;

public class StringEscapeUtil {
    private StringEscapeUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static String escapeArgListForIntelliJ(List<String> arg) {
        var builder = new StringBuilder();

        for (int i = 0; i < arg.size(); i++) {
            String s = arg.get(i);
            boolean hasSpace = s.contains(" ");
            if (hasSpace) {
                builder.append('"');
            }
            for (int j = 0; j < s.length(); j++) {
                char c = s.charAt(j);
                if (c == '"' || c == '\\') {
                    builder.append('\\');
                }
                builder.append(c);
            }
            if (hasSpace) {
                builder.append('"');
            }
            if (i < arg.size() - 1) {
                builder.append(' ');
            }
        }

        return builder.toString();
    }
}
