import java.util.List;

/**
 * Sistema di gestione sconti per uno shop online.
 * <p>
 * Il codice funziona, ma è pieno di magic numbers e magic strings.
 * Estrai tutte le costanti e dai loro nomi significativi!
 */
public class ShopDiscount {

    public static final double STANDARD_DISCOUNT = 0.85;
    public static final double PREMIUM_DISCOUNT = 0.85;
    public static final double VIP_DISCOUNT = 0.75;
    public static final double QUANTITY_DISCOUNT_5 = 0.95;
    public static final double QUANTITY_DISCOUNT_10 = 0.9;
    public static final double LARGE_ORDER_DISCOUNT_THRESHOLD = 100.0;
    public static final double LARGE_ORDER_DISCOUNT = 0.98;
    public static final double TAX_RATE = 0.22;
    public static final double TOTAL_ORDER_DISCOUNT_THRESHOLD = 500.0;
    public static final double TOTAL_ORDER_DISCOUNT = 0.95;

    public record Item(
            double price,
            int quantity
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
            price = price * PREMIUM_DISCOUNT;
        }

        // Sconto per clienti VIP
        if (customerType.equals("vip")) {
            price = price * VIP_DISCOUNT;
        }

        // Sconto per quantità
        if (quantity >= 10) {
            price = price * QUANTITY_DISCOUNT_10;
        } else if (quantity >= 5) {
            price = price * QUANTITY_DISCOUNT_5;
        }

        // Sconto minimo garantito per ordini grandi
        if (price > 100) {
            price = price * LARGE_ORDER_DISCOUNT;
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
        double subtotal = 0;

        for (Item i : products) {
            double discount = calculateDiscount(i.price(), customerType, i.quantity());
            subtotal = subtotal + (discount * i.quantity());
        }

        // Tassa sul valore
        double tax = subtotal * 0.22;

        double tot = subtotal + tax;

        // Sconto finale se l'ordine è superiore a 500 euro
        if (tot > 500) {
            tot = tot * TOTAL_ORDER_DISCOUNT;
        }

        Order result = new Order(
                Math.round(subtotal * 100.0) / 100.0,
                Math.round(tax * 100.0) / 100.0,
                Math.round(tot * 100.0) / 100.0,
                tot != subtotal + tax
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
