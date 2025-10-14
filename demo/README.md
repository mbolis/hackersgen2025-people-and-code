# Presentazione ed esempi
Qui trovate il [PDF con le slide](./Hackersgen_2025_MBOLIS.pdf) che ho usato durante la presentazione, oltre al codice da me usato come esempio durante lo speech.

## Sperimentare con il codice in locale
Per eseguire il codice in locale, occorre aver installato `uv` (trovate le istruzioni nella [documentazione di `uv` by Astral](https://docs.astral.sh/uv/getting-started/installation/)).

Se non avete ancora clonato questo repository, aprite una shell e lanciate:
```sh
git clone https://github.com/mbolis/hackersgen2025-people-and-code.git
```
Eseguite i seguenti comandi per avviare il programma di esempio:
```sh
cd hackersgen2025-people-and-code/demo
uv run app.py
```
Poi aprite il vostro browser all'indirizzo `http://localhost:5000/`.

Trovate due immagini di esempio da caricare nell'applicazione nella cartella `images`:
* `squirrel.jpg` è ruotata, ma senza i tag EXIF che specificano la rotazione
* `squirrel_rotated.jpg` ha i corretti tag EXIF

### Modificare il codice
Per effettuare modifiche al codice, vi suggerisco di usare [VS Code](https://code.visualstudio.com/download):
```sh
cd hackersgen2025-people-and-code/demo
code .
```

Il codice eseguibile si trova nel file `app.py`. Il servizio dovrebbe riavviarsi automagicamente ogni volta che salvi il tuo lavoro e la pagina nel browser dovrebbe ricaricarsi... In caso non funzionasse, riavvia il servizio da riga di comando e ricarica il browser!

La form di caricamento è nel file `static/index.html`.

Ho incluso alcune estensioni ([Python](https://marketplace.visualstudio.com/items?itemName=ms-python.python) e [Ruff](https://marketplace.visualstudio.com/items?itemName=charliermarsh.ruff)) e impostazioni suggerite per lavorare meglio con Python. Se volete dare un'occhiata, aprite la cartella `.vscode`.

Tutto il resto non sono altro che file di configurazione del progetto che specificano quali librerie (e quali versioni) scaricare per farlo partire.
