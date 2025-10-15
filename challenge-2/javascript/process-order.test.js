import fs from 'fs';
import path from 'path';
import { describe, it, expect, beforeEach } from 'vitest';
import { processOrder, MyDatabase, MyEmailService } from './process-order';

// Mock Database per i test
class MockDatabase {
  constructor() {
    this.orders = [];
    this._failNext = false;
  }

  saveOrder(order) {
    if (this._failNext) {
      this._failNext = false;
      throw new Error('[Test] database save failure');
    }
    this.orders.push(order);
  }
}

// Mock EmailService per i test
class MockEmailService {
  constructor() {
    this.sentEmails = [];
    this._failNext = false;
  }

  send(to, subject, body) {
    if (this._failNext) {
      this._failNext = false;
      throw new Error('[Test] email send failure');
    }
    this.sentEmails.push({ to, subject, body });
  }
}

describe('Process Order Tests', () => {

  describe('Test: la validazione deve funzionare', () => {
    let mockDatabase;
    let mockEmail;

    beforeEach(() => {
      mockDatabase = new MockDatabase();
      mockEmail = new MockEmailService();
    });

    it('Ordine senza articoli', () => {
      const result = processOrder(
        {
          customerName: 'Mario',
          customerEmail: 'mario@test.com',
          items: [],
        },
        mockDatabase,
        mockEmail
      );
      expect(result).toBeNull();
    });

    it('Ordine senza email', () => {
      const result = processOrder(
        {
          customerName: 'Mario',
          items: [{ name: 'Prodotto', price: 10, quantity: 1 }],
        },
        mockDatabase,
        mockEmail
      );
      expect(result).toBeNull();
    });
  });

  describe('Test: il calcolo del totale deve essere corretto', () => {
    let mockDatabase;
    let mockEmail;

    beforeEach(() => {
      mockDatabase = new MockDatabase();
      mockEmail = new MockEmailService();
    });

    it('Il totale deve essere calcolato correttamente', () => {
      const order = {
        customerName: 'Mario',
        customerEmail: 'mario@test.com',
        isVipCustomer: false,
        items: [
          { name: 'Prodotto A', price: 100, quantity: 2 },  // 200
          { name: 'Prodotto B', price: 50, quantity: 1 },   // 50
        ],
      };
      // Subtotale: 250, nessuno sconto, tasse 22%
      // Totale: 250 * 1.22 = 305

      const result = processOrder(order, mockDatabase, mockEmail);
      expect(result).not.toBeNull();
      expect(result.subtotal).toBe(250);
      expect(result.total).toBeCloseTo(305, 1);
    });

    it('Lo sconto VIP deve essere applicato correttamente', () => {
      // Ordine VIP di €100
      const order = {
        customerName: 'Mario VIP',
        customerEmail: 'mario@test.com',
        isVipCustomer: true,
        items: [
          { name: 'Prodotto', price: 100, quantity: 1 },
        ],
      };
      // Subtotale: 100, sconto VIP 15% = 85, tasse 22% = 103.7

      const result = processOrder(order, mockDatabase, mockEmail);
      expect(result).not.toBeNull();
      expect(result.vip).toBe(true);
      expect(result.subtotal).toBe(85);
    });

    it('Lo sconto per ordini grandi deve essere applicato', () => {
      // Ordine di €600 (senza VIP)
      const order = {
        customerName: 'Mario Grosso',
        customerEmail: 'mario@test.com',
        isVipCustomer: false,
        items: [
          { name: 'Prodotto Caro', price: 600, quantity: 1 },
        ],
      };
      // Subtotale: 600, sconto grandi ordini 10% = 540, tasse 22% = 658.8

      const result = processOrder(order, mockDatabase, mockEmail);
      expect(result).not.toBeNull();
      expect(result.subtotal).toBe(540);
    });
  });

  describe('Test: l\'ordine deve essere salvato nel database', () => {
    let mockDatabase;
    let mockEmail;

    beforeEach(() => {
      mockDatabase = new MockDatabase();
      mockEmail = new MockEmailService();
    });

    it('L\'ordine viene salvato correttamente', () => {
      const order = {
        customerName: 'Mario',
        customerEmail: 'mario@test.com',
        items: [
          { name: 'Prodotto', price: 50, quantity: 1 },
        ],
      };

      const result = processOrder(order, mockDatabase, mockEmail);
      expect(result).not.toBeNull();
      expect(mockDatabase.orders.length).toBe(1);

      const saved = mockDatabase.orders[0];
      expect(saved.customerName).toBe('Mario');
      expect(saved.customerEmail).toBe('mario@test.com');
      expect(saved.status).toBe('pending');
      expect(saved.subtotal).toBe(50);
      expect(saved.tax).toBe(11);
      expect(saved.total).toBe(61);
      expect(saved.vip).toBe(false);
    });

    it('Se l\'ordine non viene salvato, la funzione non ritorna niente', () => {
      const order = {
        customerName: 'Mario',
        customerEmail: 'mario@test.com',
        items: [
          { name: 'Prodotto', price: 50, quantity: 1 },
        ],
      };

      mockDatabase._failNext = true;

      const result = processOrder(order, mockDatabase, mockEmail);
      expect(result).toBeNull();
      expect(mockDatabase.orders.length).toBe(0);
    });
  });

  describe('Test: Dopo l\'ordine deve essere inviata un\'email di conferma', () => {
    let mockDatabase;
    let mockEmail;

    beforeEach(() => {
      mockDatabase = new MockDatabase();
      mockEmail = new MockEmailService();
    });

    it('L\'email di conferma viene inviata correttamente', () => {
      const order = {
        customerName: 'Mario',
        customerEmail: 'mario@test.com',
        items: [
          { name: 'Prodotto', price: 50, quantity: 1 },
        ],
      };

      const result = processOrder(order, mockDatabase, mockEmail);

      expect(result).not.toBeNull();
      expect(mockEmail.sentEmails.length).toBe(1);

      const email = mockEmail.sentEmails[0];
      expect(email.to).toBe('mario@test.com');
      expect(email.subject).toBe('Ordine confermato - €61.00');
      
      const expectedBody = `
Grazie Mario!

Il tuo ordine è stato confermato.
Totale: €61.00

Dettagli:
- Prodotto x1: €50.00
`;
      expect(email.body).toBe(expectedBody);
    });

    it('Se il servizio email ritorna un errore, l\'ordine risulta comunque accettato', () => {
      const order = {
        customerName: 'Mario',
        customerEmail: 'mario@test.com',
        items: [
          { name: 'Prodotto', price: 50, quantity: 1 },
        ],
      };

      mockEmail._failNext = true;

      const result = processOrder(order, mockDatabase, mockEmail);
      expect(result).not.toBeNull();
      expect(mockEmail.sentEmails.length).toBe(0);
    });
  });

  describe('Test: L\'ordine deve essere loggato', () => {
    let mockDatabase;
    let mockEmail;

    beforeEach(() => {
      mockDatabase = new MockDatabase();
      mockEmail = new MockEmailService();
    });

    it('L\'ordine viene loggato correttamente', () => {
      const order = {
        customerName: 'Mario',
        customerEmail: 'mario@test.com',
        items: [
          { name: 'Prodotto', price: 50, quantity: 1 },
        ],
      };

      // Tronchiamo il file di log... vero che ci starebbe bene una costante qui?
      fs.truncateSync('orders.log');

      const result = processOrder(order, mockDatabase, mockEmail);
      expect(result).not.toBeNull();

      const logged = fs.readFileSync('orders.log', 'utf-8');
      expect(logged).toBe('[ORDINE] Mario - €61.00 - VIP: false\n');
    });
  });

  const expectedHelpers = [
    'validateOrder',
    'calculateTotals',
    'saveOrder',
    'sendConfirmation',
    'logOrder',
  ];

  describe('Test che verificano se hai riorganizzato correttamente il codice', () => {

    it('Verifica che il file contenga delle funzioni helper per incapsulare ciascuna responsabilità', () => {
      /*
       * Questo test cerca delle funzioni con nomi scelti arbitrariamente,
       * niente affatto obbligatori!
       * Se hai usato nomi diversi, modifica la costante expectedHelpers
       * nel file di test per far passare questo test.
       */
      const sourcePath = path.join(__dirname, 'process-order.js');
      const source = fs.readFileSync(sourcePath, 'utf-8');

      const missingHelpers = expectedHelpers.filter(helper => 
        !source.includes(`function ${helper}`)
      );

      expect(
        missingHelpers,
        `⚠️  Attenzione! Mancano alcune funzioni attese: ${missingHelpers.join(', ')}. ` +
        'Spezza `processOrder` in funzioni con responsabilità singola.'
      ).toEqual([]);
    });

    it('Verifica che la funzione `processOrder` esista ancora', () => {
      const sourcePath = path.join(__dirname, 'process-order.js');
      const source = fs.readFileSync(sourcePath, 'utf-8');

      expect(
        source.includes('function processOrder'),
        '❌  Non hai più la funzione `ProcessOrder`. ' +
        'Dovrebbe restare come coordinatore che chiama le funzioni più piccole.'
      ).toBe(true);
    });

    it('Verifica che la firma della funzione `processOrder` non sia cambiata', () => {
      const source = processOrder.toString();

      // Estrae la lista dei parametri dalla funzione `processOrder`
      const [, paramsString] = source.match(/function\s+processOrder\s*\(\s*([^)]*)\s*\)/);
      expect(paramsString).toBeTruthy();
      
      const params = paramsString.split(/\s*,\s*/).filter(p => p);
      expect(
        params,
        '❌  Parametri errati in `processOrder`: dovrebbe avere 3 parametri (orderData, database, emailService)'
      ).toEqual(['orderData', 'database', 'emailService']);
    });

    it('Verifica che le funzioni helper aggiunte siano documentate', () => {
      /*
       * Verifica che le funzioni "helper" aggiunte siano documentate.
       */
      const sourcePath = path.join(__dirname, 'process-order.js');
      const source = fs.readFileSync(sourcePath, 'utf-8');

      for (const funcName of expectedHelpers) {
        if (source.includes(`function ${funcName}`)) {
          // Cerca un commento JSDoc prima della funzione
          const funcPattern = new RegExp(`/\\*\\*[\\s\\S]*?\\*/\\s*function\\s+${funcName}`, 'm');
          expect(
            funcPattern.test(source),
            `⚠️  Aggiungi un commento JSDoc a \`${funcName}\` per descriverne la responsabilità.`
          ).toBe(true);
        }
      }
    });

    it('Verifica che ciascuna funzione helper sia usata nella funzione `processOrder`', () => {
      const source = processOrder.toString();
      const iStartBrace = source.indexOf('{');
      const iEndBrace = source.lastIndexOf('}');
      const processOrderBody = source.slice(iStartBrace, iEndBrace);

      const missingCalls = expectedHelpers.filter(name => {
        // Verifica se la funzione esiste nel file
        if (source.includes(`function ${name}`)) {
          // Verifica se è chiamata in processOrder
          return !processOrderBody.includes(name);
        }
        return false;
      });

      expect(
        missingCalls,
        `⚠️  Alcune funzioni non sono richiamate da \`processOrder\`: ${missingCalls.join(', ')}`
      ).toEqual([]);
    });
  });
});
