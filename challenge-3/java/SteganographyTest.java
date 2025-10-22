import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test suite per la libreria di steganografia
 * <p>
 * Questa suite testa l'interfaccia attuale della libreria Steganography
 * e dimostra i problemi presenti nell'API.
 */
public class SteganographyTest {

    public static final Path NON_EXISTENT = Paths.get("/nonexistent.png");
    @TempDir
    Path tempDir;

    private Path testPng;
    private Path testPngWithMetadata;
    private List<Path> multipleTestPngs;

    /**
     * Crea un'immagine PNG di test 100x100 rossa
     */
    @BeforeEach
    void testPng() throws IOException {
        testPng = tempDir.resolve("test.png");
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                img.setRGB(x, y, Color.RED.getRGB());
            }
        }
        ImageIO.write(img, "PNG", testPng.toFile());
    }

    /**
     * Crea un'immagine PNG con metadati già incorporati
     */
    @BeforeEach
    void testPngWithMetadata() throws IOException {
        testPngWithMetadata = tempDir.resolve("test_with_meta.png");
        BufferedImage imgWithMeta = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 100; x++) {
            for (int y = 0; y < 100; y++) {
                imgWithMeta.setRGB(x, y, Color.BLUE.getRGB());
            }
        }
        ImageIO.write(imgWithMeta, "PNG", testPngWithMetadata.toFile());

        // Incorpora metadati
        Map<String, Object> metadata = Map.of(
                "author", "Test Author",
                "title", "Test Image",
                "date", "2025-10-15"
        );

        Map<String, Object> ops = Map.of(
                "embed", true,
                "data", metadata,
                "overwrite", true
        );

        Steganography.procMeta(testPngWithMetadata, ops, null);
    }

    /**
     * Crea 3 immagini PNG di test per batch processing
     */
    @BeforeEach
    void multipleTestPngs() throws IOException {
        // Create multiple test PNGs
        multipleTestPngs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Path pngFile = tempDir.resolve("test_" + i + ".png");
            BufferedImage multiImg = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 100; x++) {
                for (int y = 0; y < 100; y++) {
                    multiImg.setRGB(x, y, Color.RED.getRGB());
                }
            }
            ImageIO.write(multiImg, "PNG", pngFile.toFile());
            multipleTestPngs.add(pngFile);
        }
    }

    // =========================== Test ===========================

    @Nested
    @DisplayName("Test per la funzione procMeta")
    class TestProcMeta {

        @Test
        @DisplayName("Test incorporamento metadati base")
        void testEmbedBasicMetadata() {
            Map<String, Object> metadata = Map.of(
                    "author", "John Doe",
                    "year", 2025
            );

            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", metadata),
                    null
            );

            Map<String, Object> resultMap = assertIsResultMap(result);
            assertTrue((Boolean) resultMap.get("success"));
            assertTrue(Files.exists((Path) resultMap.get("path")));
        }

        @Test
        @DisplayName("Test incorporamento con path output specificato")
        void testEmbedWithOutputPath() throws IOException {
            byte[] originalBytes = Files.readAllBytes(testPng);

            Map<String, Object> metadata = Map.of(
                    "title", "Test Image"
            );
            Path output = tempDir.resolve("embedded.png");

            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", metadata, "out", output),
                    null
            );

            assertEquals(true, result);
            assertTrue(Files.exists(output));

            byte[] currentBytes = Files.readAllBytes(testPng);
            assertArrayEquals(originalBytes, currentBytes);

            byte[] savedBytes = Files.readAllBytes(output);
            assertFalse(Arrays.equals(originalBytes, savedBytes));
        }

        @Test
        @DisplayName("Test incorporamento sovrascrivendo il file originale")
        void testEmbedWithOverwrite() throws IOException {
            byte[] originalBytes = Files.readAllBytes(testPng);

            Map<String, Object> metadata = Map.of(
                    "description", "Overwritten"
            );

            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", metadata, "overwrite", true),
                    null
            );

            Map<String, Object> resultMap = assertIsResultMap(result);
            assertTrue((Boolean) resultMap.get("success"));

            byte[] currentBytes = Files.readAllBytes(testPng);
            assertFalse(Arrays.equals(originalBytes, currentBytes));
        }

        @Test
        @DisplayName("Test estrazione metadati")
        void testExtractMetadata() {
            Object result = Steganography.procMeta(
                    testPngWithMetadata,
                    Map.of("extract", true),
                    null
            );

            assertNotNull(result);
            Map<String, Object> resultMap = assertIsResultMap(result);
            assertEquals("Test Author", resultMap.get("author"));
        }

        @Test
        @DisplayName("Test estrazione quando non ci sono metadati")
        void testExtractNonexistentMetadata() {
            var config = new Steganography.ProcMetaConfig();
            config.raiseErr = false;

            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("extract", true),
                    config
            );

            assertNull(result);
        }

        @Test
        @DisplayName("Test verifica presenza metadati")
        void testVerifyWithMetadata() {
            Object result = Steganography.procMeta(
                    testPngWithMetadata,
                    Map.of("verify", true),
                    null
            );

            assertEquals(true, result);
        }

        @Test
        @DisplayName("Test verifica quando non ci sono metadati")
        void testVerifyWithoutMetadata() {
            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("verify", true),
                    null
            );

            assertEquals(false, result);
        }

        @Test
        @DisplayName("Test aggiornamento metadati esistenti")
        void testUpdateMetadata() throws IOException {
            Path output = tempDir.resolve("updated.png");

            Map<String, Object> updateData = Map.of(
                    "author", "New Author"
            );

            Object result = Steganography.procMeta(
                    testPngWithMetadata,
                    Map.of("update", updateData, "out", output),
                    null
            );

            Map<String, Object> resultMap = assertIsResultMap(result);
            assertTrue((Boolean) resultMap.get("success"));

            // Verifica che i metadati siano stati aggiornati
            Object extracted = Steganography.procMeta(
                    output,
                    Map.of("extract", true),
                    null
            );

            Map<String, Object> extractedMap = assertIsResultMap(extracted);
            assertEquals("New Author", extractedMap.get("author"));
            assertEquals("Test Image", extractedMap.get("title")); // Altri metadati preservati
        }

        @Test
        @DisplayName("Test con file inesistente")
        void testNonexistentFile() {
            var config = new Steganography.ProcMetaConfig();
            config.raiseErr = false;

            Object result = Steganography.procMeta(
                    NON_EXISTENT,
                    Map.of("extract", true),
                    config
            );

            Map<String, Object> resultMap = assertIsResultMap(result);
            assertFalse((Boolean) resultMap.get("success"));
            assertEquals("File not found", resultMap.get("error"));
        }

        @Test
        @DisplayName("Test con file non-PNG")
        void testInvalidFileFormat() throws IOException {
            Path textFile = tempDir.resolve("notpng.txt");
            Files.write(textFile, "not a png".getBytes());

            var config = new Steganography.ProcMetaConfig();
            config.raiseErr = false;

            Object result = Steganography.procMeta(
                    textFile,
                    Map.of("extract", true),
                    config
            );

            // In Java, this returns a Map with error instead of null like Python
            // This is an inconsistency that should be noted
            Map<String, Object> resultMap = assertIsResultMap(result);
            assertFalse((Boolean) resultMap.getOrDefault("success", true));
            assertEquals("Not PNG", resultMap.get("error"));
        }

        private static boolean channelEquals(BufferedImage a, BufferedImage b, int... channels) {
            if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
                return false;
            }

            byte[][] aChannels = splitChannels(a);
            byte[][] bChannels = splitChannels(b);

            for (int ch : channels) {
                if (!Arrays.equals(aChannels[ch], bChannels[ch])) {
                    return false;
                }
            }
            return true;
        }

        private static byte[][] splitChannels(BufferedImage image) {
            int width = image.getWidth();
            int height = image.getHeight();
            int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
            byte[][] channels = new byte[3][pixels.length];
            for (int i = 0; i < pixels.length; i++) {
                channels[0][i] = (byte) ((pixels[i] >> 16) & 0xFF);
                channels[1][i] = (byte) ((pixels[i] >> 8) & 0xFF);
                channels[2][i] = (byte) (pixels[i] & 0xFF);
            }
            return channels;
        }

        @Test
        @DisplayName("Test incorporamento su canali RGB diversi")
        void testDifferentChannels() throws IOException {
            BufferedImage original = ImageIO.read(testPng.toFile());
            Map<String, Object> metadata = Map.of(
                    "channel_test", "value"
            );

            // Canale rosso (0)
            Object resultR = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", metadata, "ch", 0),
                    null
            );

            Map<String, Object> resultRMap = assertIsResultMap(resultR);
            assertTrue((Boolean) resultRMap.get("success"));

            Path resultRPath = (Path) resultRMap.get("path");
            BufferedImage embedR = ImageIO.read(resultRPath.toFile());
            assertFalse(channelEquals(original, embedR, 0), "Canale rosso");
            assertTrue(channelEquals(original, embedR, 1, 2));

            // Canale verde (1)
            Object resultG = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", metadata, "ch", 1),
                    null
            );

            Map<String, Object> resultGMap = assertIsResultMap(resultG);
            assertTrue((Boolean) resultGMap.get("success"));

            Path resultGPath = (Path) resultGMap.get("path");
            BufferedImage embedG = ImageIO.read(resultGPath.toFile());
            assertFalse(channelEquals(original, embedG, 1), "Canale verde");
            assertTrue(channelEquals(original, embedG, 0, 2));

            // Tutti i canali (-1)
            Object resultAll = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", metadata, "ch", -1),
                    null
            );

            Map<String, Object> resultAllMap = assertIsResultMap(resultAll);
            assertTrue((Boolean) resultAllMap.get("success"));

            Path resultAllPath = (Path) resultAllMap.get("path");
            BufferedImage embedAll = ImageIO.read(resultAllPath.toFile());
            assertFalse(channelEquals(original, embedAll, 0, 1, 2), "Tutti i canali");
        }

        @Test
        @DisplayName("Test incorporamento di metadati più grandi")
        void testLargeMetadata() throws IOException {
            Map<String, Object> metadata = Map.of(
                    "title", "A".repeat(100),
                    "description", "B".repeat(200),
                    "tags", IntStream.range(0, 10)
                            .mapToObj(i -> List.of("tag1", "tag2", "tag3"))
                            .flatMap(List::stream)
                            .toList()
            );

            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", metadata),
                    null
            );

            Map<String, Object> resultMap = assertIsResultMap(result);
            assertTrue((Boolean) resultMap.get("success"));

            // Verifica estrazione
            Object extracted = Steganography.procMeta(
                    (Path) resultMap.get("path"),
                    Map.of("extract", true),
                    null
            );
            assertEquals(metadata, extracted);
        }

        @Test
        @DisplayName("Test con metadati JSON annidati")
        void testNestedMetadata() {
            Map<String, Object> metadata = Map.of(
                    "info", Map.of(
                            "author", "John",
                            "contact", Map.of(
                                    "email", "john@example.com",
                                    "phone", "123-456-7890"
                            )
                    ),
                    "stats", List.of(1L, 2L, 3L, 4L, 5.0)
            );

            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", metadata),
                    null
            );

            Map<String, Object> resultMap = assertIsResultMap(result);
            assertTrue((Boolean) resultMap.get("success"));

            Object extracted = Steganography.procMeta(
                    (Path) resultMap.get("path"),
                    Map.of("extract", true),
                    null
            );
            assertEquals(metadata, extracted);
        }
    }

    @Nested
    @DisplayName("Test per batchProc")
    class TestBatchProc {

        @Test
        @DisplayName("Test incorporamento batch di più immagini")
        void testBatchEmbedMultipleImages() throws IOException {
            Path outputDir = tempDir.resolve("output");

            Map<String, Object> metadata = Map.of(
                    "batch", "test",
                    "number", 42L
            );

            Map<Path, Boolean> results = Steganography.batchProc(
                    multipleTestPngs,
                    Map.of("embed", true, "data", metadata),
                    outputDir,
                    null
            );

            // Verifica che tutte le immagini siano state processate
            assertEquals(3, results.size());
            assertTrue(results.values().stream().allMatch(x -> x),
                    "Operazioni fallite: " + results.entrySet().stream()
                            .filter(entry -> !entry.getValue())
                            .map(Map.Entry::getKey)
                            .toList());

            // Verifica che i file di output esistano
            try (var filesStream = Files.list(outputDir)) {
                List<Path> outputFiles = filesStream.toList();

                assertNotNull(outputFiles);
                assertEquals(3, outputFiles.size(),
                        "File di output: attesi 3, trovati " + outputFiles.size());

                // Verifica che i metadati siano stati effettivamente incorporati
                for (Path outputFile : outputFiles) {
                    Object extracted = Steganography.procMeta(
                            outputFile,
                            Map.of("extract", true),
                            null
                    );
                    assertEquals(metadata, extracted);
                }

            }
        }

        @Test
        @DisplayName("Test batch con parametri personalizzati")
        void testBatchWithCustomParams() throws IOException {
            Path outputDir = tempDir.resolve("output_custom");

            Map<String, Object> metadata = Map.of(
                    "custom", true
            );

            Steganography.BatchProcConfig config = new Steganography.BatchProcConfig();
            config.channelIdx = 1;
            Map<Path, Boolean> results = Steganography.batchProc(
                    multipleTestPngs,
                    Map.of("embed", true, "data", metadata),
                    outputDir,
                    config
            );

            // Verifica che tutte le immagini siano state processate
            assertEquals(3, results.size());
            assertTrue(results.values().stream().allMatch(x -> x),
                    "Operazioni fallite: " + results.entrySet().stream()
                            .filter(entry -> !entry.getValue())
                            .map(Map.Entry::getKey));

            // Verifica che i file di output esistano
            try (var filesStream = Files.list(outputDir)) {
                List<Path> outputFiles = filesStream.toList();

                assertNotNull(outputFiles);
                assertEquals(3, outputFiles.size(),
                        "File di output: attesi 3, trovati " + outputFiles.size());

                // Verifica che i metadati siano stati effettivamente incorporati
                for (Path outputFile : outputFiles) {
                    Object extracted = Steganography.procMeta(
                            outputFile,
                            Map.of("extract", true, "ch", 1),
                            new Steganography.ProcMetaConfig(32, 1, null, null, null)
                    );
                    Map<String, Object> extractedMap = assertIsResultMap(extracted);
                    assertEquals(true, extractedMap.get("custom"));
                }
            }
        }

        @Test
        @DisplayName("Test batch handling quando alcuni file falliscono")
        void testBatchWithErrors() throws IOException {
            Path outputDir = tempDir.resolve("output_errors");

            // Aggiungi un path non valido alla lista
            List<Path> pathsWithError = new ArrayList<>(multipleTestPngs);
            pathsWithError.add(NON_EXISTENT);

            Map<String, Object> metadata = Map.of(
                    "test", "value"
            );

            Map<Path, Boolean> results = Steganography.batchProc(
                    pathsWithError,
                    Map.of("embed", true, "data", metadata),
                    outputDir,
                    null
            );

            // Verifica che le immagini valide siano processate
            assertEquals(4, results.size());
            assertEquals(3, results.values().stream().filter(x -> x).count()); // Solo 3 su 4 hanno successo
            assertFalse(results.get(NON_EXISTENT));
        }
    }

    // ==================== TEST CHE DIMOSTRANO PROBLEMI ====================

    @Nested
    @DisplayName("Questi test mostrano i problemi dell'interfaccia attuale")
    class TestInterfaceProblems {

        @Test
        @DisplayName("PROBLEMA 1: procMeta restituisce tipi diversi a seconda dell'operazione")
        void testProblem1InconsistentReturnTypes() {
            // Caso 1: restituisce Map
            Object result1 = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", Map.of("test", 1)),
                    null
            );
            Map<?, ?> resultMap1 = assertIsResultMap(result1);
            System.out.println("Tipo 1: " + resultMap1.getClass().getName());

            // Caso 2: restituisce boolean (per embed con output)
            Path output = tempDir.resolve("out.png");
            Object result2 = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", Map.of("test", 1), "out", output),
                    null
            );
            assertInstanceOf(Boolean.class, result2);
            System.out.println("Tipo 2: " + result2.getClass().getName());

            // Caso 3: restituisce None (per extract fallito)
            var config = new Steganography.ProcMetaConfig();
            config.raiseErr = false;

            Object result3 = Steganography.procMeta(
                    testPng,
                    Map.of("extract", true),
                    config
            );
            assertNull(result3);
            System.out.println("Tipo 3: null");

            // Caso 4: restituisce Path (per embed con verify)
            Path output2 = tempDir.resolve("verified.png");
            Object result4 = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", Map.of("test", 1), "out", output2, "verify", true),
                    null
            );
            assertInstanceOf(Path.class, result4);
            System.out.println("Tipo 4: " + result4.getClass().getName());

            // Caso 5: restituisce boolean (per verify senza embed!)
            Object result5 = Steganography.procMeta(
                    testPng,
                    Map.of("verify", true),
                    null
            );
            assertInstanceOf(Boolean.class, result5);
            System.out.println("Tipo 5: " + result5.getClass().getName());
        }

        @Test
        @DisplayName("PROBLEMA 2: la mappa ops ha chiavi abbreviate e poco chiare")
        void testProblem2CrypticOpsMap() {
            Object result = Steganography.procMeta(
                    testPng,
                    Map.of(
                            "embed", true,  // write? save? store?
                            "data", Map.of("key", "value"),  // metadata? meta? content?
                            "out", testPng,  // output? outputPath? destination?
                            "overwrite", true,  // replace? force?
                            "ch", 0  // channel? color? component?
                    ),
                    null
            );
            assertEquals(true, result);
        }

        @Test
        @DisplayName("PROBLEMA 3: dettagli implementativi esposti all'utente")
        void testProblem3ExposedImplementationDetails() {
            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", Map.of("test", 1)),
                    new Steganography.ProcMetaConfig(
                            32,  // Che cos'è 32? Chi lo sa senza guardare la documentazione?
                            1,   // Bit plane? LSB? MSB? Cosa significa?
                            "utf-8",  // Perché devo specificarlo?
                            0x4D455441,  // Numero magico! Ma chi lo conosce?
                            null
                    )
            );
            assertIsResultMap(result);
        }

        @Test
        @DisplayName("PROBLEMA 4: nomi dei parametri inconsistenti tra funzioni")
        void testProblem4InconsistentParameterNaming() throws IOException {
            // procMeta usa 'hdrSz' e 'bp'
            var procMetaConfig = new Steganography.ProcMetaConfig();
            procMetaConfig.hdrSz = 32;
            procMetaConfig.bp = 1;

            Object result1 = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", Map.of("test", 1)),
                    procMetaConfig
            );
            Map<String, Object> result1Map = assertIsResultMap(result1);
            assertTrue((Boolean) result1Map.get("success"));

            // batchProc usa 'headerSz' e 'bitPlane'
            var batchProcConfig = new Steganography.BatchProcConfig();
            batchProcConfig.headerSz = 32;
            batchProcConfig.bitPlane = 1;

            Path outDir = tempDir.resolve("out");
            Map<Path, Boolean> result2 = Steganography.batchProc(
                    List.of(testPng),
                    Map.of("embed", true, "data", Map.of("test", 1)),
                    outDir,
                    batchProcConfig
            );
            assertTrue(result2.values().stream().allMatch(x -> x));

            // Stesso parametro, nomi diversi!
        }

        @Test
        @DisplayName("PROBLEMA 5: gestione errori completamente inconsistente")
        void testProblem5ErrorHandlingChaos() {
            // procMeta modalità embed
            Object result1 = Steganography.procMeta(
                    NON_EXISTENT,
                    Map.of("embed", true, "data", Map.of()),
                    null
            );
            System.out.println("Errore procMeta embed: " + result1);
            Map<?, ?> resultMap1 = assertIsResultMap(result1);
            assertTrue(resultMap1.containsKey("error"));

            // procMeta modalità extract
            Object result2 = Steganography.procMeta(
                    NON_EXISTENT,
                    Map.of("extract", true),
                    null
            );
            System.out.println("Errore procMeta extract: " + result2);
            Map<?, ?> resultMap2 = assertIsResultMap(result2);
            assertTrue(resultMap2.containsKey("error"));

            // procMeta modalità verify
            Object result3 = Steganography.procMeta(
                    NON_EXISTENT,
                    Map.of("verify", true),
                    null
            );
            System.out.println("Errore procMeta verify: " + result3);
            assertEquals(false, result3);

            // procMeta con raiseErr
            var config = new Steganography.ProcMetaConfig();
            config.raiseErr = true;

            var actualException = assertThrows(RuntimeException.class, () -> {
                Steganography.procMeta(
                        NON_EXISTENT,
                        Map.of("verify", true),
                        config
                );
            });
            assertEquals("java.io.IOException: File not found: /nonexistent.png", actualException.getMessage());
        }

        @Test
        @DisplayName("PROBLEMA 6: numeri magici ovunque")
        void testProblem6MagicNumbersEverywhere() {
            Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", Map.of("test", 1), "ch", 0),
                    new Steganography.ProcMetaConfig(
                            32,  // ???
                            1,   // ???
                            null,
                            0x4D455441,  // ???
                            null
                    )
            );
        }

        @Test
        @DisplayName("PROBLEMA 7: procMeta fa TUTTO")
        void testProblem7UnclearFunctionResponsibility() {
            Path output = tempDir.resolve("everything.png");

            Steganography.procMeta(
                    testPng,
                    Map.of(
                            "embed", true,  // incorpora
                            "data", Map.of("key", "value"),  // questi dati
                            "out", output,  // salva qui
                            "verify", true,  // e verifica
                            "ch", 0  // sul canale rosso
                    ),
                    null
            );

            // Che cosa ha fatto esattamente? Chi lo sa!
        }

        @Test
        @DisplayName("PROBLEMA 8: il parametro channel non è intuitivo")
        void testProblem8ChannelParameterConfusion() {
            // Utente confuso: "Voglio usare tutti i canali... ch=3?"
            Object result = Steganography.procMeta(
                    testPng,
                    Map.of("embed", true, "data", Map.of("test", 1), "ch", 3),
                    null
            );
            Map<String, Object> resultMap = assertIsResultMap(result);
            assertFalse((Boolean) resultMap.get("success"));
            assertEquals("Index 3 out of bounds for length 3", resultMap.get("error")); // No, è -1!
        }
    }

    // ==================== TEST PER NUOVE FUNZIONI (da completare) ====================

    @Nested
    @DisplayName("Questi test rappresentano come DOVREBBE essere la nuova interfaccia")
    class TestNewInterfaceIdeas {
        /*
         * ATTENZIONE!
         * In questi test richiamiamo funzioni che non sono ancora state implementate!
         * Per questo usiamo dei metodi di utilità definiti qui di seguito: leggi i JavaDocs per informazioni.
         */

        /**
         * Cerca ed esegui un metodo {@code public static} nella classe {@link Steganography}.
         * <p>
         * Se il metodo non è trovato, vuol dire che non lo hai ancora implementato e il test viene saltato.
         * <p>
         * (Il codice di questo metodo è particolarmente oscuro: usa {@code java.reflection} per accedere al metodo
         * richiesto... è una funzionalità piuttosto avanzata)
         *
         * @param functionName Il nome del metodo da invocare
         * @param args         I parametri da passare all'invocazione del metodo
         * @return Il valore ritornato dall'invocazione, convertito per comodità nel tipo generico T
         */
        @SuppressWarnings("unchecked")
        private static <T> T _lib(String functionName, Object... args) {
            Method function = Stream.of(Steganography.class.getDeclaredMethods())
                    .filter(m -> m.getName().equals(functionName) && m.getParameters().length == args.length)
                    .findFirst()
                    .orElse(null);
            assumeTrue(function != null, "metodo " + functionName + " non ancora implementato");

            var modifiers = function.getModifiers();
            assumeTrue(Modifier.isStatic(modifiers), "metodo " + functionName + " non statico");
            assumeTrue(Modifier.isPublic(modifiers), "metodo " + functionName + " non pubblico");
            try {
                return (T) function.invoke(null, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * Cerca ed esegui un metodo sull'oggetto specificato, che ritorna il tipo specificato.
         * <p>
         * Se il metodo non è trovato, ci sarà un errore nei test.
         * <p>
         * (Il codice di questo metodo è particolarmente oscuro: usa {@code java.reflection} per accedere al metodo
         * richiesto... è una funzionalità piuttosto avanzata)
         *
         * @param clazz      Il tipo che deve essere ritornato dal metodo
         * @param obj        L'oggetto su cui cercare e invocare il metodo
         * @param methodName Il nome del metodo da invocare
         * @param args       I parametri da passare all'invocazione del metodo
         * @return Il valore ritornato dall'invocazione, convertito per comodità nel tipo generico T
         */
        @SuppressWarnings("unchecked")
        private static <T> T _call(Class<T> clazz, Object obj, String methodName, Object... args) {
            Method function = Stream.of(obj.getClass().getDeclaredMethods())
                    .filter(m -> m.getName().equals(methodName) && m.getParameters().length == args.length)
                    .findFirst()
                    .orElse(null);
            assertNotNull(function, "metodo " + obj.getClass().getSimpleName() + "." + methodName +
                    " non implementato");

            var modifiers = function.getModifiers();
            assertTrue(Modifier.isPublic(modifiers), "metodo " + obj.getClass().getSimpleName() + "." +
                    methodName + " non pubblico");
            try {
                return (T) function.invoke(obj, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        @DisplayName("Funzione proposta: embedMetadata(Path imagePath, Map metadata, Path outputPath=null) -> MetadataResult")
        void testEmbedMetadataSimple() throws Exception {
            /*
             * Dovrebbe incorporare metadati in modo semplice e diretto,
             * nascondendo completamente i dettagli implementativi.
             *
             * Esempio d'uso:
             *     var result = embedMetadata(Paths.get("photo.png"), Map.of("author", "John"));
             *     // Fatto! Niente magic numbers, niente complessità;
             */

            Path output = tempDir.resolve("embedded.png");
            // Il risultato dovrebbe essere di un tipo `MetadataResult`
            var result = /* (MetadataResult) */ _lib(
                    "embedMetadata",
                    testPng,
                    Map.of("author", "John Doe"),
                    output
            );

            assertNotNull(result);
            assertTrue(_call(Boolean.class, result, "isSuccess"));
            assertTrue(Files.exists(_call(Path.class, result, "getOutputPath")));
        }

        @Test
        @DisplayName("Funzione proposta: extractMetadata(Path imagePath) -> Map")
        void testExtractMetadataSimple() throws Exception {
            /*
             * Interfaccia semplice e chiara per estrarre metadati.
             * Restituisce sempre una Map (vuoto se non ci sono metadati).
             *
             * Esempio d'uso:
             *     var metadata = extractMetadata(Paths.get("photo.png"));
             *     System.out.println(metadata.get("author"));
             */

            Map<String, Object> result = _lib(
                    "extractMetadata",
                    testPngWithMetadata
            );

            assertNotNull(result);
            assertTrue(result.containsKey("author"));
            assertEquals("Test Author", result.get("author"));
        }

        @Test
        @DisplayName("Funzione proposta: hasMetadata(Path imagePath) -> boolean")
        void testHasMetadataSimple() throws Exception {
            /*
             * Verifica semplice se l'immagine contiene metadati nascosti.
             *
             * Esempio d'uso:
             *     if (hasMetadata(Paths.get("photo.png")) {
             *         System.out.println("Questa immagine ha metadati!");
             *     }
             */

            // immagine senza metadati
            assertFalse(_lib("hasMetadata", testPng));
            // immagine con metadati
            assertTrue(_lib("hasMetadata", testPngWithMetadata));
        }

        @Test
        @DisplayName("Funzione proposta: updateMetadata(Path imagePath, Map metadata, Path outputPath=null) -> MetadataResult")
        void testUpdateMetadataSimple() throws Exception {
            /*
             * Aggiorna metadati esistenti o ne crea di nuovi.
             *
             * Esempio d'uso:
             *     var result = updateMetadata(Paths.get("photo.png"), Map.of("year", 2025));
             *     // I metadati esistenti vengono preservati, solo "year" viene aggiunto/aggiornato
             */

            Path output = tempDir.resolve("updated.png");
            // Il risultato dovrebbe essere di un tipo `MetadataResult`
            var result = /* (MetadataResult) */ _lib(
                    "updateMetadata",
                    testPngWithMetadata,
                    Map.of("author", "New Author"),
                    output
            );

            assertNotNull(result);
            assertTrue(_call(Boolean.class, result, "isSuccess"));
            assertTrue(Files.exists(_call(Path.class, result, "getOutputPath")));

            // Verifica che i vecchi metadati siano preservati
            Map<String, Object> updatedMeta = _lib("extractMetadata", output);
            assertEquals("New Author", updatedMeta.get("author"));
            assertEquals("Test Image", updatedMeta.get("title")); // Preservato
        }

        @Test
        @DisplayName("Funzione proposta: clearMetadata(Path imagePath, Path outputPath=null) -> MetadataResult")
        void testClearMetadataSimple() throws Exception {
            /*
             * Rimuove i metadati nascosti da un'immagine.
             *
             * Esempio d'uso:
             *     var result = clearMetadata(Paths.get("photo.png"));
             *     // Metadati rimossi, tutti i LSB impostati a 0
             */

            Path output = tempDir.resolve("clean.png");
            // Il risultato dovrebbe essere di un tipo `MetadataResult`
            var result = /* (MetadataResult) */ _lib(
                    "clearMetadata",
                    testPngWithMetadata,
                    output
            );

            assertNotNull(result);
            assertTrue(_call(Boolean.class, result, "isSuccess"));
            assertTrue(Files.exists(_call(Path.class, result, "getOutputPath")));

            // Verifica che non ci siano più metadati
            assertFalse(_lib("hasMetadata", output));
        }

        @Test
        @DisplayName("Funzione proposta: copyMetadata(Path sourcePath, Path destinationPath, Path outputPath=null) -> MetadataResult")
        void testCopyMetadataBetweenImages() throws Exception {
            /*
             * Copia metadati da un'immagine all'altra.
             *
             * Esempio d'uso:
             *     var result = copyMetadata(Paths.get("source.png"), Paths.get("destination.png"));
             *     // Metadati copiati!
             */

            // Il risultato dovrebbe essere di un tipo `MetadataResult`
            var result = /* (MetadataResult) */ _lib(
                    "copyMetadata",
                    testPngWithMetadata,
                    testPng
            );

            assertNotNull(result);
            assertTrue(_call(Boolean.class, result, "isSuccess"));
            assertTrue(Files.exists(_call(Path.class, result, "getOutputPath")));

            // Verifica che i metadati siano stati copiati
            Map<String, Object> originalMeta = _lib("extractMetadata", testPngWithMetadata);
            Map<String, Object> copiedMeta = _lib("extractMetadata", testPng);
            assertEquals(originalMeta, copiedMeta);
        }
    }

    /*
    🎯 HAI FINITO LA TUA ANALISI?

    Ottimo lavoro! Ora è il momento di dimostrare che la tua nuova interfaccia
    è davvero migliore della vecchia.

    COSA HAI VISTO NEI TEST SOPRA?

    I test in TestInterfaceProblems mostrano 8 problemi gravi:
    1. ❌ Tipi di ritorno inconsistenti (Map/boolean/null/Path)
    2. ❌ Chiavi del dizionario ops criptiche ('embed', 'data', 'out', 'ch')
    3. ❌ Dettagli implementativi esposti (magic numbers, header size, bit planes)
    4. ❌ Nomi parametri inconsistenti (hdrSz vs headerSz, bp vs bitPlane, ch vs channelIdx)
    5. ❌ Gestione errori caotica (ogni funzione fa come vuole)
    6. ❌ Numeri magici ovunque (32, 1, 0x4D455441, -1)
    7. ❌ Una funzione che fa troppo (procMeta)
    8. ❌ Parametro channel confuso (0, 1, 2, -1)

    La tua nuova interfaccia dovrebbe risolvere TUTTI questi problemi!

    PROSSIMI PASSI:

    1. Implementa le funzioni suggerite in TestNewInterfaceIdeas (o inventa le tue!)
    2. Aggiungi i tuoi test qui sotto nella classe TestMyNewInterface
    3. I tuoi test dovrebbero dimostrare che:
       ✅ Ogni funzione ha un nome chiaro e auto-esplicativo
       ✅ I parametri sono intuitivi (no abbreviazioni, no dizionari complessi)
       ✅ Tutte le funzioni restituiscono lo stesso tipo di risultato
       ✅ La gestione errori è consistente ovunque
       ✅ Non ci sono dettagli implementativi esposti (magic numbers, bit planes)
       ✅ Ogni funzione ha una singola responsabilità chiara

    4. Esempi di test che potresti scrivere:

        @Test
        public void testConsistentReturnTypes(String testPng, String testPngWithMetadata) {
            // Dimostra che tutte le tue funzioni restituiscono lo stesso tipo
            Object result1 = embedMetadata(testPng, Map.of("test", 1));
            Object result2 = extractMetadata(testPngWithMetadata);
            Object result3 = hasMetadata(testPng);

            // Tutti restituiscono schemi coerenti e prevedibili
            assertTrue(result1 instanceof MetadataResult);
            assertTrue(result2 instanceof Map);
            assertTrue(result3 instanceof Boolean);
        }

        @Test
        public void testConsistentErrorHandling() {
            // Dimostra che gli errori sono gestiti in modo uniforme
            Object result1 = embedMetadata(NON_EXISTENT), Map.of());
            Object result2 = extractMetadata(NON_EXISTENT));
            Object result3 = hasMetadata(NON_EXISTENT));

            // Tutti gestiscono gli errori nello stesso modo
            // (es. tutti restituiscono success=false, o tutti sollevano eccezioni)
        }

    BONUS: Considera di aggiungere funzioni di convenienza per casi d'uso comuni:
    - addCopyright(imagePath, copyrightText): aggiunge copyright come metadato
    - addAuthorInfo(imagePath, name, email): aggiunge info autore
    - addCreationDate(imagePath, date=null): aggiunge data (default: oggi)
    - getImageInfo(imagePath): restituisce sia metadati che info base sull'immagine
    - batchEmbedMetadata(imagePaths, metadata, outputDir): incorpora gli stessi metadati in più immagini

    Buon lavoro! 🚀
    */

    @SuppressWarnings("unchecked")
    private static Map<String, Object> assertIsResultMap(Object obj) {
        assertNotNull(obj);
        return assertInstanceOf(Map.class, obj);
    }
}