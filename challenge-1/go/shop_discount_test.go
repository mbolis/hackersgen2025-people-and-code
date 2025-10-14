package main_test

import (
	"fmt"
	"go/ast"
	"go/parser"
	"go/token"
	"math"
	"os"
	"regexp"
	"strings"
	"testing"

	main "challenge_1"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// Testa che il calcolo degli sconti funzioni correttamente
func TestDiscountCalculation(t *testing.T) {
	t.Run("Cliente base senza sconto di quantità", func(t *testing.T) {
		result := main.CalculateDiscount(100, "basic", 1)
		assert.Equal(t, 100.0, result, "Cliente basic dovrebbe pagare prezzo pieno")
	})
	t.Run("Cliente premium dovrebbe avere 15% di sconto", func(t *testing.T) {
		result := main.CalculateDiscount(100, "premium", 1)
		assert.Equal(t, 85.0, result, "Cliente premium: 100 * 0.85 = 85, ottenuto %f", result)
	})
	t.Run("Cliente VIP dovrebbe avere 25% di sconto", func(t *testing.T) {
		result := main.CalculateDiscount(100, "vip", 1)
		assert.Equal(t, 75.0, result, "Cliente VIP: 100 * 0.75 = 75, ottenuto %f", result)
	})
	t.Run("5 prodotti = 5% di sconto aggiuntivo", func(t *testing.T) {
		result := main.CalculateDiscount(100, "basic", 5)
		expected := 100 * 0.95
		assert.Equal(t, expected, result, "Sconto quantità 5: %f, ottenuto %f", expected, result)
	})
	t.Run("10 prodotti = 10% di sconto aggiuntivo", func(t *testing.T) {
		result := main.CalculateDiscount(100, "basic", 10)
		expected := 100 * 0.9
		assert.Equal(t, expected, result, "Sconto quantità 10: %f, ottenuto %f", expected, result)
	})
	t.Run("Ordini > €100 ottengono 2% di sconto aggiuntivo", func(t *testing.T) {
		result := main.CalculateDiscount(150, "basic", 1)
		expected := 150 * 0.98
		assert.Equal(t, expected, result, "Sconto ordine grande: %f, ottenuto %f", expected, result)
	})
}

// Testa che il calcolo del totale dell'ordine funzioni
func TestTotalOrderCalculation(t *testing.T) {
	t.Run("Ordine semplice di un cliente basic", func(t *testing.T) {
		order := main.CalculateTotalOrder([]main.Item{{50, 1}}, "basic")
		assert.Equal(t, order.Subtotal, 50.0, "Subtotal should be 50")
		expectedTax := math.Round(50*0.22*100) / 100
		assert.Equal(t, order.Tax, expectedTax, "Tax calculation")
	})
	t.Run("Cliente premium dovrebbe avere sconto", func(t *testing.T) {
		order := main.CalculateTotalOrder([]main.Item{{100, 1}}, "premium")
		// €100 * 0.85 (premium) = €85
		// Tasse: €85 * 0.22 = €18.70
		// Totale: €85 + €18.70 = €103.70
		assert.Equal(t, order.Subtotal, 85.0, "Premium subtotal should be 85")
		expected := math.Round((85+85*0.22)*100) / 100
		assert.Equal(t, order.Total, expected, "Premium total calculation")
	})
	t.Run("Ordini > €500 ottengono ulteriore 5% di sconto", func(t *testing.T) {
		order := main.CalculateTotalOrder([]main.Item{{200, 3}}, "basic")
		// Subtotale: €600
		// Tasse: €600 * 0.22 = €132
		// Totale prima sconto: €732
		// Con sconto 5%: €732 * 0.98 * 0.95 = €681.49
		assert.True(t, order.DiscountApplied, "Discount should be applied")
		expected := math.Round(732*0.98*0.95*100) / 100
		assert.Equal(t, order.Total, expected, "Large order final discount")
	})
}

// Testa le descrizioni dei tipi di cliente
func TestCustomerTierDescription(t *testing.T) {
	t.Run("Basic tier description", func(t *testing.T) {
		result := main.GetCustomerTierDescription("basic")
		assert.Contains(t, result, "Base")
	})
	t.Run("Premium tier description", func(t *testing.T) {
		result := main.GetCustomerTierDescription("premium")
		assert.Contains(t, result, "Premium")
	})
	t.Run("VIP tier description", func(t *testing.T) {
		result := main.GetCustomerTierDescription("vip")
		assert.Contains(t, result, "VIP")
	})
}

// Test che verificano se hai estratto correttamente le costanti
func TestCodeQuality(t *testing.T) {
	const testSubjectPath = "shop_discount.go"

	/*
		Questo test cerca costanti PascalCase nel modulo.
		Se questo fallisce, significa che non hai ancora estratto tutte le costanti dal codice.
	*/
	t.Run("Verifica che il file contenga costanti ben nominate", func(t *testing.T) {
		source, err := getFileSource(testSubjectPath)
		require.NoError(t, err)

		// Controlla che ci siano costanti (linee con `const PascalCase = numero/stringa`)
		var hasConstants bool
		reConstant := regexp.MustCompile(`(?m)^const\s+[A-Z][A-Za-z]+\s*=`)
		for line := range strings.Lines(source) {
			if reConstant.MatchString(line) {
				hasConstants = true
				break
			}
		}

		assert.True(t, hasConstants,
			"Non sembra che tu abbia estratto costanti ben nominate. Cerca valori come 0.85, 0.75, 100, 500, 0.22 e convertili in costanti CONST_CASE.",
		)
	})

	/*
		(Questo è un check semplice — se vedi numeri come 0.85, 0.75, significa che non li hai ancora estratti a costanti!)
	*/
	t.Run("Controlla che la funzione CalculateDiscount non contenga numeri magici schiantati", func(t *testing.T) {
		source, err := getFunctionSource(testSubjectPath, "CalculateDiscount")
		require.NoError(t, err)

		// Lista di valori che DOVREBBERO essere stati estratti
		magicValues := []string{"0.85", "0.75", "0.9", "0.95", "0.98", "10", "5", "100"}

		var foundMagic []string
		for _, value := range magicValues {
			if strings.Contains(source, value) {
				foundMagic = append(foundMagic, value)
			}
		}

		assert.Empty(t, foundMagic,
			"⚠️  Attenzione! Trovati numeri magici: %v. Converti questi numeri in costanti ben nominate (CONST_CASE)",
			foundMagic,
		)
	})

	/*
		(Questo è un check superficiale — si accerta solo che di non trovare i nomi originari,
		assicurati però che i nomi da te scelti siano chiari e significativi!)
	*/
	t.Run("Controlla che la funzione CalculateTotalOrder non contenga variabili di una sola lettera", func(t *testing.T) {
		source, err := getFunctionSource(testSubjectPath, "CalculateTotalOrder")
		require.NoError(t, err)

		// Lista di nomi che DOVREBBERO essere stati cambiati
		varNames := strings.Split("diPQst", "")
		// Trova istanze dei nomi circondati da caratteri non-parola
		var foundVars []string
		for _, varName := range varNames {
			reVar := regexp.MustCompile(`\b` + varName + `\b`)
			if reVar.MatchString(source) {
				foundVars = append(foundVars, varName)
			}
		}

		assert.Empty(t, foundVars,
			"⚠️  Attenzione! Trovate variabili di una lettera: %v. Rinomina queste variabili con nomi significativi",
			foundVars,
		)
	})
}

// HELPER:
// Funzioni di utilità per estrarre il codice del programma

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
