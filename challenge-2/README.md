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

```python
# ❌ SRP violato: la funzione fa troppo
def process_user(user_data, database):
    # Valida i dati
    if not user_data.get("email"):
        return {"error": "Email mancante"}
    
    # Calcola qualcosa
    user_data["score"] = len(user_data["name"]) * 2
    
    # Salva nel database
    database.save(user_data)
    
    # Invia email
    send_email(user_data["email"], "Benvenuto!")
    
    # Logga
    print(f"User {user_data['name']} processed")
    
    return user_data

# ✅ SRP rispettato: ogni funzione fa una cosa sola
def validate_user(user_data):
    if not user_data.get("email"):
        raise ValueError("Email mancante")
    return True

def calculate_user_score(user_data):
    return len(user_data["name"]) * 2

def save_user_to_database(user_data, database):
    database.save(user_data)

def notify_user(email):
    send_email(email, "Benvenuto!")

def log_user_processing(username):
    print(f"User {username} processed")

# Nel codice principale, le usi tutte insieme
def process_user(user_data, database):
    validate_user(user_data)
    user_data["score"] = calculate_user_score(user_data)
    save_user_to_database(user_data, database)
    notify_user(user_data["email"])
    log_user_processing(user_data["name"])
    return user_data
```

**Nota bene:** la versione "buona" sembra più lunga, ma è molto più flessibile. Se domani devi solo validare un utente, usi la funzione `validate_user`. Se devi solo calcolare lo score, usi `calculate_user_score`. Nella versione "cattiva" non puoi.

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