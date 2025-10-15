# Sfida 3: (Inter)Facce Pulite

Hai ereditato una libreria Python per processare immagini. La libreria fa da wrapper a PIL (Pillow), ma le sue interfacce sono... problematiche.

### Problemi da risolvere

1. **Parametri criptici:** chi usa la libreria deve conoscere tag EXIF numerici, passare oggetti dal significato non ovvio, ecc.
2. **Troppa responsabilità esposta:** l'utente deve gestire manualmente orientamento, ridimensionamento, formato di output
3. **Gestione errori inesistente:** ogni errore esplode in faccia all'utente
4. **Nomi poco chiari:** `process_image` fa troppo e non è chiaro cosa
5. **Nessun default sensato:** tutto deve essere specificato esplicitamente

### Obiettivo

Riprogetta l'interfaccia della libreria per renderla:
- **Intuitiva:** i nomi delle funzioni devono dire chiaramente cosa fanno
- **Semplice:** nascondi i dettagli implementativi (tag EXIF, logiche interne)
- **Robusta:** gestisci gli errori internamente, restituisci risultati sensati
- **Ben documentata:** aggiungi docstring chiare

### Cosa mantenere

- La logica interna può rimanere simile (non serve riscrivere tutto da zero)
- Crea funzioni helper private (con `_` all'inizio del nome) per la complessità
- Esponi solo 2-3 funzioni pubbliche con interfacce pulite
- Mantieni le funzioni pubbliche pre-esistenti, anche se fanno schifo! Perché? [👇 Vedi sotto](#il-problema-del-cambiare-interfaccia)

### Suggerimenti

- Parti dalle funzioni che un utente della libreria vorrebbe usare: "Voglio ruotare un'immagine", "Voglio creare una thumbnail", ecc.
- Prendi spunto da come una libreria valida (come Pillow) gestisce la propria interfaccia. Non serve reimplementare tutto (sarebbe troppo complicato), ma puoi farti delle domande e trovare risposte interessanti nel codice altrui!
- Pensa ai casi d'uso comuni e rendili semplici; i casi rari possono richiedere più configurazione, comunque opzionale
- Usa type hints (`: str`, `: int`, ecc.) per rendere chiaro cosa aspettarsi
- Le costanti (come i tag EXIF) devono essere DENTRO la libreria, non richieste all'utente
- Considera di restituire oggetti o dizionari con dati strutturati invece di tuple

## Come partecipare

1. Clona il repository
2. Crea un branch: `git switch -c sfida3-tuonome`
3. Modifica `challenge-3/python/image_processing.py`
4. Fai il commit: `git commit -m "Sfida 3: Redesign interface"`
5. Apri una Pull Request nel repository principale
6. Aspetta il feedback!

## Eseguire il programma

Il file `image_processing.py` non è eseguibile in quanto è una libreria. Puoi realizzare un programma di test per tuo conto, ma probabilmente il modo migliore di vedere come funziona (e **se** funziona) è leggendo ed eseguendo i test.

Ciò aumenta un poco la difficoltà di questa sfida, ma non ti preoccupare! Lavorare a questa sfida sarà un'esperienza nuova, che darà alla tua abilità di programmazione una marcia in più 🏎️

Detto questo...

## Testare la libreria

Per validare automaticamente il tuo lavoro, lancia i comandi:

```bash
pip install pytest  # se non lo hai già installato
python -m pytest challenge-3/python/image_processing_tests.py -v
```

I test verificano che il comportamento del codice sia rimasto uguale e copre in parte le nuove funzioni che saranno parte della nuova interfaccia. **Fai affidamento su di essi — ti faranno da guida per capire cosa funziona e cosa no!**

**Attenzione:** Non vale modificare il file con i test per far sì che passino! Tuttavia è OK aggiungere nuovi test per testare le nuove funzioni dell'interfaccia migliorata 👍

### Il problema del cambiare interfaccia

Se esegui i test, noterai che in parte sono scritti **per la VECCHIA interfaccia** e altri sono pensati per la nuova.

Forse stai pensando che una volta scritto il nuovo codice, puoi liberarti del vecchio e anche dei test corrispondenti...

Purtroppo non è così semplice: cambiare l'interfaccia quando è già in uso è costoso e doloroso. Chi usa la tua libreria vedrà il codice rompersi dopo un aggiornamento di versione. Per questo motivo:

1. **Pensa bene all'interfaccia PRIMA** di pubblicare la versione `1.0.0`
2. **Aggiungi nuove funzioni invece di modificare le esistenti**, fornendo così un nuovo percorso, più pulito, che possa sostituire il vecchio
3. Sfrutta i **deprecation warnings**: avvisa i tuoi utenti, con qualche versione di anticipo, che presto una certa interfaccia non sarà più valida e gli conviene migrare alla nuova.
