# Hackersgen Event 2025 - People and Code

> *"Qualunque pirla può scrivere codice comprensibile da un computer. Un bravo programmatore sa scrivere codice comprensibile agli esseri umani."* - Martin Fowler

Benvenuti alle nostre sfide di leggibilità del codice! Questo repository contiene 3 sfide pratiche per migliorare le vostre capacità di scrivere codice pulito, manutenibile e comprensibile.

Se siete interessati alle slide e agli esempi della presentazione del 16 ottobre 2025, li trovate qui: [Presentazione](./demo)

## 📋 Le Sfide

Ora mettetevi alla prova con tre sfide progressivamente più complesse. Ogni sfida insegna un principio fondamentale del *clean coding*, accompagnato da una breve introduzione teorica e link per approfondire.

Partecipare è semplice: fai un fork, modifica il codice, crea una PR. ([vedi sotto per maggiori dettagli](#-come-partecipare)) **I primi a completare ogni sfida riceveranno un gadget Hackersgen!** ⭐

### 🟢 Sfida 1: Costanti e Nomi Significativi
**Argomenti:** DRY (Don't Repeat Yourself), Self-documenting code, Magic numbers e magic strings

Inizia da qui se è la tua prima sfida. Ti troverai di fronte a un codice pieno di variabili dal nome inutile (`p`, `x`, `tmp`) e valori magici sparsi dappertutto (`0.85`, `100`, `50`). Il tuo compito? Dare nomi significativi alle cose e mettere ordine.

**Tempo stimato:** 30-60 minuti  
**Difficoltà:** ⭐ Facile

👉 [Vai alla Sfida 1](./challenge-1)

---

### 🟡 Sfida 2: Single Responsibility Principle
**Argomenti:** SRP, Funzioni focalizzate, Coesione, Separazione delle responsabilità

Una funzione che fa di tutto: valida, calcola, salva, notifica, logga. È il peggiore incubo di chi deve mantenerlo. Il tuo compito? Spezzarla in funzioni piccole, ognuna con una responsabilità ben definita. Imparerai perché le funzioni focalizzate sono più facili da testare, riutilizzare e modificare.

**Tempo stimato:** 1-2 ore  
**Difficoltà:** ⭐⭐ Intermedia

👉 [Vai alla Sfida 2](./challenge-2)

---

### 🔴 Sfida 3: Incapsulamento e Astrazione
**Argomenti:** Incapsulamento, Astrazione, Information hiding, Interfacce semplici

La sfida finale: un sistema di processamento immagini con logica complessa, nesting profondo e gestione errori sparsa. Devi crearne un'astrazione elegante, nascondendo i dettagli dietro interfacce semplici e intuitive. Questa è la sfida dove metti insieme tutto quello che hai imparato.

**Tempo stimato:** 2-4 ore  
**Difficoltà:** ⭐⭐⭐ Avanzata

👉 [Vai alla Sfida 3](./challenge-3)

---

## 🏆 Hall of Fame

Il primo di voi a completare ogni sfida vince un premio speciale!

### Sfida 1: Costanti e Nomi Significativi
🥇 🥈 🥉 *In attesa delle prime submission!*

### Sfida 2: Single Responsibility Principle
🥇 🥈 🥉 *In attesa delle prime submission!*

### Sfida 3: Incapsulamento e Astrazione
🥇 🥈 🥉 *In attesa delle prime submission!*

---

## 🚀 Come Partecipare

### 1️⃣ Fai il Fork del Repository
Clicca sul pulsante "Fork" in alto a destra per creare la tua copia del repository.

### 2️⃣ Clona il Tuo Fork
```bash
git clone https://github.com/TUO-USERNAME/hackersgen2025-people-and-code.git
cd hackersgen2025-people-and-code
```

### 3️⃣ Crea un Branch per la Tua Soluzione
```bash
git switch -c challenge-1-tier-1-tuo-nome
```
Usa un nome descrittivo: `challenge-X-tier-Y-tuonome`

### 4️⃣ Lavora sulla Sfida
- Vai nella cartella della sfida (es. `challenge-01-refactoring/`)
- Scegli il linguaggio che preferisci (python, javascript, java, ecc.)
- Leggi il README della sfida per istruzioni dettagliate
- Scrivi la tua soluzione!

### 5️⃣ Commit e Push
```bash
git add .
git commit -m "Challenge 1 Tier 1 - Refactoring completato"
git push origin challenge-1-tier-1-tuo-nome
```

### 6️⃣ Apri una Pull Request
- Vai sul tuo fork su GitHub
- Clicca "Compare & pull request"
- Compila il template della PR
- Aspetta la review! 👀

**Non hai mai usato GitHub?** Nessun problema! Leggi la [Guida per Principianti](./GITHUB_GUIDE.md)

---

## 🆘 Hai Bisogno di Aiuto?

**Sono qui per aiutarti!** La programmazione si impara facendo domande. Non esistono domande stupide.

### Come chiedere aiuto:

1. **Per domande generali o concettuali**: [Apri una Issue](../../issues/new/choose) usando il template "Richiesta di Aiuto"
2. **Per feedback sul tuo codice**: Apri una Draft PR e menziona @mbolis nei commenti
3. **Prima di chiedere**: Dai un'occhiata alle [Issues già risolte](../../issues?q=is%3Aissue+is%3Aclosed) - magari qualcuno ha già fatto la tua stessa domanda!

### ⏰ Tempi di risposta

Lavoro a tempo pieno, ma controllo GitHub regolarmente. Di solito rispondo entro:
- **24-48 ore** nei giorni lavorativi
- **Nel fine settimana** potrei non rispondere

**Non c'è fretta!** Prenditi il tempo che serve. Queste sfide non hanno scadenza.

💡 **Pro tip**: Più dettagli dai nella tua domanda (cosa hai provato, cosa ti aspettavi, cosa è successo invece), più facile sarà per me aiutarti!

---

## 📚 Risorse Utili

- [GitHub Skills](https://skills.github.com/) - Tutorial interattivi su Git e GitHub

- [Astrazione e Incapsulamento](https://www.baeldung.com/cs/abstraction-vs-encapsulation)
- [Disaccoppiamento e Coesione](https://stackoverflow.com/questions/14000762/what-does-low-in-coupling-and-high-in-cohesion-mean)
- [KISS, DRY, YAGNI](https://medium.com/@curiousraj/the-principles-of-clean-code-dry-kiss-and-yagni-f973aa95fc4d)
- [SOLID](https://www.baeldung.com/solid-principles) e... [SOLID spiegato ai principianti](https://dev.to/mstuttgart/understanding-solid-principles-a-beginners-guide-4fl6)
- [Refactoring: Improving the Design of Existing Code](https://refactoring.com/) - Martin Fowler
- [Clean Code: A Handbook of Agile Software Craftsmanship](https://www.amazon.it/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882) - Robert C. Martin
- [The Pragmatic Programmer](https://pragprog.com/titles/tpp20/the-pragmatic-programmer-20th-anniversary-edition/) - Andy Hunt & Dave Thomas

- [Google Style Guides](https://google.github.io/styleguide/) - Per vari linguaggi
- [Python PEP 8 Style Guide](https://pep8.org/)

---

## 🎯 Regole e Linee Guida

### Cosa Cerco in una Submission

✅ **Codice comprensibile**: Nomi chiari, struttura logica
✅ **Buone pratiche**: DRY, KISS, YAGNI
✅ **Funziona**: Il codice deve essere eseguibile e fare quello che deve fare
✅ **Pensiero critico**: Spiega le tue scelte nella PR

❌ **Non serve perfezione**: L'importante è mostrare che hai capito i concetti!

### Code of Conduct

- 🤝 Rispetta gli altri partecipanti
- 💬 Sii costruttivo nei commenti
- 🎓 Impara dagli altri e condividi le tue conoscenze
- 🚫 Non copiare soluzioni di altri (ma puoi ispirarti dopo aver provato tu!)
- ⚖️ Non fare spoiler delle soluzioni nelle Issues pubbliche

---

## 📝 Linguaggi Supportati

Ogni sfida è disponibile in:
- ☕ Java
- 🐍 Python
- 🟨 JavaScript
- 🐹 Go
- (Altri linguaggi possono essere aggiunti su richiesta!)

---

## 🤔 FAQ

**Q: Posso tentare più sfide?**  
A: Assolutamente sì! Prova tutte quelle che vuoi.

**Q: Posso fare la stessa sfida in più linguaggi?**  
A: Certamente! È un ottimo modo per confrontarli tra loro.

**Q: Cosa succede se non arrivo nei primi 3?**  
A: Ricevi comunque feedback sulla tua soluzione e impari a fare ordine nel tuo codice! Il vero premio è il codice pulito che scriviamo lungo la strada. 😄

**Q: Posso collaborare con altri?**  
A: Sì, ma ognuno deve fare la propria submission. Potete discutere approcci, ma il codice deve essere vostro.

**Q: Quanto tempo ho?**  
A: Tutto il tempo che vuoi! Non c'è scadenza.

**Q: Posso rivedere la mia submission dopo averla inviata?**  
A: Sì, puoi fare push di nuovi commit sulla stessa PR. Anzi, è incoraggiato dopo il feedback!

---

## 📧 Contatti

**Marco Bolis**  
Senior Full-stack Developer @ Sorint.lab  
[@mbolis](https://github.com/mbolis)

---

## 📄 Licenza

Questo progetto è rilasciato sotto [licenza MIT](./LICENSE). Sentiti libero di usarlo, modificarlo e condividerlo.

---

**Buon divertimento e buon coding! 🎉**

*"Always code as if the guy who ends up maintaining your code will be a violent psychopath who knows where you live." - John F. Woods*
