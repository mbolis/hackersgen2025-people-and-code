import java.util.List;

/**
 * Sistema di gestione sconti per uno shop online.
 * <p>
 * Il codice funziona, ma è pieno di magic numbers e magic strings.
 * Estrai tutte le costanti e dai loro nomi significativi!
 */
public class ShopDiscount {

    public record Item(
            double p,
            int q
    ) {
    }

    public record Order(
            double subtotal,
            double tax,
            double total,
            boolean discountApplied
    ) {
    }

    /**
     * Calcola il prezzo finale con sconto.
     *
     * @param price        prezzo unitario del prodotto
     * @param customerType tipo di cliente ("basic", "premium", "vip")
     * @param quantity     quantità di prodotti acquistati
     * @return prezzo finale dopo applicazione di tutti gli sconti
     */
    public static double calculateDiscount(double price, String customerType, int quantity) {
        // Sconto per clienti premium
        if (customerType.equals("premium")) {
            price = price * 0.85;
        }

        // Sconto per clienti VIP
        if (customerType.equals("vip")) {
            price = price * 0.75;
        }

        // Sconto per quantità
        if (quantity >= 10) {
            price = price * 0.9;
        } else if (quantity >= 5) {
            price = price * 0.95;
        }

        // Sconto minimo garantito per ordini grandi
        if (price > 100) {
            price = price * 0.98;
        }

        return Math.round(price * 100.0) / 100.0;
    }

    /**
     * Calcola il totale di un ordine applicando sconti per cliente e tasse.
     *
     * @param products     lista di coppie [p, q]
     * @param customerType tipo di cliente ("basic", "premium", "vip")
     * @return mappa con subtotale, tasse e totale finale
     */
    public static Order calculateTotalOrder(List<Item> products, String customerType) {
        double s = 0;

        for (Item i : products) {
            double d = calculateDiscount(i.p(), customerType, i.q());
            s = s + (d * i.q());
        }

        // Tassa sul valore
        double t = s * 0.22;

        double tot = s + t;

        // Sconto finale se l'ordine è superiore a 500 euro
        if (tot > 500) {
            tot = tot * 0.95;
        }

        Order result = new Order(
                Math.round(s * 100.0) / 100.0,
                Math.round(t * 100.0) / 100.0,
                Math.round(tot * 100.0) / 100.0,
                tot != s + t
        );
        return result;
    }

    /**
     * Ritorna una descrizione del livello cliente.
     */
    public static String getCustomerTierDescription(String customerType) {
        return switch (customerType) {
            case "basic" -> "Cliente Base";
            case "premium" -> "Cliente Premium (15% sconto base)";
            case "vip" -> "Cliente VIP (25% sconto base)";
            default -> "Tipo cliente sconosciuto";
        };
    }

    /**
     * Formatta uno scontrino leggibile.
     */
    public static String formatOrderReceipt(Order orderData, String customerType) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("=".repeat(40)).append("\n");
        receipt.append("SCONTRINO ORDINE\n");
        receipt.append("=".repeat(40)).append("\n");
        receipt.append("Tipo cliente: ").append(getCustomerTierDescription(customerType)).append("\n");
        receipt.append("-".repeat(40)).append("\n");
        receipt.append("Subtotale: € ").append(orderData.subtotal()).append("\n");
        receipt.append("Tasse (22%): € ").append(orderData.tax()).append("\n");

        if (orderData.discountApplied()) {
            receipt.append("SCONTO PER ORDINE GRANDE: -5%\n");
        }

        receipt.append("-".repeat(40)).append("\n");
        receipt.append("TOTALE: € ").append(orderData.total()).append("\n");
        receipt.append("=".repeat(40)).append("\n");

        return receipt.toString();
    }

    // Esempio di utilizzo
    public static void main(String[] args) {
        List<Item> products = List.of(
                new Item(50, 2),    // 2x prodotto da €50
                new Item(30, 8)     // 8x prodotto da €30
        );

        Order order = calculateTotalOrder(products, "premium");
        System.out.println(formatOrderReceipt(order, "premium"));

        System.out.println("\nAltro esempio:");
        List<Item> products2 = List.of(
                new Item(100, 3),
                new Item(50, 5)
        );
        Order order2 = calculateTotalOrder(products2, "vip");
        System.out.println(formatOrderReceipt(order2, "vip"));
    }
}
