# Sfida 1: Clear & Constant

## La teoria: Magic Numbers, Magic Strings e il principio DRY

Immagina di aprire il codice di un collega e trovare:

```java
price = price * 0.85;
if (amount > 100) {
    price = price * 0.95;
}
if (status == "premium") {
    price = price * 0.90;
}
```

Cosa significano questi numeri? `0.85`? `100`? `0.90`? Perché proprio questi valori?

Il principio **DRY** (Don't Repeat Yourself) dice: non ripetere la stessa informazione, la stessa logica, o la stessa "intenzione" nel codice. E questo vale anche per i cosiddetti **magic numbers** e i **magic strings** — quei valori che appaiono "schiantati" nel codice senza spiegazione.

### Perché è importante?

1. **Leggibilità:** `DISCOUNT_RATE_STANDARD` esprime un intento più chiaro rispetto a `0.85`
2. **Manutenibilità:** se il tasso di sconto cambia, lo modifichi in UN posto, non dieci
3. **Riduzione degli errori:** eviti di copiare il valore sbagliato in qualche punto del codice
4. **Auto-esplicativo:** il nome della costante SPIEGA cosa rappresenta, il numero da solo, no

### Costanti vs Magic Numbers

❌ Male:
```java
double total = price * 0.85 // Cosa è questo 0.85?
```

✅ Bene:
```java
private static final double STANDARD_DISCOUNT = 0.85
// ...
double total = price * STANDARD_DISCOUNT // Chiaro!
```

Diversi linguaggi hanno diverse convenzioni per la definizione delle constanti, per esempio:
* **Java:** `public static final double STANDARD_DISCOUNT = 0.85`
* **JavaScript:** `const STANDARD_DISCOUNT = 0.85`
* **Python:** `STANDARD_DISCOUNT = 0.85`
* **Go:** `const StandardDiscount = 0.85`

## Approfondimenti

- [DRY Principle - Baeldung](https://www.baeldung.com/cs/dry-software-design-principle)
- [Magic Numbers and Magic Strings - Wikipedia](https://it.wikipedia.org/wiki/Magic_number)
- [Clean Code: Meaningful Names](https://www.cwblogs.com/posts/clean-code-meaningful-names/)

## La Sfida

Scegli un linguaggio. La sfida è la stessa indipendentemente dal liguaggio scelto, cambia solo la sintassi!

* [Java](./java)
* [JavaScript](./javascript)
* [Python](./python)
* [Go](./go)

---

**Ricorda:** Non è una competizione di velocità. Prenditi il tempo di capire PERCHÉ ogni magic number deve diventare una costante. Quello che impari oggi lo porterai nella tua programmazione per sempre. 🚀