# Sfida 2: Single Responsibility Principle - Java Edition

Ti troverai di fronte a una funzione `processOrder()` che fa di tutto: valida, calcola, salva, notifica, logga. Il tuo compito è **spezzarla in funzioni più piccole**, ciascuna con una responsabilità ben definita.

### Il tuo compito:

1. Identificare le diverse responsabilità nella funzione `processOrder()`
2. Estrarre ogni responsabilità in una funzione separata e ben nominata
3. Semplificare l'annidamento e rendere il codice leggibile
> Attenzione!
>
> Assicurarti che il codice FACCIA ANCORA LA STESSA COSA (niente deve cambiare nella logica!) e che il nome delle funzioni estratte sia autoesplicativo

### Bonus (opzionale):

- Aggiungi una robusta gestione degli errori (se una fase fallisce, cosa succede?)
- Rendi le funzioni facilmente testabili
- Usa JavaDoc per documentare le funzioni

## Come partecipare

1. Clona il repository
2. Crea un branch: `git switch -c sfida2-tuonome`
3. Modifica `challenge-2/java/ProcessOrder.java`
4. Fai il commit: `git commit -m "Sfida 2: Extract responsibilities to functions"`
5. Apri una Pull Request nel repository principale
6. Aspetta il feedback!

## Eseguire il programma

Per prima cosa assicurati che [la JDK sia installata](https://adoptium.net/temurin/releases) sul PC locale.

Per eseguire il programma di prova, compila ed esegui:

```bash
javac -d out ./challenge-2/java/ProcessOrder.java
java -cp out ProcessOrder
```

## Test (Opzionale)

Se vuoi validare automaticamente il tuo lavoro, hai due alternative:
* se hi già un IDE, importa il progetto ed esegui i test contenuti nel file `challenge-2/java/ProcessOrderTest.java` (assicurati di aggiungere JUnit come libreria al progetto);
* se vuoi lanciare i test manualmente, segui le instruzioni che seguono.

Scarica il JAR del runner [junit-platform-console-standalone](https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.14.0/junit-platform-console-standalone-1.14.0.jar) (JUnit è uno strumento per eseguire test automatici in Java).

Supponiamo di averlo scaricato sotto `lib/junit-platform-console-standalone.jar`.

Compila il codice, poi compila ed esegui i test:

```bash
javac -cp lib/junit-platform-console-standalone.jar -d order_out ./challenge-2/java/ProcessOrder.java ./challenge-2/java/ProcessOrderTest.java
java -jar lib/junit-platform-console-standalone.jar execute --class-path order_out --scan-class-path
```

I test verificano che il comportamento del codice sia rimasto uguale, ma che tu abbia estratto tutte le responsabilità in funzioni separate. **Leggili pure — sono abbastanza semplici e ti possono aiutare a capire cosa cercare!**

**Attenzione:** Non vale modificare il file con i test per far sì che passino!
