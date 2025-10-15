import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopDiscountTest {

    @Nested
    @DisplayName("Testa che il calcolo degli sconti funzioni correttamente")
    class TestDiscountCalculation {
        @Test
        @DisplayName("Cliente base senza sconto di quantità")
        void testBasicCustomerNoDiscount() {
            double result = ShopDiscount.calculateDiscount(100, "basic", 1);
            assertEquals(100, result, "Cliente basic dovrebbe pagare prezzo pieno");
        }

        @Test
        @DisplayName("Cliente premium dovrebbe avere 15% di sconto")
        void testPremiumCustomerDiscount() {
            double result = ShopDiscount.calculateDiscount(100, "premium", 1);
            assertEquals(85, result, String.format("Cliente premium: 100 * 0.85 = 85, ottenuto %f", result));
        }

        @Test
        @DisplayName("Cliente VIP dovrebbe avere 25% di sconto")
        void testVipCustomerDiscount() {
            double result = ShopDiscount.calculateDiscount(100, "vip", 1);
            assertEquals(75, result, String.format("Cliente VIP: 100 * 0.75 = 75, ottenuto %f", result));
        }

        @Test
        @DisplayName("5 prodotti = 5% di sconto aggiuntivo")
        void testQuantityDiscount5Items() {
            double result = ShopDiscount.calculateDiscount(100, "basic", 5);
            double expected = 100 * 0.95;
            assertEquals(expected, result, String.format("Sconto quantità 5: %f, ottenuto %f", expected, result));
        }

        @Test
        @DisplayName("10 prodotti = 10% di sconto aggiuntivo")
        void testQuantityDiscount10Items() {
            double result = ShopDiscount.calculateDiscount(100, "basic", 10);
            double expected = 100 * 0.9;
            assertEquals(expected, result, String.format("Sconto quantità 10: %f, ottenuto %f", expected, result));
        }

        @Test
        @DisplayName("Ordini > €100 ottengono 2% di sconto aggiuntivo")
        void testLargeOrderDiscount() {
            double result = ShopDiscount.calculateDiscount(150, "basic", 1);
            double expected = 150 * 0.98;
            assertEquals(expected, result, String.format("Sconto ordine grande: %f, ottenuto %f", expected, result));
        }
    }

    @Nested
    @DisplayName("Testa che il calcolo del totale dell'ordine funzioni")
    class TestTotalOrderCalculation {
        @Test
        @DisplayName("Ordine semplice di un cliente basic")
        void testOrderBasicCustomer() {
            final var items = List.of(new ShopDiscount.Item(50, 1));
            final var order = ShopDiscount.calculateTotalOrder(items, "basic");
            assertEquals(50, order.subtotal());
            assertEquals(Math.round(50 * 0.22 * 100.0) / 100.0, order.tax());
        }

        @Test
        @DisplayName("Cliente premium dovrebbe avere sconto")
        void testOrderPremiumCustomer() {
            final var items = List.of(new ShopDiscount.Item(100, 1));
            final var order = ShopDiscount.calculateTotalOrder(items, "premium");
            // €100 * 0.85 (premium) = €85
            // Tasse: €85 * 0.22 = €18.70
            // Totale: €85 + €18.70 = €103.70
            assertEquals(85, order.subtotal());
            double expected = Math.round((85 + 85 * 0.22) * 100.0) / 100.0;
            assertEquals(expected, order.total());
        }

        @Test
        @DisplayName("Ordini > €500 ottengono ulteriore 5% di sconto")
        void testLargeOrderGetsFinalDiscount() {
            final var items = List.of(new ShopDiscount.Item(200, 3));
            final var order = ShopDiscount.calculateTotalOrder(items, "basic");
            // Subtotale: €600
            // Tasse: €600 * 0.22 = €132
            // Totale prima sconto: €732
            // Con sconto 5%: €732 * 0.98 * 0.95 = €681.49
            assertTrue(order.discountApplied(), "Discount should be applied");
            double expected = Math.round(732 * 0.98 * 0.95 * 100.0) / 100.0;
            assertEquals(expected, order.total());
        }
    }

    @Nested
    @DisplayName("Testa le descrizioni dei tipi di cliente")
    class TestCustomerTierDescription {
        @Test
        void testBasicTierDescription() {
            String result = ShopDiscount.getCustomerTierDescription("basic");
            assertTrue(result.contains("Base"));
        }

        @Test
        void testPremiumTierDescription() {
            String result = ShopDiscount.getCustomerTierDescription("premium");
            assertTrue(result.contains("Premium"));
        }

        @Test
        void testVipTierDescription() {
            String result = ShopDiscount.getCustomerTierDescription("vip");
            assertTrue(result.contains("VIP"));
        }
    }

    @Nested
    @DisplayName("Test che verificano se hai estratto correttamente le costanti")
    class TestCodeQuality {
        private static final Path TEST_SUBJECT_PATH = Paths.get("challenge-1/java/ShopDiscount.java");

        @Test
        @DisplayName("Verifica che il file contenga costanti ben nominate")
        void testConstantsExtracted() {
            /*
             * Questo test cerca costanti UPPER_CASE nel modulo.
             * Se questo fallisce, significa che non hai ancora estratto
             * tutte le costanti dal codice.
             */
            String source = getClassSource(TEST_SUBJECT_PATH);

            // Questa espressione regolare riconosce pattern del tipo `static final CONST_CASE =`
            Pattern reConstant = Pattern.compile("static\\s+final\\s+\\S+\\s+[A-Z][A-Z0-9_]+\\s*=");
            // Usa l'espressione regolare per controllare che ci siano costanti
            boolean hasConstants = source.lines().map(reConstant::matcher).anyMatch(Matcher::find);

            assertTrue(hasConstants, "Non sembra che tu abbia estratto costanti ben nominate. " +
                    "Cerca valori come 0.85, 0.75, 100, 500, 0.22 e convertili in costanti CONST_CASE.");
        }

        @Test
        @DisplayName("Controlla che la funzione calculateDiscount non contenga numeri magici schiantati")
        void testNoMagicNumbersInDiscountFunction() {
            /*
             * (Questo è un check semplice — se vedi numeri come 0.85, 0.75,
             * significa che non li hai ancora estratti a costanti!)
             */
            String source = getMethodSource(TEST_SUBJECT_PATH, "calculateDiscount");

            // Lista di valori che DOVREBBERO essere stati estratti
            String[] magicValues = {"0.85", "0.75", "0.9", "0.95", "0.98", "10", "5", "100"};
            String[] foundMagic = Stream.of(magicValues).filter(source::contains).toArray(String[]::new);

            assertEquals(0, foundMagic.length, "⚠️  Attenzione! Trovati numeri magici: " +
                    String.join(", ", foundMagic) +
                    ". Converti questi numeri in costanti ben nominate (CONST_CASE)");
        }

        @Test
        @DisplayName("Controlla che la funzione calculateTotalOrder non contenga variabili di una sola lettera")
        void testNoBadVariableNamesInTotalOrderFunction() {
            /*
             * (Questo è un check superficiale — si accerta solo che di non trovare i nomi originari,
             * assicurati però che i nomi da te scelti siano chiari e significativi!)
             */
            String source = getMethodSource(TEST_SUBJECT_PATH, "calculateTotalOrder");
            System.out.println(source);

            // Lista di nomi che DOVREBBERO essere stati cambiati
            String[] varNames = "dipqst".split("");
            System.out.println(source.matches(".*\\bs\\b.*"));
            // Trova istanze dei nomi circondati da caratteri non-parola
            String[] foundVars = Stream.of(varNames)
                    .filter(varName -> source.matches("(?s).*\\b" + varName + "\\b.*"))
                    .toArray(String[]::new);

            assertEquals(0, foundVars.length, "⚠️  Attenzione! Trovate variabili di una lettera: " +
                    String.join(", ", foundVars) +
                    ". Rinomina queste variabili con nomi significativi");
        }
    }

    // HELPER: Funzioni di utilità per estrarre il codice del programma

    /**
     * This is a helper method to extract the source code of the class under test
     *
     * @param sourceFile {@link Path} to the class file to check
     * @return The method source code as a string, or throw a runtime exception on error
     */
    private static String getClassSource(Path sourceFile) {
        try {
            if (!Files.exists(sourceFile)) {
                sourceFile = Paths.get(System.getProperty("user.dir"), sourceFile.toString());
            }
            if (!Files.exists(sourceFile)) {
                throw new IllegalArgumentException("Source file not found: " + sourceFile);
            }

            String className = sourceFile.getFileName().toString();
            if (className.endsWith(".java")) {
                className = className.substring(0, className.length() - 5);
            }

            String content = Files.readString(sourceFile);

            // Construct a regular expression to find the class signature
            Pattern classPattern = Pattern.compile(
                    "public\\s+(abstract\\s+)?class\\s+" + Pattern.quote(className) + "\\s*\\{",
                    Pattern.MULTILINE
            );
            Matcher classMatcher = classPattern.matcher(content);
            if (!classMatcher.find()) {
                throw new IllegalArgumentException("Class not found: " + className);
            }

            try {
                int startIndex = classMatcher.start();
                return extractBody(content, startIndex).indent(-4);
            } catch (Exception e) {
                throw new IllegalArgumentException("Body not found for class: " + className);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract class source: " + e);
        }
    }

    /**
     * This is a helper method to extract the source code of a method of the class under test
     *
     * @param sourceFile {@link Path} to the class file to check
     * @param methodName Name of the method to extract
     * @return The method source code as a string, or throw a runtime exception on error
     */
    private static String getMethodSource(Path sourceFile, String methodName) {
        try {
            String content = getClassSource(sourceFile);

            // Construct a regular expression to find the method signature
            Pattern methodPattern = Pattern.compile(
                    "\\b" + Pattern.quote(methodName) + "\\s*\\(",
                    Pattern.MULTILINE
            );
            Matcher methodMatcher = methodPattern.matcher(content);
            if (!methodMatcher.find()) {
                throw new IllegalArgumentException("Method not found: " + methodName);
            }

            try {
                int startIndex = methodMatcher.start();
                return extractBody(content, startIndex).indent(-4);
            } catch (Exception e) {
                throw new IllegalArgumentException("Body not found for method: " + methodName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract method source: " + e.getMessage());
        }
    }

    /**
     * Extract a body with matching open-closed curly braces from a string.
     *
     * @param content    Source code string to extract the body from
     * @param startIndex Index from where to start. Should be the index where the block name starts.
     * @return The extracted content from the start index to the closing brace, included.
     */
    private static String extractBody(String content, int startIndex) {
        // Find open brace '{'
        int braceIndex = content.indexOf('{', startIndex);
        if (braceIndex == -1) {
            throw new IllegalArgumentException();
        }

        // Balance parentheses/braces to find the end
        int braceCount = 0;
        int endIndex = braceIndex;

        for (int i = braceIndex; i < content.length(); i++) {
            char currentChar = content.charAt(i);

            // Skip strings and comments
            if (currentChar == '"' || currentChar == '\'') {
                i++;
                while (i < content.length() && content.charAt(i) != currentChar) {
                    if (content.charAt(i) == '\\') i++; // Skip escaped chars
                    i++;
                }
                continue;
            }

            if (currentChar == '/') {
                int nextIndex = i + 1;
                if (nextIndex < content.length() && content.charAt(nextIndex) == '/') {
                    // Line comment - skip to end of line
                    i = content.indexOf('\n', i);
                    continue;
                }
                if (nextIndex < content.length() && content.charAt(nextIndex) == '*') {
                    // Block comment - skip to */
                    i = content.indexOf("*/", i) + 1;
                    continue;
                }
            }

            if (currentChar == '{') {
                braceCount++;
            } else if (currentChar == '}') {
                braceCount--;
                if (braceCount == 0) {
                    // reached the end
                    endIndex = i + 1;
                    break;
                }
            }
        }

        return content.substring(startIndex, endIndex);
    }
}
