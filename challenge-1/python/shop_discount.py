"""
Sistema di gestione sconti per uno shop online.

Il codice funziona, ma è pieno di magic numbers e magic strings.
Estrai tutte le costanti e dai loro nomi significativi!
"""


def calculate_discount(price, customer_type, quantity):
    """
    Calcola il prezzo finale con sconto.
    
    Args:
        price: prezzo unitario del prodotto
        customer_type: tipo di cliente ("basic", "premium", "vip")
        quantity: quantità di prodotti acquistati
    
    Returns:
        prezzo finale dopo applicazione di tutti gli sconti
    """
    
    # Sconto per clienti premium
    if customer_type == "premium":
        price = price * 0.85
    
    # Sconto per clienti VIP
    if customer_type == "vip":
        price = price * 0.75
    
    # Sconto per quantità
    if quantity >= 10:
        price = price * 0.9
    elif quantity >= 5:
        price = price * 0.95
    
    # Sconto minimo garantito per ordini grandi
    if price > 100:
        price = price * 0.98
    
    return round(price, 2)


def calculate_total_order(products, customer_type):
    """
    Calcola il totale di un ordine applicando sconti per cliente e tasse.
    
    Args:
        products: lista di tuple (price, quantity)
        customer_type: tipo di cliente ("basic", "premium", "vip")
    
    Returns:
        dizionario con subtotale, tasse e totale finale
    """
    
    s = 0  # subtotale
    
    for p, q in products:
        d = calculate_discount(p, customer_type, q)
        s = s + (d * q)
    
    # Tassa sul valore
    t = s * 0.22  # 22% di tassa
    
    tot = s + t
    
    # Sconto finale se l'ordine è superiore a 500 euro
    if tot > 500:
        tot = tot * 0.95
    
    return {
        "subtotal": round(s, 2),
        "tax": round(t, 2),
        "total": round(tot, 2),
        "discount_applied": tot != s + t
    }


def get_customer_tier_description(c_type):
    """
    Ritorna una descrizione del livello cliente.
    """
    
    if c_type == "basic":
        return "Cliente Base"
    elif c_type == "premium":
        return "Cliente Premium (15% sconto base)"
    elif c_type == "vip":
        return "Cliente VIP (25% sconto base)"
    else:
        return "Tipo cliente sconosciuto"


def format_order_receipt(order_data, customer_type):
    """
    Formatta uno scontrino leggibile.
    """
    
    receipt = ""
    receipt += "=" * 40 + "\n"
    receipt += "SCONTRINO ORDINE\n"
    receipt += "=" * 40 + "\n"
    receipt += f"Tipo cliente: {get_customer_tier_description(customer_type)}\n"
    receipt += "-" * 40 + "\n"
    receipt += f"Subtotale: € {order_data['subtotal']}\n"
    receipt += f"Tasse (22%): € {order_data['tax']}\n"
    
    if order_data["discount_applied"]:
        receipt += "SCONTO PER ORDINE GRANDE: -5%\n"
    
    receipt += "-" * 40 + "\n"
    receipt += f"TOTALE: € {order_data['total']}\n"
    receipt += "=" * 40 + "\n"
    
    return receipt


# Esempio di utilizzo
if __name__ == "__main__":
    products = [
        (50, 2),  # 2x prodotto da €50
        (30, 8),  # 8x prodotto da €30
    ]
    
    order = calculate_total_order(products, "premium")
    print(format_order_receipt(order, "premium"))
    
    print("\nAltro esempio:")
    products2 = [(100, 3), (50, 5)]
    order2 = calculate_total_order(products2, "vip")
    print(format_order_receipt(order2, "vip"))
