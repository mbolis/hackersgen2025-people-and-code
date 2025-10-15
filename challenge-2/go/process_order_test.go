package main_test

import (
	"errors"
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"os"
	"strings"
	"testing"

	main "challenge_2"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// MockDatabase è un database fasullo per i test
type MockDatabase struct {
	Orders   []main.OrderRecord
	FailNext bool
}

func (db *MockDatabase) SaveOrder(order main.OrderRecord) error {
	if db.FailNext {
		db.FailNext = false
		return errors.New("[Test] database save failure")
	}
	db.Orders = append(db.Orders, order)
	return nil
}

// MockEmailService è un servizio email fasullo per i test
type MockEmailService struct {
	SentEmails []EmailMessage
	FailNext   bool
}

type EmailMessage struct {
	To      string
	Subject string
	Body    string
}

func (e *MockEmailService) Send(to, subject, body string) error {
	if e.FailNext {
		e.FailNext = false
		return errors.New("[Test] email send failure")
	}
	e.SentEmails = append(e.SentEmails, EmailMessage{To: to, Subject: subject, Body: body})
	return nil
}

// Test: la validazione deve funzionare
func TestOrderValidation(t *testing.T) {
	mockDatabase := &MockDatabase{}
	mockEmail := &MockEmailService{}

	t.Run("Ordine senza articoli", func(t *testing.T) {
		result := main.ProcessOrder(
			main.OrderData{
				CustomerName:  "Mario",
				CustomerEmail: "mario@test.com",
				Items:         []main.Item{},
			},
			mockDatabase,
			mockEmail,
		)
		assert.Nil(t, result, "Ordine vuoto dovrebbe essere rifiutato")
	})

	t.Run("Ordine senza email", func(t *testing.T) {
		result := main.ProcessOrder(
			main.OrderData{
				CustomerName: "Mario",
				Items:        []main.Item{{Name: "Prodotto", Price: 10, Quantity: 1}},
			},
			mockDatabase,
			mockEmail,
		)
		assert.Nil(t, result, "Ordine senza email dovrebbe essere rifiutato")
	})
}

// Test: il calcolo del totale deve essere corretto
func TestOrderCalculation(t *testing.T) {
	mockDatabase := &MockDatabase{}
	mockEmail := &MockEmailService{}

	t.Run("Il totale deve essere calcolato correttamente", func(t *testing.T) {
		order := main.OrderData{
			CustomerName:  "Mario",
			CustomerEmail: "mario@test.com",
			IsVipCustomer: false,
			Items: []main.Item{
				{Name: "Prodotto A", Price: 100, Quantity: 2}, // 200
				{Name: "Prodotto B", Price: 50, Quantity: 1},  // 50
			},
		}
		// Subtotale: 250, nessuno sconto, tasse 22%
		// Totale: 250 * 1.22 = 305

		result := main.ProcessOrder(order, mockDatabase, mockEmail)
		assert.NotNil(t, result, "Ordine valido dovrebbe essere accettato")
		assert.Equal(t, 250.0, result.Subtotal, "Subtotale dovrebbe essere 250, è %f", result.Subtotal)
		assert.InDelta(t, 305.0, result.Total, 0.01, "Totale dovrebbe essere ~305, è %f", result.Total)
	})

	t.Run("Lo sconto VIP deve essere applicato correttamente", func(t *testing.T) {
		// Ordine VIP di €100
		order := main.OrderData{
			CustomerName:  "Mario VIP",
			CustomerEmail: "mario@test.com",
			IsVipCustomer: true,
			Items: []main.Item{
				{Name: "Prodotto", Price: 100, Quantity: 1},
			},
		}
		// Subtotale: 100, sconto VIP 15% = 85, tasse 22% = 103.7

		result := main.ProcessOrder(order, mockDatabase, mockEmail)
		assert.NotNil(t, result)
		assert.True(t, result.Vip)
		assert.Equal(t, 85.0, result.Subtotal, "Subtotale VIP dovrebbe essere 85, è %f", result.Subtotal)
	})

	t.Run("Lo sconto per ordini grandi deve essere applicato", func(t *testing.T) {
		// Ordine di €600 (senza VIP)
		order := main.OrderData{
			CustomerName:  "Mario Grosso",
			CustomerEmail: "mario@test.com",
			IsVipCustomer: false,
			Items: []main.Item{
				{Name: "Prodotto Caro", Price: 600, Quantity: 1},
			},
		}
		// Subtotale: 600, sconto grandi ordini 10% = 540, tasse 22% = 658.8

		result := main.ProcessOrder(order, mockDatabase, mockEmail)
		assert.NotNil(t, result)
		assert.Equal(t, 540.0, result.Subtotal, "Subtotale con sconto dovrebbe essere 540, è %f", result.Subtotal)
	})
}

// Test: l'ordine deve essere salvato nel database
func TestDatabaseStorage(t *testing.T) {
	mockEmail := &MockEmailService{}

	t.Run("L'ordine viene salvato correttamente", func(t *testing.T) {
		order := main.OrderData{
			CustomerName:  "Mario",
			CustomerEmail: "mario@test.com",
			Items: []main.Item{
				{Name: "Prodotto", Price: 50, Quantity: 1},
			},
		}

		mockDatabase := &MockDatabase{}

		result := main.ProcessOrder(order, mockDatabase, mockEmail)
		assert.NotNil(t, result)
		assert.Equal(t, 1, len(mockDatabase.Orders), "Ordine dovrebbe essere salvato nel database")
		saved := mockDatabase.Orders[0]
		assert.Equal(t, "Mario", saved.CustomerName)
		assert.Equal(t, "mario@test.com", saved.CustomerEmail)
		assert.Equal(t, "pending", saved.Status)
		assert.Equal(t, 50.0, saved.Subtotal)
		assert.Equal(t, 11.0, saved.Tax)
		assert.Equal(t, 61.0, saved.Total)
		assert.False(t, saved.Vip)
	})

	t.Run("Se l'ordine non viene salvato, la funzione non ritorna niente", func(t *testing.T) {
		order := main.OrderData{
			CustomerName:  "Mario",
			CustomerEmail: "mario@test.com",
			Items: []main.Item{
				{Name: "Prodotto", Price: 50, Quantity: 1},
			},
		}

		mockDatabase := &MockDatabase{FailNext: true}

		result := main.ProcessOrder(order, mockDatabase, mockEmail)
		assert.Nil(t, result)
		assert.Equal(t, 0, len(mockDatabase.Orders), "Ordine non dovrebbe essere salvato nel database")
	})
}

// Test: Dopo l'ordine deve essere inviata un'email di conferma
func TestEmailSending(t *testing.T) {
	mockDatabase := &MockDatabase{}

	t.Run("L'email di conferma viene inviata correttamente", func(t *testing.T) {
		order := main.OrderData{
			CustomerName:  "Mario",
			CustomerEmail: "mario@test.com",
			Items: []main.Item{
				{Name: "Prodotto", Price: 50, Quantity: 1},
			},
		}

		mockEmail := &MockEmailService{}
		result := main.ProcessOrder(order, mockDatabase, mockEmail)
		assert.NotNil(t, result)
		assert.Equal(t, 1, len(mockEmail.SentEmails), "Email dovrebbe essere stata inviata")
		email := mockEmail.SentEmails[0]
		assert.Equal(t, "mario@test.com", email.To)
		assert.Equal(t, "Ordine confermato - €61.00", email.Subject)
		expectedBody := `
Grazie Mario!

Il tuo ordine è stato confermato.
Totale: €61.00

Dettagli:
- Prodotto x1: €50.00
`
		assert.Equal(t, expectedBody, email.Body)
	})

	t.Run("Se il servizio email ritorna un errore, l'ordine risulta comunque accettato", func(t *testing.T) {
		order := main.OrderData{
			CustomerName:  "Mario",
			CustomerEmail: "mario@test.com",
			Items: []main.Item{
				{Name: "Prodotto", Price: 50, Quantity: 1},
			},
		}

		mockEmail := &MockEmailService{FailNext: true}

		result := main.ProcessOrder(order, mockDatabase, mockEmail)
		assert.NotNil(t, result, "Errore di invio email non deve essere bloccante")
		assert.Equal(t, 0, len(mockEmail.SentEmails), "Email non dovrebbe essere stata inviata")
	})
}

// Test: L'ordine deve essere loggato
func TestLogging(t *testing.T) {
	mockDatabase := &MockDatabase{}
	mockEmail := &MockEmailService{}

	t.Run("L'ordine viene loggato correttamente", func(t *testing.T) {
		order := main.OrderData{
			CustomerName:  "Mario",
			CustomerEmail: "mario@test.com",
			Items: []main.Item{
				{Name: "Prodotto", Price: 50, Quantity: 1},
			},
		}

		// Tronchiamo il file di log... vero che ci starebbe bene una costante qui?
		err := os.Truncate("orders.log", 0)
		require.NoError(t, err)

		result := main.ProcessOrder(order, mockDatabase, mockEmail)
		assert.NotNil(t, result)

		logData, err := os.ReadFile("orders.log")
		require.NoError(t, err)
		logged := string(logData)
		assert.Equal(t, "[ORDINE] Mario - €61.00 - VIP: false\n", logged, "Ordine dovrebbe essere loggato")
	})
}

var expectedHelpers = []string{
	"validateOrder",
	"calculateTotals",
	"saveOrder",
	"sendConfirmation",
	"logOrder",
}

// Test che verificano se hai riorganizzato correttamente il codice
func TestCodeQuality(t *testing.T) {
	const testSubjectPath = "process_order.go"

	t.Run("Verifica che il file contenga delle funzioni helper per incapsulare ciascuna responsabilità", func(t *testing.T) {
		/*
			Questo test cerca delle funzioni con nomi scelti arbitrariamente,
			niente affatto obbligatori!
			Se hai usato nomi diversi, modifica la variabile expectedHelpers
			nel file di test per far passare questo test.
		*/
		foundFunctions, err := getFunctionDecls(testSubjectPath)
		assert.NoError(t, err)

		var missingHelpers []string
		for _, helper := range expectedHelpers {
			if _, ok := foundFunctions[helper]; !ok {
				missingHelpers = append(missingHelpers, helper)
			}
		}

		if len(missingHelpers) > 0 {
			t.Errorf("⚠️  Attenzione! Mancano alcune funzioni attese: %s. "+
				"Spezza `ProcessOrder` in funzioni con responsabilità singola.",
				strings.Join(missingHelpers, ", "))
		}
	})

	t.Run("Verifica che la funzione `ProcessOrder` esista ancora", func(t *testing.T) {
		foundFunctions, err := getFunctionDecls(testSubjectPath)
		assert.NoError(t, err)

		_, found := foundFunctions["ProcessOrder"]

		assert.True(t, found, "❌  Non hai più la funzione `ProcessOrder`. "+
			"Dovrebbe restare come coordinatore che chiama le funzioni più piccole.")
	})

	t.Run("Verifica che la firma della funzione `ProcessOrder` non sia cambiata", func(t *testing.T) {
		foundFunctions, err := getFunctionDecls(testSubjectPath)
		assert.NoError(t, err)

		decl, found := foundFunctions["ProcessOrder"]
		if !found {
			t.SkipNow()
		}

		params := decl.Type.Params.List
		var paramNames []string
		for _, param := range params {
			for _, name := range param.Names {
				paramNames = append(paramNames, name.Name)
			}
		}
		expected := []string{"orderData", "database", "emailService"}
		assert.Equal(t, expected, paramNames,
			"❌  Parametri errati in `process_order`: attesi %s, trovati %s",
			strings.Join(expected, ", "),
			strings.Join(paramNames, ", "),
		)
	})

	t.Run("Verifica che le funzioni helper aggiunte siano documentate", func(t *testing.T) {
		/*
			Verifica che le funzioni "helper" aggiunte siano documentate.
		*/
		foundFunctions, err := getFunctionDecls(testSubjectPath)
		assert.NoError(t, err)

		for _, helper := range expectedHelpers {
			funcDecl, found := foundFunctions[helper]
			if !found {
				continue
			}
			if funcDecl.Doc == nil || len(funcDecl.Doc.List) == 0 {
				t.Errorf("⚠️  Aggiungi un commento a `%s` per descriverne la responsabilità.", helper)
			}
		}
	})

	t.Run("Verifica che ciascuna funzione helper sia usata nella funzione `ProcessOrder`", func(t *testing.T) {
		source, err := getFileSource(testSubjectPath)
		require.NoError(t, err)

		processOrderSource, err := getFunctionSource(testSubjectPath, "ProcessOrder")
		require.NoError(t, err)

		var missingCalls []string
		for _, name := range expectedHelpers {
			// Check if the function exists in the file
			if strings.Contains(source, fmt.Sprintf("func %s", name)) {
				// Check if it's called in ProcessOrder
				if !strings.Contains(processOrderSource, name) {
					missingCalls = append(missingCalls, name)
				}
			}
		}

		if len(missingCalls) > 0 {
			t.Errorf("⚠️  Alcune funzioni non sono richiamate da `ProcessOrder`: %s", strings.Join(missingCalls, ", "))
		}
	})
}

// HELPER: Funzioni di utilità per estrarre il codice del programma

// getFileSource gets the contents of a file
func getFileSource(filePath string) (string, error) {
	data, err := os.ReadFile(filePath)
	if err != nil {
		return "", fmt.Errorf("could not read source file %s: %w", filePath, err)
	}
	return string(data), nil
}

// getFunctionSource extracts a function's source code from a Go source file
func getFunctionSource(filePath string, funcName string) (string, error) {
	fset := token.NewFileSet()

	node, err := parser.ParseFile(fset, filePath, nil, 0)
	if err != nil {
		return "", fmt.Errorf("failed to parse file: %w", err)
	}

	// Find the function in the AST
	for _, decl := range node.Decls {
		funcDecl, ok := decl.(*ast.FuncDecl)
		if !ok {
			// not a function
			continue
		}

		if funcDecl.Name.Name == funcName {
			return extractSource(fset, filePath, funcDecl)
		}
	}

	return "", fmt.Errorf("function not found: %s", funcName)
}

// extractSource reads the actual source code for a function
func extractSource(fset *token.FileSet, filePath string, funcDecl *ast.FuncDecl) (string, error) {
	data, err := getFileSource(filePath)
	if err != nil {
		return "", err
	}

	start := fset.Position(funcDecl.Pos()).Offset
	end := fset.Position(funcDecl.End()).Offset
	return data[start:end], nil
}

// getFunctionDecls extracts the set of exported function names in a source file
func getFunctionDecls(filePath string) (map[string]*ast.FuncDecl, error) {
	fset := token.NewFileSet()
	node, err := parser.ParseFile(fset, filePath, nil, parser.ParseComments)
	if err != nil {
		return nil, err
	}

	foundFunctions := make(map[string]*ast.FuncDecl)
	for _, decl := range node.Decls {
		if funcDecl, ok := decl.(*ast.FuncDecl); ok && funcDecl.Recv == nil {
			foundFunctions[funcDecl.Name.Name] = funcDecl
		}
	}
	return foundFunctions, nil
}
