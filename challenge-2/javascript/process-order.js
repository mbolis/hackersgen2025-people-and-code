"use strict"
/**
 * Sfida 2: Single Responsibility Principle
 * 
 * Questo file contiene una funzione che viola il SRP.
 * Il tuo compito è spezzarla in funzioni più piccole e focalizzate.
 * 
 * Usa questo file come punto di partenza. Puoi modificarlo come preferisci,
 * aggiungere funzioni, cambiare la struttura - l'importante è che alla fine
 * il codice sia più leggibile e manutenibile.
 */

const fs = require('fs');

/**
 * Processa un ordine dal caricamento alla notifica.
 * ⚠️ ATTENZIONE: questa funzione fa TROPPO!
 * 
 * @param {Object} orderData - i dati dell'ordine
 * @param {Object} database - il database per salvare l'ordine
 * @param {Object} emailService - il servizio email per inviare conferme
 * @returns {Object|null} l'ordine salvato, o null se ci sono errori
 */
function processOrder(orderData, database, emailService) {
  // Validazione
  if (!orderData.items || orderData.items.length === 0) {
    console.log("❌ ERRORE: Ordine vuoto");
    return null;
  }

  if (!orderData.customerEmail) {
    console.log("❌ ERRORE: Email cliente mancante");
    return null;
  }

  if (!orderData.customerName) {
    console.log("❌ ERRORE: Nome cliente mancante");
    return null;
  }

  for (const item of orderData.items) {
    if ((item.price || 0) <= 0) {
      console.log(`❌ ERRORE: Prezzo non valido per ${item.name}`);
      return null;
    }
    if ((item.quantity || 0) <= 0) {
      console.log(`❌ ERRORE: Quantità non valida per ${item.name}`);
      return null;
    }
  }

  // Calcolo del totale e applicazione di sconti
  let total = 0;
  for (const item of orderData.items) {
    total += item.price * item.quantity;
  }

  // Sconto fedeltà cliente
  if (orderData.isVipCustomer) {
    total = total * 0.85;
    console.log("✓ Sconto VIP applicato (15%)");
  }

  // Sconto ordini grandi
  if (total > 500) {
    total = total * 0.90;
    console.log("✓ Sconto ordine grande applicato (10%)");
  }

  // Calcolo tasse
  const tax = total * 0.22;
  const finalTotal = total + tax;

  console.log(`Totale: €${total.toFixed(2)}`);
  console.log(`Tasse (22%): €${tax.toFixed(2)}`);
  console.log(`Totale finale: €${finalTotal.toFixed(2)}`);

  // Salvataggio nel database
  const orderRecord = {
    customerName: orderData.customerName,
    customerEmail: orderData.customerEmail,
    items: orderData.items,
    subtotal: total,
    tax: tax,
    total: finalTotal,
    vip: orderData.isVipCustomer || false,
    status: "pending",
  };

  try {
    database.saveOrder(orderRecord);
    console.log("✓ Ordine salvato nel database");
  } catch (e) {
    console.log(`❌ ERRORE nel salvataggio: ${e.message}`);
    return null;
  }

  // Invio email di conferma
  try {
    const emailSubject = `Ordine confermato - €${finalTotal.toFixed(2)}`;
    let emailBody = `
Grazie ${orderData.customerName}!

Il tuo ordine è stato confermato.
Totale: €${finalTotal.toFixed(2)}

Dettagli:
`;
    for (const item of orderData.items) {
      emailBody += `- ${item.name} x${item.quantity}: €${(item.price * item.quantity).toFixed(2)}\n`;
    }

    emailService.send(orderData.customerEmail, emailSubject, emailBody);
    console.log(`✓ Email di conferma inviata a ${orderData.customerEmail}`);
  } catch (e) {
    console.log(`❌ ERRORE nell'invio email: ${e.message}`);
    // Nota: in questo caso continuiamo comunque (l'ordine è già salvato)
  }

  // Logging
  const logMessage = `[ORDINE] ${orderData.customerName} - €${finalTotal.toFixed(2)} - VIP: ${orderData.isVipCustomer || false}`;
  try {
    fs.appendFileSync("orders.log", logMessage + "\n");
    console.log("✓ Ordine loggato");
  } catch (e) {
    console.log(`⚠️ AVVISO: Non è stato possibile loggare (${e.message})`);
  }

  return orderRecord;
}

// ==================== Esempio di utilizzo ====================

/**
 * Database fasullo per l'esempio
 */
class MyDatabase {
  constructor() {
    this.orders = [];
  }

  saveOrder(order) {
    this.orders.push(order);
  }
}

/**
 * Servizio email fasullo per l'esempio
 */
class MyEmailService {
  send(to, subject, body) {
    console.log("\n📧 EMAIL INVIATA:");
    console.log(`   A: ${to}`);
    console.log(`   Oggetto: ${subject}`);
    console.log(`   Corpo:\n${body}`);
  }
}

if (require.main === module) {
  const myDatabase = new MyDatabase();
  const myEmail = new MyEmailService();

  // Ordine di esempio
  const sampleOrder = {
    customerName: "Mario Rossi",
    customerEmail: "mario@example.com",
    isVipCustomer: true,
    items: [
      { name: "Laptop", price: 800, quantity: 1 },
      { name: "Mouse", price: 25, quantity: 2 },
    ],
  };

  console.log("============================================================");
  console.log("INIZIO ELABORAZIONE ORDINE");
  console.log("============================================================");
  processOrder(sampleOrder, myDatabase, myEmail);
  console.log("\n============================================================");
  console.log("FINE ELABORAZIONE ORDINE");
  console.log("============================================================");
}

module.exports = {
  processOrder,
  MyDatabase,
  MyEmailService,
};
