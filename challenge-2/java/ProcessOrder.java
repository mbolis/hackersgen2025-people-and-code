import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Sfida 2: Single Responsibility Principle
 * <p>
 * Questo file contiene una funzione che viola il SRP.
 * Il tuo compito è spezzarla in funzioni più piccole e focalizzate.
 * <p>
 * Usa questo file come punto di partenza. Puoi modificarlo come preferisci,
 * aggiungere funzioni, cambiare la struttura - l'importante è che alla fine
 * il codice sia più leggibile e manutenibile.
 */
public class ProcessOrder {

    public record Item(
            String name,
            double price,
            int quantity
    ) {
    }

    public record OrderData(
            String customerName,
            String customerEmail,
            boolean isVipCustomer,
            List<Item> items
    ) {
    }

    public record OrderRecord(
            String customerName,
            String customerEmail,
            List<Item> items,
            double subtotal,
            double tax,
            double total,
            boolean vip,
            String status
    ) {
    }

    public interface Database {
        void saveOrder(OrderRecord order) throws Exception;
    }

    public interface EmailService {
        void send(String to, String subject, String body) throws Exception;
    }

    /**
     * Processa un ordine dal caricamento alla notifica.
     * ⚠️ ATTENZIONE: questa funzione fa TROPPO!
     *
     * @param orderData    i dati dell'ordine
     * @param database     il database per salvare l'ordine
     * @param emailService il servizio email per inviare conferme
     * @return l'ordine salvato, o null se ci sono errori
     */
    public static OrderRecord processOrder(OrderData orderData, Database database, EmailService emailService) {
        // Validazione
        if (orderData.items().isEmpty()) {
            System.out.println("❌ ERRORE: Ordine vuoto");
            return null;
        }

        if (orderData.customerEmail() == null || orderData.customerEmail().isEmpty()) {
            System.out.println("❌ ERRORE: Email cliente mancante");
            return null;
        }

        if (orderData.customerName() == null || orderData.customerName().isEmpty()) {
            System.out.println("❌ ERRORE: Nome cliente mancante");
            return null;
        }

        for (Item item : orderData.items()) {
            if (item.price() <= 0) {
                System.out.println("❌ ERRORE: Prezzo non valido per " + item.name());
                return null;
            }
            if (item.quantity() <= 0) {
                System.out.println("❌ ERRORE: Quantità non valida per " + item.name());
                return null;
            }
        }

        // Calcolo del totale e applicazione di sconti
        double total = 0;
        for (Item item : orderData.items()) {
            total += item.price() * item.quantity();
        }

        // Sconto fedeltà cliente
        if (orderData.isVipCustomer()) {
            total = total * 0.85;
            System.out.println("✓ Sconto VIP applicato (15%)");
        }

        // Sconto ordini grandi
        if (total > 500) {
            total = total * 0.90;
            System.out.println("✓ Sconto ordine grande applicato (10%)");
        }

        // Calcolo tasse
        double tax = total * 0.22;
        double finalTotal = total + tax;

        System.out.printf("Totale: €%.2f%n", total);
        System.out.printf("Tasse (22%%): €%.2f%n", tax);
        System.out.printf("Totale finale: €%.2f%n", finalTotal);

        // Salvataggio nel database
        OrderRecord orderRecord = new OrderRecord(
                orderData.customerName(),
                orderData.customerEmail(),
                orderData.items(),
                total,
                tax,
                finalTotal,
                orderData.isVipCustomer(),
                "pending"
        );

        try {
            database.saveOrder(orderRecord);
            System.out.println("✓ Ordine salvato nel database");
        } catch (Exception e) {
            System.out.println("❌ ERRORE nel salvataggio: " + e.getMessage());
            return null;
        }

        // Invio email di conferma
        try {
            String emailSubject = String.format("Ordine confermato - €%.2f", finalTotal);
            StringBuilder emailBody = new StringBuilder();
            emailBody.append(String.format("""

                    Grazie %s!

                    Il tuo ordine è stato confermato.
                    Totale: €%.2f

                    Dettagli:
                    """, orderData.customerName(), finalTotal));

            for (Item item : orderData.items()) {
                emailBody.append(String.format("- %s x%d: €%.2f%n",
                        item.name(), item.quantity(), item.price() * item.quantity()));
            }

            emailService.send(orderData.customerEmail(), emailSubject, emailBody.toString());
            System.out.println("✓ Email di conferma inviata a " + orderData.customerEmail());
        } catch (Exception e) {
            System.out.println("❌ ERRORE nell'invio email: " + e.getMessage());
            // Nota: in questo caso continuiamo comunque (l'ordine è già salvato)
        }

        // Logging
        String logMessage = String.format("[ORDINE] %s - €%.2f - VIP: %b",
                orderData.customerName(), finalTotal, orderData.isVipCustomer());
        try (PrintWriter writer = new PrintWriter(new FileWriter("orders.log", true))) {
            writer.println(logMessage);
            System.out.println("✓ Ordine loggato");
        } catch (IOException e) {
            System.out.println("⚠️ AVVISO: Non è stato possibile loggare (" + e.getMessage() + ")");
        }

        return orderRecord;
    }

    // ==================== Esempio di utilizzo ====================

    /**
     * Database fasullo per l'esempio
     */
    static class MyDatabase implements Database {
        private final List<OrderRecord> orders = new java.util.ArrayList<>();

        @Override
        public void saveOrder(OrderRecord order) {
            orders.add(order);
        }

        public List<OrderRecord> getOrders() {
            return orders;
        }
    }

    /**
     * Servizio email fasullo per l'esempio
     */
    static class MyEmailService implements EmailService {
        @Override
        public void send(String to, String subject, String body) {
            System.out.println("\n📧 EMAIL INVIATA:");
            System.out.println("   A: " + to);
            System.out.println("   Oggetto: " + subject);
            System.out.println("   Corpo:\n" + body);
        }
    }

    public static void main(String[] args) {
        MyDatabase myDatabase = new MyDatabase();
        MyEmailService myEmail = new MyEmailService();

        // Ordine di esempio
        OrderData sampleOrder = new OrderData(
                "Mario Rossi",
                "mario@example.com",
                true,
                List.of(
                        new Item("Laptop", 800, 1),
                        new Item("Mouse", 25, 2)
                )
        );

        System.out.println("============================================================");
        System.out.println("INIZIO ELABORAZIONE ORDINE");
        System.out.println("============================================================");
        processOrder(sampleOrder, myDatabase, myEmail);
        System.out.println("\n============================================================");
        System.out.println("FINE ELABORAZIONE ORDINE");
        System.out.println("============================================================");
    }
}
