# Sfida 2: Single Responsibility Principle - Python Edition

Ti troverai di fronte a una funzione `process_order()` che fa di tutto: valida, calcola, salva, notifica, logga. Il tuo compito è **spezzarla in funzioni più piccole**, ciascuna con una responsabilità ben definita.

### Il tuo compito:

1. Identificare le diverse responsabilità nella funzione `process_order()`
2. Estrarre ogni responsabilità in una funzione separata e ben nominata
3. Semplificare l'annidamento e rendere il codice leggibile
> Attenzione!
>
> Assicurarti che il codice FACCIA ANCORA LA STESSA COSA (niente deve cambiare nella logica!) e che il nome delle funzioni estratte sia autoesplicativo

### Bonus (opzionale):

- Aggiungi error handling robusto (se una fase fallisce, cosa succede?)
- Rendi le funzioni facilmente testabili
- Usa type hints per rendere il codice più chiaro

## Come partecipare

1. Clona il repository
2. Crea un branch: `git switch -c sfida2-tuonome`
3. Modifica `challenge-2/python/process_order.py`
4. Fai il commit: `git commit -m "Sfida 2: Extract responsibilities to functions"`
5. Apri una Pull Request nel repository principale
6. Aspetta il feedback!

## Eseguire il programma

Per prima cosa assicurati che [Python sia installato](https://wiki.python.org/moin/BeginnersGuide/Download) sul PC locale.

Per eseguire il programma di prova, lancia semplicemente:

```bash
python challenge-2/python/process_order.py
```

## Test (Opzionale)

Se vuoi validare automaticamente il tuo lavoro, lancia i comandi:

```bash
pip install pytest  # se non lo hai già installato
python -m pytest challenge-2/python/process_order_tests.py -v
```

I test verificano che il comportamento del codice sia rimasto uguale, ma che tu abbia estratto tutte le costanti. **Leggili pure — sono semplici e ti possono aiutare a capire cosa cercare!**

**Attenzione:** Non vale modificare il file con i test per far sì che passino!
