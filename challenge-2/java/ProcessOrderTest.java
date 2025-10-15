import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessOrderTest {

    // Mock Database per i test
    static class MockDatabase implements ProcessOrder.Database {
        List<ProcessOrder.OrderRecord> orders = new ArrayList<>();
        boolean failNext = false;

        void failNext() {
            failNext = true;
        }

        @Override
        public void saveOrder(ProcessOrder.OrderRecord order) throws Exception {
            if (failNext) {
                failNext = false;
                throw new Exception("[Test] database save failure");
            }
            orders.add(order);
        }
    }

    // Mock EmailService per i test
    static class MockEmailService implements ProcessOrder.EmailService {
        record SentEmail(String to, String subject, String body) {
        }

        List<SentEmail> sentEmails = new ArrayList<>();
        boolean failNext = false;

        void failNext() {
            failNext = true;
        }

        @Override
        public void send(String to, String subject, String body) throws Exception {
            if (failNext) {
                failNext = false;
                throw new Exception("[Test] email send failure");
            }
            sentEmails.add(new SentEmail(to, subject, body));
        }
    }

    @Nested
    @DisplayName("Test: la validazione deve funzionare")
    class TestOrderValidation {
        MockDatabase mockDatabase = new MockDatabase();
        MockEmailService mockEmail = new MockEmailService();

        @Test
        @DisplayName("Ordine senza articoli")
        void testOrderWithoutItems() {
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario",
                    "mario@test.com",
                    false,
                    List.of()
            );

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNull(result, "Ordine vuoto dovrebbe essere rifiutato");
        }

        @Test
        @DisplayName("Ordine senza email")
        void testOrderWithoutEmail() {
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario",
                    null,
                    false,
                    List.of(new ProcessOrder.Item("Prodotto", 10, 1))
            );

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNull(result, "Ordine senza email dovrebbe essere rifiutato");
        }
    }

    @Nested
    @DisplayName("Test: il calcolo del totale deve essere corretto")
    class TestOrderCalculation {
        MockDatabase mockDatabase = new MockDatabase();
        MockEmailService mockEmail = new MockEmailService();

        @Test
        @DisplayName("Il totale deve essere calcolato correttamente")
        void testBasicOrder() {
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario",
                    "mario@test.com",
                    false,
                    List.of(
                            new ProcessOrder.Item("Prodotto A", 100, 2),  // 200
                            new ProcessOrder.Item("Prodotto B", 50, 1)    // 50
                    )
            );
            // Subtotale: 250, nessuno sconto, tasse 22%
            // Totale: 250 * 1.22 = 305

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNotNull(result, "Ordine valido dovrebbe essere accettato");
            assertEquals(250.0, result.subtotal(), 0.01,
                    String.format("Subtotale dovrebbe essere 250, è %f", result.subtotal()));
            assertEquals(305.0, result.total(), 0.01,
                    String.format("Totale dovrebbe essere 305, è %f", result.total()));
        }

        @Test
        @DisplayName("Lo sconto VIP deve essere applicato correttamente")
        void testVipDiscount() {
            // Ordine VIP di €100
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario VIP",
                    "mario@test.com",
                    true,
                    List.of(new ProcessOrder.Item("Prodotto", 100, 1))
            );
            // Subtotale: 100, sconto VIP 15% = 85, tasse 22% = 103.7

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNotNull(result);
            assertTrue(result.vip());
            assertEquals(85.0, result.subtotal(), 0.01,
                    String.format("Subtotale VIP dovrebbe essere 85, è %f", result.subtotal()));
        }

        @Test
        @DisplayName("Lo sconto per ordini grandi deve essere applicato")
        void testLargeOrderDiscount() {
            // Ordine di €600 (senza VIP)
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario Grosso",
                    "mario@test.com",
                    false,
                    List.of(new ProcessOrder.Item("Prodotto Caro", 600, 1))
            );
            // Subtotale: 600, sconto grandi ordini 10% = 540, tasse 22% = 658.8

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNotNull(result);
            assertEquals(540.0, result.subtotal(), 0.01,
                    String.format("Subtotale con sconto dovrebbe essere 540, è %f", result.subtotal()));
        }
    }

    @Nested
    @DisplayName("Test: l'ordine deve essere salvato nel database")
    class TestDatabaseStorage {
        MockEmailService mockEmail = new MockEmailService();

        @Test
        @DisplayName("L'ordine viene salvato correttamente")
        void testSaveOrder() {
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario",
                    "mario@test.com",
                    false,
                    List.of(new ProcessOrder.Item("Prodotto", 50, 1))
            );

            MockDatabase mockDatabase = new MockDatabase();

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNotNull(result);
            assertEquals(1, mockDatabase.orders.size(), "Ordine dovrebbe essere salvato nel database");

            ProcessOrder.OrderRecord saved = mockDatabase.orders.getFirst();
            assertEquals("Mario", saved.customerName());
            assertEquals("mario@test.com", saved.customerEmail());
            assertEquals("pending", saved.status());
            assertEquals(50.0, saved.subtotal(), 0.01);
            assertEquals(11.0, saved.tax(), 0.01);
            assertEquals(61.0, saved.total(), 0.01);
            assertFalse(saved.vip());
        }

        @Test
        @DisplayName("Se l'ordine non viene salvato, la funzione non ritorna niente")
        void testSaveOrderError() {
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario",
                    "mario@test.com",
                    false,
                    List.of(new ProcessOrder.Item("Prodotto", 50, 1))
            );

            MockDatabase mockDatabase = new MockDatabase();
            mockDatabase.failNext();

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNull(result);
            assertEquals(0, mockDatabase.orders.size(), "Ordine non dovrebbe essere salvato nel database");
        }
    }

    @Nested
    @DisplayName("Test: Dopo l'ordine deve essere inviata un'email di conferma")
    class TestEmailSending {
        MockDatabase mockDatabase = new MockDatabase();

        @Test
        @DisplayName("L'email di conferma viene inviata correttamente")
        void testSendEmail() {
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario",
                    "mario@test.com",
                    false,
                    List.of(new ProcessOrder.Item("Prodotto", 50, 1))
            );

            MockEmailService mockEmail = new MockEmailService();
            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);

            assertNotNull(result);
            assertEquals(1, mockEmail.sentEmails.size(), "Email dovrebbe essere stata inviata");

            MockEmailService.SentEmail email = mockEmail.sentEmails.getFirst();
            assertEquals("mario@test.com", email.to());
            assertEquals(String.format("Ordine confermato - €%.2f", 61.0), email.subject());

            String expectedBody = String.format("""
                    
                    Grazie Mario!
                    
                    Il tuo ordine è stato confermato.
                    Totale: €%.2f
                    
                    Dettagli:
                    - Prodotto x1: €%.2f
                    """, 61.0, 50.0);
            assertEquals(expectedBody, email.body());
        }

        @Test
        @DisplayName("Se il servizio email ritorna un errore, l'ordine risulta comunque accettato")
        void testSendEmailError() {
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario",
                    "mario@test.com",
                    false,
                    List.of(new ProcessOrder.Item("Prodotto", 50, 1))
            );

            MockEmailService mockEmail = new MockEmailService();
            mockEmail.failNext();

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNotNull(result, "Errore di invio email non deve essere bloccante");
            assertEquals(0, mockEmail.sentEmails.size(), "Email non dovrebbe essere stata inviata");
        }
    }

    @Nested
    @DisplayName("Test: L'ordine deve essere loggato")
    class TestLogging {
        MockDatabase mockDatabase = new MockDatabase();
        MockEmailService mockEmail = new MockEmailService();

        @Test
        @DisplayName("L'ordine viene loggato correttamente")
        void testOrderLogged() throws IOException {
            ProcessOrder.OrderData order = new ProcessOrder.OrderData(
                    "Mario",
                    "mario@test.com",
                    false,
                    List.of(new ProcessOrder.Item("Prodotto", 50, 1))
            );

            // Vero che ci starebbe bene una costante qui?
            Path logsPath = Paths.get("orders.log");
            // Tronchiamo il file di log...
            Files.writeString(logsPath, "");

            ProcessOrder.OrderRecord result = ProcessOrder.processOrder(order, mockDatabase, mockEmail);
            assertNotNull(result);

            String logged = Files.readString(logsPath);
            assertEquals(String.format("[ORDINE] Mario - €%.2f - VIP: false\n", 61.0), logged,
                    "Ordine dovrebbe essere loggato");
        }
    }

    private static final List<String> EXPECTED_HELPERS = List.of(
            "validateOrder",
            "calculateTotals",
            "saveOrder",
            "sendConfirmation",
            "logOrder"
    );

    @Nested
    @DisplayName("Test che verificano se hai riorganizzato correttamente il codice")
    class TestCodeQuality {
        private static final Path TEST_SUBJECT_PATH = Paths.get("challenge-2/java/ProcessOrder.java");

        @Test
        @DisplayName("Verifica che il file contenga delle funzioni helper per incapsulare ciascuna responsabilità")
        void testExpectedHelpersExist() {
            /*
             * Questo test cerca delle funzioni con nomi scelti arbitrariamente,
             * niente affatto obbligatori!
             * Se hai usato nomi diversi, modifica la costante EXPECTED_HELPERS
             * nel file di test per far passare questo test.
             */
            List<String> missingHelpers = EXPECTED_HELPERS.stream()
                    .filter(helper -> getMethodByName(ProcessOrder.class, helper) == null)
                    .toList();

            assertTrue(missingHelpers.isEmpty(),
                    "⚠️  Attenzione! Mancano alcune funzioni attese: " + missingHelpers +
                            ". Spezza `processOrder` in funzioni con responsabilità singola.");
        }


        @Test
        @DisplayName("Verifica che la funzione `processOrder` esista ancora")
        void testProcessOrderExists() {
            Method processOrderMethod = getMethodByName(ProcessOrder.class, "processOrder");

            assertNotNull(processOrderMethod,
                    "❌  Non hai più la funzione `ProcessOrder`. " +
                            "Dovrebbe restare come coordinatore che chiama le funzioni più piccole.");
        }

        @Test
        @DisplayName("Verifica che la firma della funzione `processOrder` non sia cambiata")
        void testProcessOrderSignature() {
            Method processOrderMethod = getMethodByName(ProcessOrder.class, "processOrder");
            if (processOrderMethod == null) {
                return;
            }

            List<? extends Class<?>> paramTypes = Stream.of(processOrderMethod.getParameters())
                    .map(Parameter::getType)
                    .toList();
            List<? extends Class<?>> expected = List.of(
                    ProcessOrder.OrderData.class,
                    ProcessOrder.Database.class,
                    ProcessOrder.EmailService.class
            );
            assertEquals(expected, paramTypes,
                    String.format("❌  Parametri errati in `processOrder`: attesi %s, trovati %s", expected, paramTypes));
        }

        @Test
        @DisplayName("Verifica che le funzioni helper aggiunte siano documentate")
        void testHelpersHaveDocstrings() {
            /*
             * Verifica che le funzioni "helper" aggiunte siano documentate.
             */
            String source = getClassSource(TEST_SUBJECT_PATH);

            for (String funcName : EXPECTED_HELPERS) {
                if (getMethodByName(ProcessOrder.class, funcName) != null) {
                    // Construct a regular expression to check for JavaDoc comment before the method definition
                    Pattern methodPattern = Pattern.compile(
                            "/\\*\\*.*?\\*/\\s*((public|private|protected)\\s+)?(static\\s+)?\\S+\\s+" +
                                    Pattern.quote(funcName) + "\\s*\\(",
                            Pattern.DOTALL
                    );

                    assertTrue(methodPattern.matcher(source).find(),
                            "⚠️  Aggiungi una docstring JavaDoc a `" + funcName +
                                    "` per descriverne la responsabilità.");
                }
            }
        }

        @Test
        @DisplayName("Verifica che ciascuna funzione helper sia usata nella funzione `processOrder`")
        void testHelpersCalled() {
            String processOrderSource = getMethodSource(TEST_SUBJECT_PATH, "processOrder");
            if (processOrderSource == null) {
                return;
            }

            List<String> missingCalls = EXPECTED_HELPERS.stream()
                    .filter(name -> getMethodByName(ProcessOrder.class, name) != null
                            && !processOrderSource.contains(name))
                    .toList();

            assertTrue(missingCalls.isEmpty(),
                    "⚠️  Alcune funzioni non sono richiamate da `processOrder`: " + missingCalls);
        }
    }

    // HELPER: Funzioni di utilità per estrarre il codice del programma

    /**
     * Cerca un metodo per nome tra i metodi dichiarati da una classe.
     *
     * @param clazz      La classe in cui cercare il metodo
     * @param methodName Il nome del metodo da cercare
     * @return Il metodo, o {@code null} se non trovato
     */
    private static Method getMethodByName(Class<?> clazz, String methodName) {
        Method[] declaredMethods = clazz.getDeclaredMethods();
        return Stream.of(declaredMethods)
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Estrae il codice sorgente della classe dal file.
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

            // Costruisce regex per trovare la classe
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
     * Estrae il codice sorgente di un metodo dal file.
     */
    private static String getMethodSource(Path sourceFile, String methodName) {
        try {
            String content = getClassSource(sourceFile);

            // Costruisce regex per trovare il metodo
            Pattern methodPattern = Pattern.compile(
                    "\\b" + Pattern.quote(methodName) + "\\s*\\(",
                    Pattern.MULTILINE
            );
            Matcher methodMatcher = methodPattern.matcher(content);
            if (!methodMatcher.find()) {
                return null;
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
     * Estrae un blocco di codice con parentesi graffe bilanciate.
     */
    private static String extractBody(String content, int startIndex) {
        // Trova la parentesi graffa aperta '{'
        int braceIndex = content.indexOf('{', startIndex);
        if (braceIndex == -1) {
            throw new IllegalArgumentException();
        }

        // Bilancia le parentesi graffe per trovare la fine
        int braceCount = 0;
        int endIndex = braceIndex;

        for (int i = braceIndex; i < content.length(); i++) {
            char currentChar = content.charAt(i);

            // Salta stringhe e commenti
            if (currentChar == '"' || currentChar == '\'') {
                i++;
                while (i < content.length() && content.charAt(i) != currentChar) {
                    if (content.charAt(i) == '\\') i++; // Salta caratteri escaped
                    i++;
                }
                continue;
            }

            if (currentChar == '/') {
                int nextIndex = i + 1;
                if (nextIndex < content.length() && content.charAt(nextIndex) == '/') {
                    // Commento di linea - salta fino a fine riga
                    i = content.indexOf('\n', i);
                    continue;
                }
                if (nextIndex < content.length() && content.charAt(nextIndex) == '*') {
                    // Commento a blocco - salta fino a */
                    i = content.indexOf("*/", i) + 1;
                    continue;
                }
            }

            if (currentChar == '{') {
                braceCount++;
            } else if (currentChar == '}') {
                braceCount--;
                if (braceCount == 0) {
                    // Raggiunta la fine
                    endIndex = i + 1;
                    break;
                }
            }
        }

        return content.substring(startIndex, endIndex);
    }
}
