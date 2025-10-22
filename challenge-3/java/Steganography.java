import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * Steganography Library - BEFORE refactoring
 * <p>
 * Questa libreria permette di nascondere metadati JSON in immagini PNG
 * usando la tecnica LSB (Least Significant Bit).
 * PROBLEMA: le interfacce sono troppo complesse e "trapelano" dettagli implementativi.
 * <p>
 * Il tuo compito: riprogettare l'interfaccia per renderla semplice e intuitiva.
 */
public class Steganography {

    /**
     * Parametri di configurazione opzionali per procMeta
     */
    public static class ProcMetaConfig {
        public Integer hdrSz;
        public Integer bp;
        public String enc;
        public Integer magic;
        public Boolean raiseErr;

        public ProcMetaConfig() {
        }

        public ProcMetaConfig(Integer hdrSz, Integer bp, String enc, Integer magic, Boolean raiseErr) {
            this.hdrSz = hdrSz;
            this.bp = bp;
            this.enc = enc;
            this.magic = magic;
            this.raiseErr = raiseErr;
        }
    }

    /**
     * Processa metadati su un'immagine PNG. Può fare embed, extract, verify,
     * e update a seconda delle chiavi nel dizionario ops.
     *
     * @param p      percorso file
     * @param ops    dizionario operazioni con chiavi: 'embed', 'extract', 'verify',
     *               'data', 'out', 'ch', 'overwrite'
     * @param config configurazione (se null o campi null, usa defaults)
     * @return Dipende dall'operazione: dict con metadati, bool per successo,
     * null per errore, o stringa path
     */
    public static Object procMeta(
            Path p,
            Map<String, Object> ops,
            ProcMetaConfig config
    ) {
        // Apply defaults
        if (config == null) {
            config = new ProcMetaConfig();
        }
        int hdrSz = config.hdrSz != null ? config.hdrSz : 32;
        int bp = config.bp != null ? config.bp : 1;
        String enc = config.enc != null ? config.enc : "utf-8";
        int magic = config.magic != null ? config.magic : 0x4D455441;
        boolean raiseErr = config.raiseErr != null ? config.raiseErr : false;

        try {
            if (!Files.exists(p)) {
                if (raiseErr) {
                    throw new IOException("File not found: " + p);
                }
                if ((Boolean) ops.getOrDefault("verify", false)) {
                    return false;
                }
                return Map.of(
                        "success", false,
                        "error", "File not found"
                );
            }

            BufferedImage img = openImage(p);
            if (img == null) {
                if (raiseErr) {
                    throw new IOException("Not a valid PNG file");
                }
                if ((Boolean) ops.getOrDefault("verify", false)) {
                    return false;
                }
                return Map.of(
                        "success", false,
                        "error", "Not PNG"
                );
            }

            // Extract
            if ((Boolean) ops.getOrDefault("extract", false)) {
                int ch = (Integer) ops.getOrDefault("ch", -1);
                Map<String, Object> extracted = extBits(img, hdrSz, bp, ch, magic, enc);
                if (extracted == null) {
                    if (raiseErr) {
                        throw new RuntimeException("No metadata found");
                    }
                    return null;
                }
                return extracted;
            }

            // Embed
            if ((Boolean) ops.getOrDefault("embed", false)) {
                if (!ops.containsKey("data")) {
                    if (raiseErr) {
                        throw new RuntimeException("No data to embed");
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", false);
                    result.put("error", "No data");
                    return result;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> metaData = (Map<String, Object>) ops.get("data");
                int ch = (Integer) ops.getOrDefault("ch", -1);

                BufferedImage modifiedImg = embBits(img, metaData, hdrSz, bp, ch, magic, enc);

                Path outputPath = (Path) ops.getOrDefault("out", p);
                if (!((Boolean) ops.getOrDefault("overwrite", false)) && outputPath.equals(p)) {
                    String[] baseExt = splitNameExtension(p.getFileName());
                    outputPath = outputPath.resolveSibling(baseExt[0] + "_embedded" + baseExt[1]);
                }

                ImageIO.write(modifiedImg, "PNG", outputPath.toFile());

                if (ops.containsKey("verify") && (Boolean) ops.get("verify")) {
                    return outputPath;
                } else if (ops.containsKey("data") && ops.containsKey("out")) {
                    return true;
                } else {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("path", outputPath);
                    result.put("size", toJson(metaData).length());
                    return result;
                }
            }

            // Update (extract + modify + embed)
            if (ops.containsKey("update")) {
                int ch = (Integer) ops.getOrDefault("ch", -1);
                Map<String, Object> existing = extBits(img, hdrSz, bp, ch, magic, enc);

                if (existing == null) {
                    existing = new HashMap<>();
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> updateData = (Map<String, Object>) ops.get("update");
                existing.putAll(updateData);

                BufferedImage modifiedImg = embBits(img, existing, hdrSz, bp, ch, magic, enc);

                Path outputPath = (Path) ops.getOrDefault("out", p);
                ImageIO.write(modifiedImg, "PNG", outputPath.toFile());

                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("path", outputPath);
                return result;
            }

            // Verify
            if ((Boolean) ops.getOrDefault("verify", false)) {
                int ch = (Integer) ops.getOrDefault("ch", -1);
                boolean hasMeta = checkMeta(img, hdrSz, bp, ch, magic);
                return hasMeta;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", "No valid operation");
            return result;

        } catch (Exception ex) {
            if (raiseErr) {
                throw new RuntimeException(ex);
            }
            if ((Boolean) ops.getOrDefault("verify", false)) {
                return false;
            } else if ((Boolean) ops.getOrDefault("extract", false)) {
                return null;
            }
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", ex.getMessage());
            return result;
        }
    }

    private static BufferedImage openImage(Path filePath) throws IOException {
        BufferedImage img = ImageIO.read(filePath.toFile());
        if (img == null) {
            return null;
        }

        // Convert to RGB if needed
        if (img.getType() == BufferedImage.TYPE_INT_RGB) {
            return img;
        }

        BufferedImage rgbImg = new BufferedImage(
                img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        rgbImg.getGraphics().drawImage(img, 0, 0, null);
        return rgbImg;
    }

    private static String[] splitNameExtension(Path fileNamePath) {
        String fileName = fileNamePath.toString();
        int dotIdx = fileName.lastIndexOf('.');
        String base = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
        String ext = dotIdx > 0 ? fileName.substring(dotIdx) : "";
        return new String[]{base, ext};
    }

    /**
     * Nasconde metadati JSON nei bit meno significativi dell'immagine
     */
    private static BufferedImage embBits(
            BufferedImage img,
            Map<String, Object> metadata,
            int headerSize,
            int bitPlane,
            int channel,
            int magicNum,
            String encoding
    ) {
        String jsonStr = toJson(metadata);
        byte[] jsonBytes = jsonStr.getBytes(Charset.forName(encoding));

        // Crea header: magic number (4 bytes) + lunghezza (4 bytes)
        int payloadLen = jsonBytes.length;
        byte[] header = new byte[8];

        // Magic number (4 bytes, big-endian)
        System.arraycopy(intToBytes(magicNum), 0, header, 0, 4);
        // Payload length (4 bytes, big-endian)
        System.arraycopy(intToBytes(payloadLen), 0, header, 4, 4);

        byte[] fullPayload = new byte[header.length + jsonBytes.length];
        System.arraycopy(header, 0, fullPayload, 0, header.length);
        System.arraycopy(jsonBytes, 0, fullPayload, header.length, jsonBytes.length);

        // Converti in lista di bit
        List<Integer> bits = new ArrayList<>();
        for (byte b : fullPayload) {
            for (int i = 7; i >= 0; i--) {
                bits.add((b >> i) & 1);
            }
        }

        // Create a copy to modify
        BufferedImage modified = new BufferedImage(
                img.getWidth(),
                img.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        modified.getGraphics().drawImage(img, 0, 0, null);

        int width = modified.getWidth();
        int height = modified.getHeight();
        int bitIdx = 0;

        // Define a bit-mask to force the steganographic data bit to 0
        int dataBit = bitPlane - 1;
        int mask = ~(1 << dataBit);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIdx >= bits.size()) {
                    break;
                }

                byte[] pixel = readPixel(modified, x, y);

                // Embed in specific channel or all channels
                if (channel == -1) {
                    // All channels
                    for (int c = 0; c < 3; c++) {
                        if (bitIdx >= bits.size()) {
                            break;
                        }
                        pixel[c] = (byte) ((pixel[c] & mask) | (bits.get(bitIdx) << dataBit));
                        bitIdx++;
                    }
                } else {
                    // Specific channel
                    pixel[channel] = (byte) ((pixel[channel] & mask) | (bits.get(bitIdx) << dataBit));
                    bitIdx++;
                }

                writePixel(modified, x, y, pixel);
            }
        }

        return modified;
    }

    private static byte[] intToBytes(int i) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(i);
        return buffer.array();
    }

    private static int bytesToInt(byte[] bytes) {
        return bytesToInt(bytes, 0);
    }

    private static int bytesToInt(byte[] bytes, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(offset);
        return buffer.getInt();
    }

    private static byte[] readPixel(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(rgb);
        return Arrays.copyOfRange(buffer.array(), 1, 4); // ritaglia il canale alfa
    }

    private static void writePixel(BufferedImage image, int x, int y, byte[] pixel) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put((byte) 0xFF); // aggiungi il canale alfa
        buffer.put(pixel);
        buffer.flip();

        int rgb = buffer.getInt();
        image.setRGB(x, y, rgb);
    }

    /**
     * Estrae metadati JSON nascosti nell'immagine
     */
    private static Map<String, Object> extBits(
            BufferedImage img,
            int headerSize,
            int bitPlane,
            int channel,
            int magicNum,
            String encoding
    ) {
        List<Integer> bits = readBits(img, headerSize, bitPlane, channel, -1);

        // Decode header (8 bytes = 64 bits)
        byte[] headerBytes = bitsToBytes(bits.subList(0, Math.min(64, bits.size())));

        if (headerBytes.length < 4) {
            return null;
        }

        int foundMagic = bytesToInt(headerBytes);
        if (foundMagic != magicNum) {
            return null;
        }

        // Extract payload
        if (headerBytes.length < 8) {
            return null;
        }

        int payloadLen = bytesToInt(headerBytes, 4);

        int neededBits = payloadLen * 8;
        int endIdx = Math.min(64 + neededBits, bits.size());
        byte[] payloadBytes = bitsToBytes(bits.subList(64, endIdx));

        try {
            String jsonStr = new String(payloadBytes, Charset.forName(encoding));
            return fromJson(jsonStr);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Integer> readBits(
            BufferedImage img,
            int headerSize,
            int bitPlane,
            int channel,
            int bitn
    ) {
        int width = img.getWidth();
        int height = img.getHeight();

        List<Integer> bits = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitn >= 0 && bits.size() >= bitn) {
                    break;
                }

                byte[] pixel = readPixel(img, x, y);

                if (channel == -1) {
                    // Extract from all channels
                    for (int c = 0; c < 3; c++) {
                        if (bitn >= 0 && bits.size() >= bitn) {
                            break;
                        }
                        int bit = (pixel[c] >> (bitPlane - 1)) & 1;
                        bits.add(bit);
                    }
                } else {
                    // Extract from specific channel
                    int bit = (pixel[channel] >> (bitPlane - 1)) & 1;
                    bits.add(bit);
                }
            }
        }
        return bits;
    }

    /**
     * Verifica se l'immagine contiene metadati validi
     */
    private static boolean checkMeta(
            BufferedImage img,
            int headerSize,
            int bitPlane,
            int channel,
            int magicNum
    ) {
        // Extract only magic number (first 32 bits)
        List<Integer> bits = readBits(img, headerSize, bitPlane, channel, 32);

        byte[] headerBytes = bitsToBytes(bits);

        if (headerBytes.length < 4) {
            return false;
        }

        int foundMagic = bytesToInt(headerBytes);
        return foundMagic == magicNum;
    }

    /**
     * Converte lista di bit in bytes
     */
    private static byte[] bitsToBytes(List<Integer> bits) {
        byte[] result = new byte[(bits.size() + 7) / 8];
        for (int i = 0; i < bits.size(); i += 8) {
            int byteVal = 0;
            for (int j = 0; j < 8; j++) {
                if (i + j < bits.size()) {
                    byteVal = (byteVal << 1) | bits.get(i + j);
                } else {
                    byteVal = byteVal << 1;
                }
            }
            result[i / 8] = (byte) byteVal;
        }
        return result;
    }

    /**
     * Parametri di configurazione opzionali per batchProc
     */
    public static class BatchProcConfig {
        public Integer headerSz;
        public Integer bitPlane;
        public Integer channelIdx;

        public BatchProcConfig() {
        }

        public BatchProcConfig(Integer headerSz, Integer bitPlane, Integer channelIdx) {
            this.headerSz = headerSz;
            this.bitPlane = bitPlane;
            this.channelIdx = channelIdx;
        }
    }

    /**
     * Processa batch di immagini con stesse operazioni.
     *
     * @param paths  lista percorsi immagini
     * @param ops    dizionario operazioni (vedi procMeta)
     * @param outDir directory output
     * @param config configurazione (se null o campi null, usa defaults)
     * @return dict {path_originale: success_bool}
     */
    public static Map<Path, Boolean> batchProc(
            List<Path> paths,
            Map<String, Object> ops,
            Path outDir,
            BatchProcConfig config
    ) throws IOException {
        // Apply defaults
        if (config == null) {
            config = new BatchProcConfig();
        }
        int headerSz = config.headerSz != null ? config.headerSz : 32;
        int bitPlane = config.bitPlane != null ? config.bitPlane : 1;
        int channelIdx = config.channelIdx != null ? config.channelIdx : -1;

        Map<Path, Boolean> results = new HashMap<>();

        if (!Files.exists(outDir)) {
            Files.createDirectories(outDir);
        }

        for (Path imgPath : paths) {
            try {
                Path filename = imgPath.getFileName();
                Path outputPath = outDir.resolve(filename);

                Map<String, Object> opsCopy = new HashMap<>(ops);
                opsCopy.put("out", outputPath);
                opsCopy.put("ch", channelIdx);

                ProcMetaConfig procConfig = new ProcMetaConfig();
                procConfig.hdrSz = headerSz;
                procConfig.bp = bitPlane;
                Object result = procMeta(imgPath, opsCopy, procConfig);

                switch (result) {
                    case Map map -> {
                        @SuppressWarnings("unchecked")
                        boolean success = (Boolean) map.getOrDefault("success", false);
                        results.put(imgPath, success);
                    }
                    case Boolean b -> results.put(imgPath, b);
                    case String s -> results.put(imgPath, Files.exists(Paths.get(s)));
                    case null, default -> results.put(imgPath, false);
                }
            } catch (Exception e) {
                results.put(imgPath, false);
            }
        }

        return results;
    }

    /**
     * Estrae metadati nascosti da immagine PNG.
     *
     * @param p    percorso immagine
     * @param keys lista chiavi specifiche da estrarre (null = tutte)
     * @return dizionario metadati
     */
    public static Map<String, Object> getMeta(String p, List<String> keys) {
        throw new UnsupportedOperationException("get_meta");
    }

    /**
     * Serializza in formato JSON una mappa contenente stringhe, numeri, booleani, array, oggetti.
     * I campi nulli vengono saltati.
     *
     * @param map L'oggetto da serializzare
     * @return La stringa JSON che descrive l'oggetto
     */
    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();

        List<String> keyValues = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            // Skip null values
            if (value == null) {
                continue;
            }

            sb.append("\"").append(escapeJson(key)).append("\":").append(valueToJson(value));

            keyValues.add(sb.toString());
            sb.setLength(0);
        }

        return "{" + String.join(",", keyValues) + "}";
    }

    private static String valueToJson(Object value) {
        return switch (value) {
            case Map m -> toJson(m);
            case List l -> "[" + l.stream().map(Steganography::valueToJson).collect(joining(",")) + "]";
            case String s -> "\"" + escapeJson(s) + "\"";
            case Number n -> n.toString();
            case Boolean b -> b.toString();
            default -> throw new IllegalArgumentException(value.toString());
        };
    }

    private static String escapeJson(String str) {
        return str.replaceAll("[\"\n\r\t\\\\]", "\\\\$0");
    }

    /**
     * Deserializza un oggetto generico dalla rappresentazione JSON. Supporta stringhe, numeri, booleani, array, oggetti.
     *
     * @param json La stringa JSON da deserializzare
     * @return Una mappa contenente i valori estratti
     */
    private static Map<String, Object> fromJson(String json) {
        JSONParser parser = new JSONParser(json);
        return parser.parseMainObject();
    }
}