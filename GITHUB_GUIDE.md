# 📚 Guida GitHub per Principianti

Mai usato GitHub prima? Nessun problema! Questa guida ti accompagnerà passo passo.

## 🤔 Cos'è GitHub?

GitHub è una piattaforma per:
- 💾 **Salvare codice** (con tutta la cronologia delle modifiche)
- 🤝 **Collaborare** con altri sviluppatori
- 📝 **Documentare** progetti
- 🐛 **Tracciare** bug e richieste di funzionalità

È come Google Drive, ma per il codice, con superpoteri! 🦸

---

## 🛠️ Setup Iniziale

### 1. Crea un Account GitHub

1. Vai su [github.com](https://github.com)
2. Clicca "Sign up"
3. Segui le istruzioni
4. Verifica la tua email

### 2. Installa Git

**Linux**:
```bash
sudo apt install git     # Ubuntu/Debian
sudo dnf install git     # Fedora
sudo pacman -S git       # Arch Linux/Manjaro
sudo zypper install git  # OpenSUSE
```

**macOS**:
- Apri il Terminale
- Digita: `git --version`
- Se non installato, ti chiederà di installarlo

**Windows**:
- Scarica da [git-scm.com](https://git-scm.com/download/win)
- Installa con le opzioni predefinite

### 3. Configura Git

Apri il terminale/prompt dei comandi e digita:

```bash
git config --global user.name "Il Tuo Nome"
git config --global user.email "tua.email@example.com"
```

⚠️ Usa la stessa email del tuo account GitHub!

---

## 🚀 Come Partecipare alle Sfide

### Passo 1: Fork del Repository

Il "fork" crea una tua copia personale del progetto.

1. Vai sulla pagina del repository
2. Clicca il pulsante **"Fork"** in alto a destra
3. Aspetta qualche secondo
4. Ora hai la tua copia! 🎉

### Passo 2: Clona il Tuo Fork

Ora scarica la tua copia sul tuo computer.

1. Sul **tuo** fork, clicca il pulsante verde **"Code"**
2. Copia l'URL (dovrebbe contenere il TUO username)
3. Apri il terminale/prompt
4. Vai nella cartella dove vuoi il progetto:
   ```bash
   cd Documenti  # o dove preferisci
   ```
5. Clona:
   ```bash
   git clone https://github.com/TUO-USERNAME/hackersgen2025-people-and-code.git
   ```
6. Entra nella cartella:
   ```bash
   cd hackersgen2025-people-and-code
   ```

### Passo 3: Crea un Branch

Un "branch" è come una timeline alternativa dove puoi lavorare senza toccare l'originale.

```bash
git switch -c challenge-1-tier-1-tuonome
```

Sostituisci con il tuo nome e la sfida che vuoi fare!

💡 **Naming convention**: `challenge-X-tier-Y-tuonome`
- Esempio: `challenge-1-tier-1-nickcarter`

### Passo 4: Lavora sulla Sfida

1. Apri la cartella della sfida (es. `challenge-01-refactoring/`)
2. Scegli il linguaggio (es. `python/`)
3. Leggi il README per le istruzioni
4. Scrivi la tua soluzione!
5. Testa che funzioni

### Passo 5: Salva le Tue Modifiche (Commit)

Quando hai finito (o fatto progressi significativi):

```bash
# Aggiungi i file modificati
git add .

# Crea un commit con un messaggio descrittivo
git commit -m "Completato Challenge 1 Tier 1 - refactoring con costanti e funzioni separate"
```

💡 **Tip sui messaggi di commit**:
- ✅ "Completato refactoring con costanti e nomi chiari"
- ✅ "Aggiunta gestione errori con try-except"
- ❌ "fix"
- ❌ "update"

### Passo 6: Carica su GitHub (Push)

```bash
git push origin challenge-1-tier-1-tuonome
```

(usa il nome del tuo branch!)

### Passo 7: Apri una Pull Request (PR)

1. Vai sul **tuo** fork su GitHub
2. Dovresti vedere un banner giallo che dice "Compare & pull request" - clicca!
3. Compila il template:
   - Quale sfida hai fatto?
   - Cosa hai imparato?
   - Eventuali domande
4. Clicca **"Create pull request"**
5. 🎉 Fatto! Ora aspetta la review!

---

## 🆘 Chiedere Aiuto

### Tramite Issue

1. Vai sul repository originale (non il tuo fork!)
2. Clicca su **"Issues"**
3. Clicca **"New issue"**
4. Scegli **"Richiesta di Aiuto"**
5. Compila il template
6. Clicca **"Submit new issue"**

Riceverai una notifica via email quando ti rispondo!

### Tramite PR Comments

Se hai già aperto una PR e vuoi chiedere qualcosa di specifico:

1. Vai sulla tua PR
2. Scrivi un commento nella sezione "Conversation"
3. Menziona @mbolis per assicurarti che riceva una notifica
4. Esempio: "@mbolis non sono sicuro se ho usato bene le costanti qui"

---

## 📖 Glossario

**Repository (Repo)**: Un progetto con tutto il suo codice e cronologia

**Fork**: Una tua copia personale di un repository

**Clone**: Scaricare un repository sul tuo computer

**Branch**: Una "linea temporale" separata dove puoi lavorare

**Commit**: Salvare un insieme di modifiche con un messaggio

**Push**: Caricare i tuoi commit su GitHub

**Pull Request (PR)**: Chiedere di includere le tue modifiche nel progetto originale

**Merge**: Unire le modifiche di un branch in un altro

**Issue**: Una discussione, domanda, o segnalazione di bug

---

## 🎯 Comandi Git Più Usati

```bash
# Vedere lo stato delle tue modifiche
git status

# Vedere la cronologia dei commit
git log

# Vedere su quale branch sei
git branch

# Cambiare branch
git switch nome-branch

# Creare un nuovo branch
git switch -c nuovo-branch

# Aggiungere file allo staging
git add nome-file.py
git add .  # aggiunge tutto

# Fare un commit
git commit -m "Messaggio descrittivo"

# Caricare su GitHub
git push origin nome-branch

# Scaricare le ultime modifiche
git pull

# Vedere le differenze non ancora committate
git diff
```

---

## 💡 Tips & Tricks

### Commit Piccoli e Frequenti

Meglio tanti piccoli commit che uno gigante:
- ✅ "Aggiunte costanti per orientamento EXIF"
- ✅ "Estratta funzione get_rotation_angle"
- ✅ "Aggiunta gestione errori"

### Testa Prima di Committare

Assicurati che il codice funzioni prima di fare commit!

### Usa Git Come Salvataggio

Fai commit anche mentre lavori, non solo alla fine. È come premere Ctrl+S ma meglio!

### Non Aver Paura di Sperimentare

Con Git puoi sempre tornare indietro. È impossibile rompere definitivamente qualcosa!

### Leggi i Messaggi di Errore

Git è verboso. Se qualcosa non va, leggi l'errore - spesso ti dice esattamente cosa fare.

---

## 🆘 Problemi Comuni

### "Permission denied" quando faccio push

Probabilmente stai cercando di pushare sul repository originale invece che sul tuo fork.

Verifica con:
```bash
git remote -v
```

Dovresti vedere il TUO username nell'URL. Se vedi quello originale:
```bash
git remote set-url origin https://github.com/TUO-USERNAME/hackersgen2025-people-and-code.git
```

### "Nothing to commit"

Non hai modificato nessun file, o non li hai aggiunti con `git add`.

### "Merge conflict"

Raramente succederà in queste sfide. Se succede, chiedi aiuto aprendo un'issue!

### Ho fatto un commit sbagliato

Se non hai ancora fatto push:
```bash
git reset --soft HEAD~1  # Annulla ultimo commit, mantiene modifiche
```

Se hai già fatto push, meglio fare un nuovo commit che corregge!

---

## 📚 Risorse per Imparare di Più

- [GitHub Skills](https://skills.github.com/) - Tutorial interattivi
- [Git Handbook](https://guides.github.com/introduction/git-handbook/) - Guida ufficiale
- [Visualizing Git](https://git-school.github.io/visualizing-git/) - Vedi cosa fanno i comandi
- [Oh Shit, Git!](https://ohshitgit.com/) - Come risolvere errori comuni
- [Pro Git Book](https://git-scm.com/book/it/v2) - Libro completo (anche in italiano!)

---

## 🎉 Sei Pronto!

Ora hai tutto quello che ti serve per partecipare. Non preoccuparti se all'inizio sembra complicato - **tutti** hanno iniziato da zero!

Ricorda:
- 💪 Fai errori, è normale
- 🤔 Chiedi aiuto quando ti blocchi
- 🌟 Impara facendo
- 🚀 Divertiti!

**Buon coding!** 🎊

---

*Hai ancora domande? [Apri un'issue](../../issues/new/choose) e chiedi!*
