"use strict"
/**
 * Sistema di gestione sconti per uno shop online.
 * 
 * Il codice funziona, ma è pieno di magic numbers e magic strings.
 * Estrai tutte le costanti e dai loro nomi significativi!
 */

/**
 * Calcola il prezzo finale con sconto.
 * 
 * @param {number} price - prezzo unitario del prodotto
 * @param {string} customerType - tipo di cliente ("basic", "premium", "vip")
 * @param {number} quantity - quantità di prodotti acquistati
 * @returns {number} prezzo finale dopo applicazione di tutti gli sconti
 */
function calculateDiscount(price, customerType, quantity) {
  // Sconto per clienti premium
  if (customerType === "premium") {
    price = price * 0.85;
  }

  // Sconto per clienti VIP
  if (customerType === "vip") {
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

  return Math.round(price * 100) / 100;
}

/**
 * Calcola il totale di un ordine applicando sconti per cliente e tasse.
 * 
 * @param {Array<[number, number]>} products - lista di coppie [price, quantity]
 * @param {string} customerType - tipo di cliente ("basic", "premium", "vip")
 * @returns {Object} oggetto con subtotale, tasse e totale finale
 */
function calculateTotalOrder(products, customerType) {
  let s = 0;

  for (const [p, q] of products) {
    const d = calculateDiscount(p, customerType, q);
    s = s + (d * q);
  }

  // Tassa sul valore
  const t = s * 0.22;

  let tot = s + t;

  // Sconto finale se l'ordine è superiore a 500 euro
  if (tot > 500) {
    tot = tot * 0.95;
  }

  return {
    subtotal: Math.round(s * 100) / 100,
    tax: Math.round(t * 100) / 100,
    total: Math.round(tot * 100) / 100,
    discountApplied: tot !== s + t
  };
}

/**
 * Ritorna una descrizione del livello cliente.
 */
function getCustomerTierDescription(cType) {
  if (cType === "basic") {
    return "Cliente Base";
  } else if (cType === "premium") {
    return "Cliente Premium (15% sconto base)";
  } else if (cType === "vip") {
    return "Cliente VIP (25% sconto base)";
  } else {
    return "Tipo cliente sconosciuto";
  }
}

/**
 * Formatta uno scontrino leggibile.
 */
function formatOrderReceipt(orderData, customerType) {
  let receipt = "";
  receipt += "=".repeat(40) + "\n";
  receipt += "SCONTRINO ORDINE\n";
  receipt += "=".repeat(40) + "\n";
  receipt += `Tipo cliente: ${getCustomerTierDescription(customerType)}\n`;
  receipt += "-".repeat(40) + "\n";
  receipt += `Subtotale: € ${orderData.subtotal}\n`;
  receipt += `Tasse (22%): € ${orderData.tax}\n`;

  if (orderData.discountApplied) {
    receipt += "SCONTO PER ORDINE GRANDE: -5%\n";
  }

  receipt += "-".repeat(40) + "\n";
  receipt += `TOTALE: € ${orderData.total}\n`;
  receipt += "=".repeat(40) + "\n";

  return receipt;
}

// Esempio di utilizzo
if (require.main === module) {
  const products = [
    [50, 2], // 2x prodotto da €50
    [30, 8], // 8x prodotto da €30
  ];

  const order = calculateTotalOrder(products, "premium");
  console.log(formatOrderReceipt(order, "premium"));

  console.log("\nAltro esempio:");
  const products2 = [[100, 3], [50, 5]];
  const order2 = calculateTotalOrder(products2, "vip");
  console.log(formatOrderReceipt(order2, "vip"));
}

module.exports = {
  calculateDiscount,
  calculateTotalOrder,
  getCustomerTierDescription,
  formatOrderReceipt,
};