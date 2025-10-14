/*
Sistema di gestione sconti per uno shop online.

Il codice funziona, ma è pieno di magic numbers e magic strings.
Estrai tutte le costanti e dai loro nomi significativi!
*/
package main

import (
	"fmt"
	"math"
	"strings"
)

// Item rappresenta un prodotto con prezzo e quantità
type Item struct {
	P float64
	Q int
}

// Order contiene i dati di un ordine
type Order struct {
	Subtotal        float64
	Tax             float64
	Total           float64
	DiscountApplied bool
}

/*
CalculateDiscount calcola il prezzo finale con sconto.

Parametri:
  - price: prezzo unitario del prodotto
  - customerType: tipo di cliente ("basic", "premium", "vip")
  - quantity: quantità di prodotti acquistati

Ritorna il prezzo finale dopo applicazione di tutti gli sconti
*/
func CalculateDiscount(price float64, customerType string, quantity int) float64 {
	// Sconto per clienti premium
	if customerType == "premium" {
		price = price * 0.85
	}

	// Sconto per clienti VIP
	if customerType == "vip" {
		price = price * 0.75
	}

	// Sconto per quantità
	if quantity >= 10 {
		price = price * 0.9
	} else if quantity >= 5 {
		price = price * 0.95
	}

	// Sconto minimo garantito per ordini grandi
	if price > 100 {
		price = price * 0.98
	}

	return math.Round(price*100) / 100
}

/*
CalculateTotalOrder calcola il totale di un ordine applicando sconti per cliente e tasse.

Parametri:
  - products: slice di Product
  - customerType: tipo di cliente ("basic", "premium", "vip")

Ritorna una struct OrderData con subtotale, tasse e totale finale
*/
func CalculateTotalOrder(items []Item, customerType string) Order {
	s := 0.0

	for _, i := range items {
		discountedPrice := CalculateDiscount(i.P, customerType, i.Q)
		s = s + (discountedPrice * float64(i.Q))
	}

	// Tassa sul valore
	t := s * 0.22

	tot := s + t

	// Sconto finale se l'ordine è superiore a 500 euro
	if tot > 500 {
		tot = tot * 0.95
	}

	return Order{
		Subtotal:        math.Round(s*100) / 100,
		Tax:             math.Round(t*100) / 100,
		Total:           math.Round(tot*100) / 100,
		DiscountApplied: tot != s+t,
	}
}

/*
GetCustomerTierDescription ritorna una descrizione del livello cliente.
*/
func GetCustomerTierDescription(cType string) string {
	switch cType {
	case "basic":
		return "Cliente Base"
	case "premium":
		return "Cliente Premium (15% sconto base)"
	case "vip":
		return "Cliente VIP (25% sconto base)"
	default:
		return "Tipo cliente sconosciuto"
	}
}

/*
FormatOrderReceipt formatta uno scontrino leggibile.
*/
func FormatOrderReceipt(orderData Order, customerType string) string {
	receipt := ""
	receipt += strings.Repeat("=", 40) + "\n"
	receipt += "SCONTRINO ORDINE\n"
	receipt += strings.Repeat("=", 40) + "\n"
	receipt += fmt.Sprintf("Tipo cliente: %s\n", GetCustomerTierDescription(customerType))
	receipt += strings.Repeat("-", 40) + "\n"
	receipt += fmt.Sprintf("Subtotale: € %.2f\n", orderData.Subtotal)
	receipt += fmt.Sprintf("Tasse (22%%): € %.2f\n", orderData.Tax)

	if orderData.DiscountApplied {
		receipt += "SCONTO PER ORDINE GRANDE: -5%\n"
	}

	receipt += strings.Repeat("-", 40) + "\n"
	receipt += fmt.Sprintf("TOTALE: € %.2f\n", orderData.Total)
	receipt += strings.Repeat("=", 40) + "\n"

	return receipt
}

// Esempio di utilizzo
func main() {
	products := []Item{
		{P: 50, Q: 2}, // 2x prodotto da €50
		{P: 30, Q: 8}, // 8x prodotto da €30
	}

	order := CalculateTotalOrder(products, "premium")
	fmt.Print(FormatOrderReceipt(order, "premium"))

	fmt.Println("\nAltro esempio:")
	products2 := []Item{
		{P: 100, Q: 3},
		{P: 50, Q: 5},
	}
	order2 := CalculateTotalOrder(products2, "vip")
	fmt.Print(FormatOrderReceipt(order2, "vip"))
}
