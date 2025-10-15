# Sfida 2: Single Responsibility Principle - Go Edition

Ti troverai di fronte a una funzione `ProcessOrder()` che fa di tutto: valida, calcola, salva, notifica, logga. Il tuo compito è **spezzarla in funzioni più piccole**, ciascuna con una responsabilità ben definita.

### Il tuo compito:

1. Identificare le diverse responsabilità nella funzione `ProcessOrder()`
2. Estrarre ogni responsabilità in una funzione separata e ben nominata
3. Semplificare l'annidamento e rendere il codice leggibile
> Attenzione!
>
> Assicurarti che il codice FACCIA ANCORA LA STESSA COSA (niente deve cambiare nella logica!) e che il nome delle funzioni estratte sia autoesplicativo

### Bonus (opzionale):

- Aggiungi una robusta gestione degli errori (se una fase fallisce, cosa succede?)
- Rendi le funzioni facilmente testabili
- Usa commenti documentativi per rendere il codice più chiaro

## Come partecipare

1. Clona il repository
2. Crea un branch: `git switch -c sfida2-tuonome`
3. Modifica `challenge-2/go/process_order.go`
4. Fai il commit: `git commit -m "Sfida 2: Extract responsibilities to functions"`
5. Apri una Pull Request nel repository principale
6. Aspetta il feedback!

## Eseguire il programma

Per prima cosa assicurati che [Go sia installato](https://go.dev/doc/install) sul PC locale.

Per eseguire il programma di prova, lancia semplicemente:

```bash
go run ./challenge-2/go
```

## Test (Opzionale)

Se vuoi validare automaticamente il tuo lavoro, lancia il comando:

```bash
go test ./challenge-2/go -v
```

I test verificano che il comportamento del codice sia rimasto uguale, ma che tu abbia estratto tutte le responsabilità in funzioni separate. **Leggili pure — sono abbastanza semplici e ti possono aiutare a capire cosa cercare!**

**Attenzione:** Non vale modificare il file con i test per far sì che passino!
