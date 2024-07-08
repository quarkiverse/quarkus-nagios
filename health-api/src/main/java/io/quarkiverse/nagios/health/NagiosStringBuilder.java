package io.quarkiverse.nagios.health;

import java.util.function.Consumer;

public class NagiosStringBuilder {

    private final StringBuilder buffer = new StringBuilder();

    public NagiosStringBuilder write(Consumer<? super StringBuilder> writer) {
        writer.accept(write());
        return this;
    }

    public NagiosStringBuilder set(Object value) {
        return write(sb -> sb.append(value));
    }

    public StringBuilder write() {
        buffer.setLength(0);
        return buffer;
    }

    public StringBuilder asInfo() {
        return replace('|', '/', '\n', ' ', '\r', ' ');
    }

    public StringBuilder asLabel(Object value) {
        return set(value).asLabel();
    }

    public StringBuilder asLabel() {
        return replace('\'', '"', '=', ':', '\n', ' ', '\r', ' ');
    }

    public StringBuilder asUnit(Object value) {
        return set(value).asUnit();
    }

    public StringBuilder asUnit() {
        return remove(UNIT_FORBIDDEN_CHARS);
    }

    public StringBuilder asRange() {
        return remove(';', '\n', '\r');
    }

    public StringBuilder asOutputLine(Object value) {
        return set(value).asOutputLine();
    }

    public StringBuilder asOutputLine() {
        return replace('|', '/', '\n', ' ', '\r', ' ');
    }

    private StringBuilder replace(char... replacements) {
        for (var i = 0; i < buffer.length(); i++) {
            var c = buffer.charAt(i);
            for (var j = 0; j < replacements.length; j += 2) {
                if (c == replacements[j]) {
                    buffer.setCharAt(i, replacements[j + 1]);
                    break;
                }
            }
        }
        return trim();
    }

    private StringBuilder remove(char... filter) {
        var n = skipUntil(filter);
        for (var i = n + 1; i < buffer.length(); i++) {
            var c = buffer.charAt(i);
            if (!isFiltered(c, filter)) {
                buffer.setCharAt(n, c);
                n++;
            }
        }
        buffer.setLength(n);
        return trim();
    }

    private int skipUntil(char... filter) {
        for (var i = 0; i < buffer.length(); i++) {
            if (isFiltered(buffer.charAt(i), filter)) {
                return i;
            }
        }
        return buffer.length();
    }

    private StringBuilder trim() {
        var cut = 0;
        var i = -1;
        while (++i < buffer.length()) {
            if (Character.isWhitespace(buffer.charAt(i)))
                continue;
            if (cut < i) {
                buffer.delete(cut, i);
                i = cut;
            }
            cut = i + 2;
        }
        buffer.setLength(cut == 0 ? 0 : cut - 1);
        return buffer;
    }

    private static boolean isFiltered(char c, char[] filter) {
        for (var f : filter) {
            if (c == f) {
                return true;
            }
        }
        return false;
    }

    private static final char[] UNIT_FORBIDDEN_CHARS = "0123456789;'\"=\n\r ".toCharArray();
}
