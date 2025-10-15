"""
Sfida 2: Single Responsibility Principle

Questo file contiene una funzione che viola il SRP.
Il tuo compito è spezzarla in funzioni più piccole e focalizzate.

Usa questo file come punto di partenza. Puoi modificarlo come preferisci,
aggiungere funzioni, cambiare la struttura - l'importante è che alla fine
il codice sia più leggibile e manutenibile.

"""


def process_order(order_data, database, email_service):
    """
    Processa un ordine dal caricamento alla notifica.
    ⚠️ ATTENZIONE: questa funzione fa TROPPO!
    """

    # Validazione
    if not order_data.get("items"):
        print("❌ ERRORE: Ordine vuoto")
        return None

    if not order_data.get("customer_email"):
        print("❌ ERRORE: Email cliente mancante")
        return None

    if not order_data.get("customer_name"):
        print("❌ ERRORE: Nome cliente mancante")
        return None

    for item in order_data["items"]:
        if item.get("price", 0) <= 0:
            print(f"❌ ERRORE: Prezzo non valido per {item.get('name')}")
            return None
        if item.get("quantity", 0) <= 0:
            print(f"❌ ERRORE: Quantità non valida per {item.get('name')}")
            return None

    # Calcolo del totale e applicazione di sconti
    total = 0
    for item in order_data["items"]:
        total += item["price"] * item["quantity"]

    # Sconto fedeltà cliente
    if order_data.get("is_vip_customer"):
        total = total * 0.85
        print("✓ Sconto VIP applicato (15%)")

    # Sconto ordini grandi
    if total > 500:
        total = total * 0.90
        print("✓ Sconto ordine grande applicato (10%)")

    # Calcolo tasse
    tax = total * 0.22
    final_total = total + tax

    print(f"Totale: €{total:.2f}")
    print(f"Tasse (22%): €{tax:.2f}")
    print(f"Totale finale: €{final_total:.2f}")

    # Salvataggio nel database
    order_record = {
        "customer_name": order_data["customer_name"],
        "customer_email": order_data["customer_email"],
        "items": order_data["items"],
        "subtotal": total,
        "tax": tax,
        "total": final_total,
        "vip": order_data.get("is_vip_customer", False),
        "status": "pending",
    }

    try:
        database.save_order(order_record)
        print("✓ Ordine salvato nel database")
    except Exception as e:
        print(f"❌ ERRORE nel salvataggio: {e}")
        return None

    # Invio email di conferma
    try:
        email_subject = f"Ordine confermato - €{final_total:.2f}"
        email_body = f"""
Grazie {order_data["customer_name"]}!

Il tuo ordine è stato confermato.
Totale: €{final_total:.2f}

Dettagli:
"""
        for item in order_data["items"]:
            email_body += f"- {item['name']} x{item['quantity']}: €{item['price'] * item['quantity']:.2f}\n"

        email_service.send(order_data["customer_email"], email_subject, email_body)
        print(f"✓ Email di conferma inviata a {order_data['customer_email']}")
    except Exception as e:
        print(f"❌ ERRORE nell'invio email: {e}")
        # Nota: in questo caso continuiamo comunque (l'ordine è già salvato)

    # Logging
    log_message = f"[ORDINE] {order_data['customer_name']} - €{final_total:.2f} - VIP: {order_data.get('is_vip_customer', False)}"
    try:
        with open("orders.log", "a") as log_file:
            log_file.write(log_message + "\n")
        print("✓ Ordine loggato")
    except Exception as e:
        print(f"⚠️ AVVISO: Non è stato possibile loggare ({e})")

    return order_record


# ==================== Esempio di utilizzo ====================

if __name__ == "__main__":
    # Simulazione di database e servizio email
    class MyDatabase:
        def __init__(self):
            self.orders = []

        def save_order(self, order):
            self.orders.append(order)

    my_database = MyDatabase()

    class MyEmailService:
        def send(self, to, subject, body):
            print("\n📧 EMAIL INVIATA:")
            print(f"   A: {to}")
            print(f"   Oggetto: {subject}")
            print(f"   Corpo:\n{body}")

    my_email = MyEmailService()

    # Ordine di esempio
    sample_order = {
        "customer_name": "Mario Rossi",
        "customer_email": "mario@example.com",
        "is_vip_customer": True,
        "items": [
            {"name": "Laptop", "price": 800, "quantity": 1},
            {"name": "Mouse", "price": 25, "quantity": 2},
        ],
    }

    print("=" * 60)
    print("INIZIO ELABORAZIONE ORDINE")
    print("=" * 60)
    result = process_order(sample_order, my_database, my_email)
    print("\n" + "=" * 60)
    print("FINE ELABORAZIONE ORDINE")
    print("=" * 60)
