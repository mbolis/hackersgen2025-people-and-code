# Sfida 1: Clear & Constant - Go Edition

È dato il file `shop_discount.go` — un piccolo sistema di sconti per uno shop online.

Il codice funziona, ma è pieno di **magic numbers** e **magic strings**. Variabili con nomi inutili (`p`, `d`, `c`). Valori sparsi ovunque.

**Il tuo compito:**

1. Identificare tutti i valori "schiantati"
2. Convertirli in costanti ben nominate (in PascalCase, questa è la convenzione per i nomi di costante in Go)
3. Usare queste costanti nel codice
> Attenzione!
>
> Assicurarti che il codice FACCIA ANCORA LA STESSA COSA (niente deve cambiare nella logica!) e che il nome di ogni costante deve essere autoesplicativo
4. (bonus) Assegnare nomi autoesplicativi a ogni variabile, evitando a ogni costo nomi inutilmente sintetici

**Suggerimento:** Cerca valori come `0.85`, `100`, `50`, stringhe tipo `"basic"`, `"premium"`, e nomi di variabili insensati come `p`, `d`, `c`.

## Come partecipare

1. Clona il repository
2. Crea un branch: `git switch -c sfida1-tuonome`
3. Modifica `challenge-1/to/shop_discount.go`
4. Fai il commit: `git commit -m "Sfida 1: Extract magic numbers to constants"`
5. Apri una Pull Request nel repository principale
6. Aspetta il feedback!

## Eseguire il programma

Per prima cosa assicurati che [Go sia installato](https://go.dev/doc/install) sul PC locale.

Per eseguire il programma di prova, lancia semplicemente:

```bash
go run ./challenge-1/go
```

## Test (Opzionale)

Se vuoi validare automaticamente il tuo lavoro, lancia il comand9:

```bash
go test ./challenge-1/go -v
```

I test verificano che il comportamento del codice sia rimasto uguale, ma che tu abbia estratto tutte le costanti. **Leggili pure — sono abbastanza semplici e ti possono aiutare a capire cosa cercare!**
