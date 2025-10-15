# Sfida 1: Costanti e Nomi Significativi - Java Edition

È dato il file `ShopDiscount.java` — un piccolo sistema di sconti per uno shop online.

Il codice funziona, ma è pieno di **magic numbers** e **magic strings**. Variabili con nomi inutili (`p`, `d`, `c`). Valori sparsi ovunque.

**Il tuo compito:**

1. Identificare tutti i valori "schiantati"
2. Convertirli in costanti (`static` e `final`!) ben nominate (in CONST_CASE... c'è un motivo se si chiama così, no?)
3. Usare queste costanti nel codice
> Attenzione!
>
> Assicurarti che il codice FACCIA ANCORA LA STESSA COSA (niente deve cambiare nella logica!) e che il nome di ogni costante deve essere autoesplicativo
4. (bonus) Assegnare nomi autoesplicativi a ogni variabile, evitando a ogni costo nomi inutilmente sintetici

**Suggerimento:** Cerca valori come `0.85`, `100`, `50`, stringhe tipo `"basic"`, `"premium"`, e nomi di variabili insensati come `p`, `d`, `c`.

## Come partecipare

1. Clona il repository
2. Crea un branch: `git switch -c sfida1-tuonome`
3. Modifica `challenge-1/java/ShopDiscount.java`
4. Fai il commit: `git commit -m "Sfida 1: Extract magic numbers to constants"`
5. Apri una Pull Request nel repository principale
6. Aspetta il feedback!

## Eseguire il programma

Per prima cosa assicurati che [la JDK sia installata](https://adoptium.net/temurin/releases) sul PC locale.

Per eseguire il programma di prova, compila ed esegui:

```bash
javac -d out ./challenge-1/java/ShopDiscount.java
java -cp out ShopDiscount
```

## Test (Opzionale)

Se vuoi validare automaticamente il tuo lavoro, hai due alternative:
* se hi già un IDE, importa il progetto ed esegui i test contenuti nel file `challenge-1/java/ShopDiscountTest.java` (assicurati di aggiungere JUnit come libreria al progetto);
* se vuoi lanciare i test manualmente, segui le instruzioni che seguono.

Scarica il JAR del runner [junit-platform-console-standalone](https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/1.14.0/junit-platform-console-standalone-1.14.0.jar) (JUnit è uno strumento per eseguire test automatici in Java).

Supponiamo di averlo scaricato sotto `lib/junit-platform-console-standalone.jar`.

Compila il codice, poi compila ed esegui i test:
* sotto Linux e MacOS:

```bash
javac -cp 'out:lib/junit-platform-console-standalone.jar' -d test_out ./challenge-1/java/ShopDiscountTest.java
java -jar lib/junit-platform-console-standalone.jar execute --class-path 'out:test_out' --scan-class-path
```

* sotto Windows:

```bash
javac -cp "out;lib/junit-platform-console-standalone.jar" -d test_out ./challenge-1/java/ShopDiscountTest.java
java -jar lib/junit-platform-console-standalone.jar execute --class-path "out;test_out" --scan-class-path
```

I test verificano che il comportamento del codice sia rimasto uguale, ma che tu abbia estratto tutte le costanti. **Leggili pure — sono abbastanza semplici e ti possono aiutare a capire cosa cercare!**

**Attenzione:** Non vale modificare il file con i test per far sì che passino!
