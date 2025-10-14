import fs from 'fs';
import path from 'path';
import { describe, it, expect, beforeAll } from 'vitest';
import { calculateDiscount, calculateTotalOrder, getCustomerTierDescription } from './shop-discount';

describe('Shop Discount Tests', () => {

  describe('Discount Calculation: Testa che il calcolo degli sconti funzioni correttamente.', () => {

    it('Cliente base senza sconto di quantità', () => {
      const result = calculateDiscount(100, 'basic', 1);
      expect(result).toBe(100);
    });

    it('Cliente premium dovrebbe avere 15% di sconto', () => {
      const result = calculateDiscount(100, 'premium', 1);
      expect(result).toBe(85);
    });

    it('Cliente VIP dovrebbe avere 25% di sconto', () => {
      const result = calculateDiscount(100, 'vip', 1);
      expect(result).toBe(75);
    });

    it('5 prodotti = 5% di sconto aggiuntivo', () => {
      const result = calculateDiscount(100, 'basic', 5);
      const expected = 100 * 0.95;
      expect(result).toBe(expected);
    });

    it('10 prodotti = 10% di sconto aggiuntivo', () => {
      const result = calculateDiscount(100, 'basic', 10);
      const expected = 100 * 0.9;
      expect(result).toBe(expected);
    });

    it('Ordini > €100 ottengono 2% di sconto aggiuntivo', () => {
      const result = calculateDiscount(150, 'basic', 1);
      const expected = 150 * 0.98;
      expect(result).toBe(expected);
    });
  });

  describe('Total Order Calculation: Testa che il calcolo del totale dell\'ordine funzioni', () => {

    it('Ordine semplice di un cliente basic', () => {
      const products = [[50, 1]];
      const order = calculateTotalOrder(products, 'basic');
      expect(order.subtotal).toBe(50);
      expect(order.tax).toBe(Math.round(50 * 0.22 * 100) / 100);
    });

    it('Cliente premium dovrebbe avere sconto', () => {
      const products = [[100, 1]];
      const order = calculateTotalOrder(products, 'premium');
      // €100 * 0.85 (premium) = €85
      // Tasse: €85 * 0.22 = €18.70
      // Totale: €85 + €18.70 = €103.70
      expect(order.subtotal).toBe(85);
      const expected = Math.round((85 + 85 * 0.22) * 100) / 100;
      expect(order.total).toBe(expected);
    });

    it('Ordini > €500 ottengono ulteriore 5% di sconto', () => {
      const products = [[200, 3]]; // 3x €200 = €600
      const order = calculateTotalOrder(products, 'basic');
      // Subtotale: €600
      // Tasse: €600 * 0.22 = €132
      // Totale prima sconto: €732
      // Con sconto 5%: €732 * 0.98 * 0.95 = €681.49
      expect(order.discountApplied).toBe(true);
      const expected = Math.round(732 * 0.98 * 0.95 * 100) / 100;
      expect(order.total).toBe(expected);
    });
  });

  describe('Customer Tier Description: Testa le descrizioni dei tipi di cliente', () => {

    it('should return description containing "Base" for basic tier', () => {
      const result = getCustomerTierDescription('basic');
      expect(result).toContain('Base');
    });

    it('should return description containing "Premium" for premium tier', () => {
      const result = getCustomerTierDescription('premium');
      expect(result).toContain('Premium');
    });

    it('should return description containing "VIP" for vip tier', () => {
      const result = getCustomerTierDescription('vip');
      expect(result).toContain('VIP');
    });
  });

  describe('Code Quality: Test che verificano se hai estratto correttamente le costanti', () => {

    const sourceCodes = {};

    beforeAll(() => {
      // Read module file to analyze source code
      try {
        sourceCodes.calculateDiscount = getFunctionSource(calculateDiscount);
        sourceCodes.calculateTotalOrder = getFunctionSource(calculateTotalOrder);
        const sourcePath = path.join(__dirname, "shop-discount.js");
        sourceCodes.file = fs.readFileSync(sourcePath, 'utf-8');
      } catch (err) {
        console.warn('Warning: Could not read source for code quality checks:', err.message);
      }
    });

    /*
     * Questo test cerca costanti UPPER_CASE nel modulo.
     * Se questo fallisce, significa che non hai ancora estratto
     * tutte le costanti dal codice.
     */
    it('Verifica che il file contenga costanti ben nominate', () => {
      const source = sourceCodes.file;
      // Controlla che ci siano costanti (linee con `const CONST_CASE = numero/stringa`)
      const hasConstants = /^const\s+[A-Z_][A-Z0-9_]*\s*=/m.test(source);
      expect(
        hasConstants,
        'Non sembra che tu abbia estratto costanti ben nominate. Cerca valori come 0.85, 0.75, 100, 500, 0.22 e convertili in costanti CONST_CASE.',
      ).toBe(true);
    });

    /*
     * (Questo è un check semplice — se vedi numeri come 0.85, 0.75,
     * significa che non li hai ancora estratti a costanti!)
     */
    it('Controlla che la funzione calculateDiscount non contenga numeri magici schiantati', () => {
      const source = sourceCodes.calculateDiscount;

      // Lista di valori che DOVREBBERO essere stati estratti
      const magicValues = ['0.85', '0.75', '0.9', '0.95', '0.98', '10', '5', '100'];
      const foundMagic = magicValues.filter(v => source.includes(v));

      expect(
        foundMagic,
        `⚠️  Attenzione! Trovati numeri magici: ${foundMagic.join(', ')}. Converti questi numeri in costanti ben nominate (CONST_CASE)`,
      ).toEqual([]);
    });

    /*
     * (Questo è un check superficiale — si accerta solo che di non trovare i nomi originari,
     * assicurati però che i nomi da te scelti siano chiari e significativi!)
     */
    it('Controlla che la funzione calculateTotalOrder non contenga variabili di una sola lettera', () => {
      const source = sourceCodes.calculateTotalOrder;
      
      // Lista di nomi che DOVREBBERO essere stati cambiati
      const varNames = [...'dpqst'];
      // Trova istanze dei nomi circondati da caratteri non-parola
      const foundVars = varNames.filter(v => source.match(RegExp(`\\b${v}\\b`)));

      expect(
        foundVars,
        `⚠️  Attenzione! Trovate variabili di una lettera: ${foundVars.join(', ')}. Rinomina queste variabili con nomi significativi`,
      ).toEqual([]);
    });
  });
});

/**
 * Helper to extract function source code
 * 
 * @param {Function} functionRef
 */
function getFunctionSource(functionRef) {
  return functionRef.toString();
}
