# Sfida 3: (Inter)Facce Pulite

## La teoria: Interfacce semplici per complessità nascoste

Quando scriviamo una libreria o un modulo, stiamo creando uno **strumento per altri sviluppatori**. L'interfaccia di questo strumento - i parametri delle funzioni, i nomi, cosa restituiscono - è il *contratto* che offriamo al mondo esterno.

### Il problema delle interfacce complesse

Un'interfaccia mal progettata costringe chi la usa a:
- Conoscere troppi dettagli implementativi
- Gestire casi limite che dovrebbero essere trasparenti
- Passare parametri criptici o troppo numerosi
- Ricordarsi "regole nascoste" non evidenti dal nome della funzione

In ogni caso, l'utente della funzione deve conoscere elementi non immediatamente desumibili dal contesto in cui la funzione viene chiamata.  
Deve per così dire lasciare il mondo del proprio codice ed entrare nel mondo della funzione che sta usando...

### I principi di una buona interfaccia

1. **Information Hiding:** nascondi i dettagli, esponi solo l'essenziale
2. **Autoesplicativa:** il nome e i parametri devono essere chiari senza documentazione
3. **Default robusti:** applica parametri predefiniti sensati
4. **Fallire con grazia:** gestisci gli errori in modo chiaro e prevedibile
5. **Stabile:** una volta pubblicata, un'interfaccia è difficile da cambiare

### Esempio rapido

❌ **Male** - interfaccia complessa che lascia trapelare troppi dettagli:
```javascript
function processImg(path, doExif=true, exifTag=274, rotations={3:180, 6:270, 8:90}, 
  resize=null, format=null, quality=85, raiseOnError=false) {
  // ...
}
```

✅ **Bene** - interfaccia semplice e chiara:
```javascript
/**
 * Prepara un'immagine per la visualizzazione web.
 *
 * Ruota automaticamente l'immagine secondo i metadati EXIF,
 * la ridimensiona e restituisce il path dell'immagine processata.
 * 
 * @param {string} imagePath Percorso dell'immagine
 * @param {int=} maxSize Larghezza massima dell'immagine
 */
function prepareImageForDisplay(imagePath, maxSize = 800) {
  // ...
}
```

## Link per approfondimenti

- [Information Hiding Principle](https://en.wikipedia.org/wiki/Information_hiding)
- [Difference Between Information Hiding and Encapsulation](https://www.baeldung.com/java-information-hiding-vs-encapsulation)
- [Using Python Optional Arguments When Defining Functions](https://realpython.com/python-optional-arguments/), lettura valida per tutti, non solo sviluppatori Python!

## La sfida

Scegli un linguaggio. La sfida è la stessa indipendentemente dal liguaggio scelto, cambia solo la sintassi!

* [Java](./java)
* [JavaScript](./javascript)
* [Python](./python)
* [Go](./go)

---

**Ricorda:** Non è una competizione di velocità. Prenditi il tempo di capire PERCHÉ ogni magic number deve diventare una costante. Quello che impari oggi lo porterai nella tua programmazione per sempre. 🚀