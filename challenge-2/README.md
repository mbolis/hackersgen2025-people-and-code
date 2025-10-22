# Sfida 2: Single Responsibility Principle (SRP)

## La teoria: Single Responsibility Principle

Il **Single Responsibility Principle** (SRP) è uno dei cinque principi SOLID della programmazione ad oggetti. Recita così:

> Una classe (o funzione) dovrebbe avere una sola responsabilità, e dovrebbe cambiarsi per una sola ragione.

In altre parole: **ogni funzione dovrebbe fare UNA cosa sola, e farla bene.**

Applicare bene questo principio significa aver compreso intimamente i concetti di *disaccoppiamento* e *coesione*, anche senza averli mai studiati!

**Nota importante:** Anche se SOLID è stato ideato con un'attenzione particolare alla programmazione a oggetti, **tutti gli stili di programmazione possono beneficiarne!**

### Perché è importante?

Una funzione che fa troppe cose è difficile da:
- **Capire**: leggere una funzione con molte responsabilità è stancante e genera confusione e facilmente errori.
- **Modificare**: se cambi un comportamento, rischi di rompere tutto il resto!
- **Riutilizzare**: se quella funzione fa 5 cose, ma altrove te ne serve solo una, non puoi riusarla. Ti tocca ricopiare solo quella funzionalità.
- **Testare**: quanti test ti tocca scrivere per una funzione che fa 5 cose diverse?

Una funzione focalizzata è invece:
- **Facile da capire**: il nome dice esattamente cosa fa, né più né meno.
- **Facile da modificare**: cambi il minimo indispensabile e non impatti nient'altro.
- **Facile da riutilizzare**: se fai una cosa bene, la usi ovunque!
- **Facile da testare**: testi una cosa alla volta, con minor numero di combinazioni.

### Un semplice esempio

```go
// ❌ SRP violato: la funzione fa troppo
func ProcessUser(userData UserData, database DB) (UserData, error) {
  // Valida i dati
  if userData.Email == "" {
    return UserData{}, errors.New("Email mancante")
  }
  // Calcola qualcosa
  userData.Score = len(userData.Name) * 2
  // Salva nel database
  err := database.save(userData)
  if err != nil {
    return UserData{}, err
  }
  // Invia email
  err = SendEmail(userData.Email, "Benvenuto!")
  if err != nil {
    return UserData{}, err
  }
  // Logga
  log.Printf("User %s processed", userData.Name)

  return userData, nil
}

// ✅ SRP rispettato: ogni funzione fa una cosa sola
func validateUser(userData UserData) error {
  if userData.Email == "" {
    return errors.New("validation: Email mancante")
  }
  return nil
}
func calculateUserScore(userData UserData) int {
  return len(userData.Name) * 2
}
func saveUserToDatabase(userData UserData, database DB) error {
  err := database.save(userData)
  if err != nil {
    return fmt.Errorf("save_user: %w", err)
  }
  return nil
}
func notifyUser(email string) error {
  err := SendEmail(email, "Benvenuto!")
  if err != nil {
    return fmt.Errorf("notifiy_user: %w", err)
  }
  return nil
}
func logUserProcessing(username string) {
  log.Printf("User %s processed", username)
}

// Il punto di accesso principale è responsabile di orchestrarle
func ProcessUser(userData UserData, database DB) (UserData, error) {
  err := validateUser(userData)
  if err != nil {
    return UserData{}, err
  }
  userData.Score = calculateUserScore(userData)
  err = saveUserToDatabase(userData, database)
  if err != nil {
    return UserData{}, err
  }
  err = notifyUser(userData.Email)
  if err != nil {
    return UserData{}, err
  }
  logUserProcessing(userData.Name)
  return userData, nil
}
```

**Nota bene:** la versione "buona" sembra più lunga, ma è molto più flessibile. Se domani devi solo validare un utente, usi la funzione `validateUser`. Se devi solo calcolare lo score, usi `calculateUserScore`. Nella versione "cattiva" non puoi.

## Approfondimenti

- [SOLID Principles Explained](https://www.baeldung.com/solid-principles)
- [Why Single Responsibility Matters](https://dev.to/mstuttgart/understanding-solid-principles-a-beginners-guide-4fl6)

## La sfida

Scegli un linguaggio. La sfida è la stessa indipendentemente dal liguaggio scelto, cambia solo la sintassi!

* [Java](./java)
* [JavaScript](./javascript)
* [Python](./python)
* [Go](./go)

---

**Ricorda:** Non è una competizione di velocità. Prenditi il tempo di capire PERCHÉ ogni magic number deve diventare una costante. Quello che impari oggi lo porterai nella tua programmazione per sempre. 🚀