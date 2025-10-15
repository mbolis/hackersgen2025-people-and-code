# Sfida 1: Costanti e Nomi Significativi - Python Edition

È dato il file `shop_discount.py` — un piccolo sistema di sconti per uno shop online.

Il codice funziona, ma è pieno di **magic numbers** e **magic strings**. Variabili con nomi inutili (`p`, `d`, `c`). Valori sparsi ovunque.

## Il tuo compito:

1. Identificare tutti i valori "schiantati"
2. Convertirli in costanti ben nominate (in CONST_CASE... c'è un motivo se si chiama così, no?)
3. Usare queste costanti nel codice
> Attenzione!
>
> Assicurarti che il codice FACCIA ANCORA LA STESSA COSA (niente deve cambiare nella logica!) e che il nome di ogni costante deve essere autoesplicativo
4. (bonus) Assegnare nomi autoesplicativi a ogni variabile, evitando a ogni costo nomi inutilmente sintetici

**Suggerimento:** Cerca valori come `0.85`, `100`, `50`, stringhe tipo `"basic"`, `"premium"`, e nomi di variabili insensati come `p`, `d`, `c`.

## Come partecipare

1. Clona il repository
2. Crea un branch: `git switch -c sfida1-tuonome`
3. Modifica `challenge-1/python/shop_discount.py`
4. Fai il commit: `git commit -m "Sfida 1: Extract magic numbers to constants"`
5. Apri una Pull Request nel repository principale
6. Aspetta il feedback!

## Eseguire il programma

Per prima cosa assicurati che [Python sia installato](https://wiki.python.org/moin/BeginnersGuide/Download) sul PC locale.

Per eseguire il programma di prova, lancia semplicemente:

```bash
python challenge-1/python/shop_discount.py
```

## Test (Opzionale)

Se vuoi validare automaticamente il tuo lavoro, lancia i comandi:

```bash
pip install pytest  # se non lo hai già installato
python -m pytest challenge-1/python/shop_discount_tests.py -v
```

I test verificano che il comportamento del codice sia rimasto uguale, ma che tu abbia estratto tutte le costanti. **Leggili pure — sono semplici e ti possono aiutare a capire cosa cercare!**

**Attenzione:** Non vale modificare il file con i test per far sì che passino!
