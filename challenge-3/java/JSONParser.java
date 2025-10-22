import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class JSONParser {
    private final String json;
    private int i;

    JSONParser(String json) {
        this.json = json;
    }

    Map<String, Object> parseMainObject() {
        skipWhitespace();
        Map<String, Object> result = parseObject();
        skipWhitespace();
        mustBeAtEnd();
        return result;
    }

    private void skipWhitespace() {
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
    }

    private void skipNextMustBe(char expected) {
        char next = json.charAt(i);
        if (next != expected) {
            throw new IllegalStateException("Expected '" + expected + "', but '" + next + "' found");
        }
        i++;
    }

    private void mustBeAtEnd() {
        if (i < json.length()) {
            throw new IllegalStateException("Expected end-of-input, but '" + json.substring(i) + "' found");
        }
    }

    private Map<String, Object> parseObject() {
        skipNextMustBe('{');
        skipWhitespace();

        Map<String, Object> object = new HashMap<>();

        loop:
        while (i < json.length()) {
            skipWhitespace();
            String key = parseString();

            skipWhitespace();
            skipNextMustBe(':');
            skipWhitespace();

            Object value = parseValue();
            object.put(key, value);

            skipWhitespace();
            char next = json.charAt(i);
            switch (next) {
                case ',':
                    i++;
                    continue;
                case '}':
                    break loop;
                default:
                    throw new IllegalStateException("Expected one of ',', '}', but '" + next + "' found");
            }
        }

        skipNextMustBe('}');
        return object;
    }

    private List<Object> parseArray() {
        skipNextMustBe('[');
        skipWhitespace();

        List<Object> array = new ArrayList<>();

        loop:
        while (i < json.length()) {
            skipWhitespace();

            Object value = parseValue();
            array.add( value);

            skipWhitespace();
            char next = json.charAt(i);
            switch (next) {
                case ',':
                    i++;
                    continue;
                case ']':
                    break loop;
                default:
                    throw new IllegalStateException("Expected one of ',', ']', but '" + next + "' found");
            }
        }

        skipNextMustBe(']');

        return array;
    }

    private Object parseValue() {
        char next = json.charAt(i);
        if (next == '{') {
            return parseObject();
        } else if (next == '[') {
            return parseArray();
        } else if (next == '"') {
            return parseString();
        } else if (Character.isDigit(next)) {
            return parseNumber();
        } else if (next == 't' || next == 'f') {
            return parseBoolean();
        } else {
            throw new IllegalStateException("Expected one of '\"', 'f', 't', or a digit, but '" + next + "' found");
        }
    }

    private String parseString() {
        skipNextMustBe('"');

        StringBuilder sb = new StringBuilder();
        while (i < json.length()) {
            char next = json.charAt(i);
            if (next == '\\') {
                i++;
                String escapeSequence = "\\" + json.charAt(i);
                sb.append(escapeSequence.translateEscapes());
            } else if (next == '"') {
                break;
            } else {
                sb.append(next);
            }

            i++;
        }

        skipNextMustBe('"');
        return sb.toString();
    }

    private Number parseNumber() {
        StringBuilder sb = new StringBuilder();
        boolean afterDot = false;
        while (i < json.length()) {
            char next = json.charAt(i);
            if (Character.isDigit(next)) {
                sb.append(next);
            } else if (next == '.') {
                if (afterDot) {
                    throw new IllegalStateException("Unexpected '.' found. Bad number literal");
                }

                afterDot = true;
                sb.append('.');
            } else {
                break;
            }

            i++;
        }

        if (afterDot) {
            return Double.parseDouble(sb.toString());
        } else {
            return Long.parseLong(sb.toString());
        }
    }

    private Boolean parseBoolean() {
        char next = json.charAt(i);
        switch (next) {
            case 'f' -> {
                i++;
                skipNextMustBe('a');
                skipNextMustBe('l');
                skipNextMustBe('s');
                skipNextMustBe('e');
                return false;
            }
            case 't' -> {
                i++;
                skipNextMustBe('r');
                skipNextMustBe('u');
                skipNextMustBe('e');
                return true;
            }
            default -> throw new IllegalStateException("Expected one of 'true' or 'false', but '" + next + "' found");
        }
    }

}
