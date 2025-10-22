# Sfida 3: (Inter)Facce Pulite - JavaScript Edition

## Antefatto: Nascondere segreti nelle immagini

Hai mai pensato a come nascondere informazioni dentro un'immagine senza che nessuno se ne accorga? La **steganografia LSB** (Least Significant Bit) è una tecnica affascinante che permette proprio questo.

In breve: ogni pixel di un'immagine PNG è composto da valori RGB (rosso, verde, blu), ciascuno da 0 a 255. Modificando solo l'ultimo bit (il *meno significativo*) di questi valori, l'immagine rimane visivamente identica all'occhio umano, ma può contenere dati nascosti! Ad esempio, il colore (255, 128, 64) può diventare (254, 129, 65) senza che tu noti alcuna differenza.

Questa tecnica è perfetta per incorporare metadati JSON in un'immagine PNG: titolo, autore, copyright, coordinate GPS, e qualsiasi altra informazione tu voglia "nascondere" nel file stesso.

**Vuoi saperne di più?** Leggi questo articolo introduttivo sulla steganografia LSB: [LSB Steganography — Hiding a message in the pixels of an image](https://medium.com/@renantkn/lsb-steganography-hiding-a-message-in-the-pixels-of-an-image-4722a8567046), oppure guarda questo video: [Secrets Hidden in Images (Steganography) - Computerphile](https://www.youtube.com/watch?v=TWEXCYQKyDc). Anche Wikipedia ha un articolo interessante: [Steganograpy - Wikipedia](https://en.wikipedia.org/wiki/Steganography) ma fa' attenzione a non perdertici!

---

## La Sfida

Hai ereditato una libreria JavaScript per gestire metadati nascosti nelle immagini PNG tramite steganografia LSB. La libreria permette di salvare e leggere dati JSON incorporati direttamente nei pixel, ma le sue interfacce sono... problematiche.

### Problemi da risolvere

1. **Parametri criptici:** chi usa la libreria deve conoscere flag numerici oscuri, passare dizionari con chiavi incomprensibili, ecc.
2. **Troppa responsabilità esposta:** l'utente deve gestire manualmente encoding, bit planes, formati di serializzazione
3. **Gestione errori inesistente:** ogni errore esplode in faccia all'utente in modi diversi e imprevedibili
4. **Nomi poco chiari:** `procMeta` fa troppo e non è chiaro cosa
5. **Nessun default sensato:** tutto deve essere specificato esplicitamente, anche le cose più ovvie
6. **Tipi di ritorno inconsistenti:** a seconda dell'operazione ricevi `object`, `boolean`, `string`, `null` o altri tipi casuali

### Obiettivo

Riprogetta l'interfaccia della libreria per renderla:
- **Intuitiva:** i nomi delle funzioni devono dire chiaramente cosa fanno
- **Semplice:** nascondi i dettagli implementativi (manipolazione dei bit, encoding, gestione degli header)
- **Robusta:** gestisci gli errori internamente, restituisci risultati sensati
- **Ben documentata:** aggiungi JSDoc chiari

### Cosa mantenere

- La logica interna può rimanere simile (non serve riscrivere tutto da zero)
- Crea funzioni helper private (basta non esportarle!) per tenere a bada la complessità
- Esponi alcune funzioni pubbliche con interfacce pulite
- **Mantieni le funzioni pubbliche pre-esistenti, anche se fanno schifo!** Puoi cambiarne l'implementazione affinché sia più pulita, ma non toccare l'interfaccia! Perché? [👇 Vedi sotto](#il-problema-del-cambiare-interfaccia)

### Suggerimenti

- Parti dalle funzioni che un utente della libreria vorrebbe usare: "Voglio salvare metadati in un'immagine", "Voglio leggere i metadati nascosti", ecc.
- Prendi spunto da come una libreria valida (come `pngjs`) gestisce la propria interfaccia. Non serve reimplementare tutto (sarebbe troppo complicato), ma puoi farti delle domande e trovare risposte interessanti nel codice altrui!
- Pensa ai casi d'uso comuni e rendili semplici; i casi rari possono richiedere più configurazione, comunque opzionale
- Usa i JSDocs per specificare type hints (`@param {type} name Description`, ad esempio) per rendere chiaro cosa aspettarsi
- Le costanti (come i numeri del formato) devono restare rinchiuse **dentro** la libreria, non vanno richieste all'utente
- Considera di restituire oggetti con dati strutturati invece di valori sparsi

## Come partecipare

1. Clona il repository
2. Crea un branch: `git switch -c sfida3-tuonome`
3. Modifica `challenge-3/javascript/steganography.js`
4. Fai il commit: `git commit -m "Sfida 3: Redesign interface"`
5. Apri una Pull Request nel repository principale
6. Aspetta il feedback!

## Eseguire il programma

Il file `steganography.js` non è eseguibile in quanto è una libreria. Puoi realizzare un programma di test per tuo conto, ma probabilmente il modo migliore di vedere come funziona (e **se** funziona) è leggendo ed eseguendo i test.

Ciò aumenta un poco la difficoltà di questa sfida, ma non ti preoccupare! Lavorare a questa sfida sarà un'esperienza nuova, che darà alla tua abilità di programmazione una marcia in più 🏎️

Detto questo...

## Testare la libreria

Per prima cosa assicurati che [Node.js sia installato](https://www.geeksforgeeks.org/installation-guide/install-node-js-windows-macos-linux/) sul PC locale.

La nostra libreria dipende da alcune librerie esterne, per cui normalmente dovresti lanciare alcuni comandi per impostare l'ambiente locale. Per semplificarti il lavoro è tutto incluso in un piccolo script...

Per validare automaticamente il tuo lavoro, basta lanciare il comando:

```bash
node challenge-3/javascript
```

> ℹ️ Potrebbe impiegare un po' più di tempo la prima volta che lo esegui, perché deve scaricare le librerie esterne.

I test verificano che il comportamento del codice sia rimasto uguale e copre in parte le nuove funzioni che saranno parte della nuova interfaccia. **Fai affidamento su di essi — ti faranno da guida per capire cosa funziona e cosa no!**

**Attenzione:** Non vale modificare il file con i test per far sì che passino! Tuttavia è OK aggiungere nuovi test per testare le nuove funzioni dell'interfaccia migliorata 👍

### Il problema del cambiare interfaccia

Se esegui i test, noterai che in parte sono scritti **per la VECCHIA interfaccia** e altri sono pensati per la nuova.

Forse stai pensando che una volta scritto il nuovo codice, puoi liberarti del vecchio e anche dei test corrispondenti...

Purtroppo non è così semplice: cambiare l'interfaccia quando è già in uso è costoso e doloroso. Chi usa la tua libreria vedrà il codice rompersi dopo un aggiornamento di versione. Per questo motivo:

1. **Pensa bene all'interfaccia PRIMA** di pubblicare la versione `1.0.0`
2. **Aggiungi nuove funzioni invece di modificare le esistenti**, fornendo così un nuovo percorso, più pulito, che possa sostituire il vecchio
3. Sfrutta i **deprecation warnings**: usa `@deprecated` nei JSDoc e avvisa i tuoi utenti, con qualche versione di anticipo, che presto una certa interfaccia non sarà più valida e gli conviene migrare alla nuova.
