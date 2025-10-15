/*
Sfida 2: Single Responsibility Principle

Questo file contiene una funzione che viola il SRP.
Il tuo compito è spezzarla in funzioni più piccole e focalizzate.

Usa questo file come punto di partenza. Puoi modificarlo come preferisci,
aggiungere funzioni, cambiare la struttura - l'importante è che alla fine
il codice sia più leggibile e manutenibile.
*/
package main

import (
	"fmt"
	"os"
)

// Item rappresenta un articolo nell'ordine
type Item struct {
	Name     string
	Price    float64
	Quantity int
}

// OrderData contiene i dati di un ordine
type OrderData struct {
	CustomerName  string
	CustomerEmail string
	IsVipCustomer bool
	Items         []Item
}

// OrderRecord rappresenta un ordine salvato nel database
type OrderRecord struct {
	CustomerName  string
	CustomerEmail string
	Items         []Item
	Subtotal      float64
	Tax           float64
	Total         float64
	Vip           bool
	Status        string
}

// Database interface per il salvataggio degli ordini
type Database interface {
	SaveOrder(order OrderRecord) error
}

// EmailService interface per l'invio delle email
type EmailService interface {
	Send(to, subject, body string) error
}

/*
ProcessOrder processa un ordine dal caricamento alla notifica.
⚠️ ATTENZIONE: questa funzione fa TROPPO!
*/
func ProcessOrder(orderData OrderData, database Database, emailService EmailService) *OrderRecord {
	// Validazione
	if len(orderData.Items) == 0 {
		fmt.Println("❌ ERRORE: Ordine vuoto")
		return nil
	}

	if orderData.CustomerEmail == "" {
		fmt.Println("❌ ERRORE: Email cliente mancante")
		return nil
	}

	if orderData.CustomerName == "" {
		fmt.Println("❌ ERRORE: Nome cliente mancante")
		return nil
	}

	for _, item := range orderData.Items {
		if item.Price <= 0 {
			fmt.Printf("❌ ERRORE: Prezzo non valido per %s\n", item.Name)
			return nil
		}
		if item.Quantity <= 0 {
			fmt.Printf("❌ ERRORE: Quantità non valida per %s\n", item.Name)
			return nil
		}
	}

	// Calcolo del totale e applicazione di sconti
	total := 0.0
	for _, item := range orderData.Items {
		total += item.Price * float64(item.Quantity)
	}

	// Sconto fedeltà cliente
	if orderData.IsVipCustomer {
		total = total * 0.85
		fmt.Println("✓ Sconto VIP applicato (15%)")
	}

	// Sconto ordini grandi
	if total > 500 {
		total = total * 0.90
		fmt.Println("✓ Sconto ordine grande applicato (10%)")
	}

	// Calcolo tasse
	tax := total * 0.22
	finalTotal := total + tax

	fmt.Printf("Totale: €%.2f\n", total)
	fmt.Printf("Tasse (22%%): €%.2f\n", tax)
	fmt.Printf("Totale finale: €%.2f\n", finalTotal)

	// Salvataggio nel database
	orderRecord := OrderRecord{
		CustomerName:  orderData.CustomerName,
		CustomerEmail: orderData.CustomerEmail,
		Items:         orderData.Items,
		Subtotal:      total,
		Tax:           tax,
		Total:         finalTotal,
		Vip:           orderData.IsVipCustomer,
		Status:        "pending",
	}

	err := database.SaveOrder(orderRecord)
	if err != nil {
		fmt.Printf("❌ ERRORE nel salvataggio: %v\n", err)
		return nil
	}
	fmt.Println("✓ Ordine salvato nel database")

	// Invio email di conferma
	emailSubject := fmt.Sprintf("Ordine confermato - €%.2f", finalTotal)
	emailBody := fmt.Sprintf(`
Grazie %s!

Il tuo ordine è stato confermato.
Totale: €%.2f

Dettagli:
`, orderData.CustomerName, finalTotal)

	for _, item := range orderData.Items {
		emailBody += fmt.Sprintf("- %s x%d: €%.2f\n", item.Name, item.Quantity, item.Price*float64(item.Quantity))
	}

	err = emailService.Send(orderData.CustomerEmail, emailSubject, emailBody)
	if err != nil {
		fmt.Printf("❌ ERRORE nell'invio email: %v\n", err)
		// Nota: in questo caso continuiamo comunque (l'ordine è già salvato)
	} else {
		fmt.Printf("✓ Email di conferma inviata a %s\n", orderData.CustomerEmail)
	}

	// Logging
	logMessage := fmt.Sprintf("[ORDINE] %s - €%.2f - VIP: %t", orderData.CustomerName, finalTotal, orderData.IsVipCustomer)
	file, err := os.OpenFile("orders.log", os.O_APPEND|os.O_CREATE|os.O_WRONLY, 0644)
	if err != nil {
		fmt.Printf("⚠️ AVVISO: Non è stato possibile loggare (%v)\n", err)
	} else {
		defer file.Close()
		_, err = file.WriteString(logMessage + "\n")
		if err != nil {
			fmt.Printf("⚠️ AVVISO: Non è stato possibile loggare (%v)\n", err)
		} else {
			fmt.Println("✓ Ordine loggato")
		}
	}

	return &orderRecord
}

// ==================== Esempio di utilizzo ====================

// MyDatabase è una simulazione di database per l'esempio
type MyDatabase struct {
	Orders []OrderRecord
}

func (db *MyDatabase) SaveOrder(order OrderRecord) error {
	db.Orders = append(db.Orders, order)
	return nil
}

// MyEmailService è una simulazione di servizio email per l'esempio
type MyEmailService struct{}

func (e *MyEmailService) Send(to, subject, body string) error {
	fmt.Println("\n📧 EMAIL INVIATA:")
	fmt.Printf("   A: %s\n", to)
	fmt.Printf("   Oggetto: %s\n", subject)
	fmt.Printf("   Corpo:\n%s\n", body)
	return nil
}

func main() {
	myDatabase := &MyDatabase{Orders: []OrderRecord{}}
	myEmail := &MyEmailService{}

	// Ordine di esempio
	sampleOrder := OrderData{
		CustomerName:  "Mario Rossi",
		CustomerEmail: "mario@example.com",
		IsVipCustomer: true,
		Items: []Item{
			{Name: "Laptop", Price: 800, Quantity: 1},
			{Name: "Mouse", Price: 25, Quantity: 2},
		},
	}

	fmt.Println("============================================================")
	fmt.Println("INIZIO ELABORAZIONE ORDINE")
	fmt.Println("============================================================")
	ProcessOrder(sampleOrder, myDatabase, myEmail)
	fmt.Println("\n============================================================")
	fmt.Println("FINE ELABORAZIONE ORDINE")
	fmt.Println("============================================================")
}
