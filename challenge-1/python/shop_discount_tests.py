import re
import pytest
from shop_discount import (
    calculate_discount,
    calculate_total_order,
    get_customer_tier_description,
)


class TestDiscountCalculation:
    """Testa che il calcolo degli sconti funzioni correttamente."""
    
    def test_basic_customer_no_discount(self):
        """Cliente base senza sconto di quantità."""
        price = 100
        result = calculate_discount(price, "basic", 1)
        assert result == 100, "Cliente basic dovrebbe pagare prezzo pieno"
    
    def test_premium_customer_discount(self):
        """Cliente premium dovrebbe avere 15% di sconto."""
        price = 100
        result = calculate_discount(price, "premium", 1)
        assert result == 85, f"Cliente premium: 100 * 0.85 = 85, ottenuto {result}"
    
    def test_vip_customer_discount(self):
        """Cliente VIP dovrebbe avere 25% di sconto."""
        price = 100
        result = calculate_discount(price, "vip", 1)
        assert result == 75, f"Cliente VIP: 100 * 0.75 = 75, ottenuto {result}"
    
    def test_quantity_discount_5_items(self):
        """5 prodotti = 5% di sconto aggiuntivo."""
        price = 100
        result = calculate_discount(price, "basic", 5)
        expected = 100 * 0.95
        assert result == expected, f"Sconto quantità 5: {expected}, ottenuto {result}"
    
    def test_quantity_discount_10_items(self):
        """10 prodotti = 10% di sconto aggiuntivo."""
        price = 100
        result = calculate_discount(price, "basic", 10)
        expected = 100 * 0.9
        assert result == expected, f"Sconto quantità 10: {expected}, ottenuto {result}"
    
    def test_large_order_discount(self):
        """Ordini > €100 ottengono 2% di sconto aggiuntivo."""
        price = 150
        result = calculate_discount(price, "basic", 1)
        expected = 150 * 0.98
        assert result == expected, f"Sconto ordine grande: {expected}, ottenuto {result}"


class TestTotalOrderCalculation:
    """Testa che il calcolo del totale dell'ordine funzioni."""
    
    def test_order_basic_customer(self):
        """Ordine semplice di un cliente basic."""
        products = [(50, 1)]  # 1x €50
        order = calculate_total_order(products, "basic")
        assert order["subtotal"] == 50
        assert order["tax"] == round(50 * 0.22, 2)
    
    def test_order_premium_customer(self):
        """Cliente premium dovrebbe avere sconto."""
        products = [(100, 1)]
        order = calculate_total_order(products, "premium")
        # €100 * 0.85 (premium) = €85
        # Tasse: €85 * 0.22 = €18.70
        # Totale: €85 + €18.70 = €103.70
        assert order["subtotal"] == 85
        assert order["total"] == round(85 + 85 * 0.22, 2)
    
    def test_large_order_gets_final_discount(self):
        """Ordini > €500 ottengono ulteriore 5% di sconto."""
        products = [(200, 3)]  # 3x €200 = €600
        order = calculate_total_order(products, "basic")
        # Subtotale: €600
        # Tasse: €600 * 0.22 = €132
        # Totale prima sconto: €732
        # Con sconto 5%: €732 * 0.98 * 0.95 = €681.49
        assert order["discount_applied"] == True
        assert order["total"] == round(732 * 0.98 * 0.95, 2)


class TestCustomerTierDescription:
    """Testa le descrizioni dei tipi di cliente."""
    
    def test_basic_tier_description(self):
        result = get_customer_tier_description("basic")
        assert "Base" in result
    
    def test_premium_tier_description(self):
        result = get_customer_tier_description("premium")
        assert "Premium" in result
    
    def test_vip_tier_description(self):
        result = get_customer_tier_description("vip")
        assert "VIP" in result


class TestCodeQuality:
    """Test che verificano se hai estratto correttamente le costanti."""
    
    def test_constants_extracted(self):
        """
        Verifica che il file contenga costanti ben nominate.
        
        Questo test cerca costanti CONST_CASE nel modulo.
        Se questo fallisce, significa che non hai ancora estratto
        tutte le costanti dal codice.
        """
        import shop_discount
        import inspect
        
        # Ottieni il source code del modulo
        source = inspect.getsource(shop_discount)
        
        # Controlla che ci siano costanti (linee con CONST_CASE = numero/stringa)
        has_constants = any(
            line.strip() for line in source.split('\n')
            if '=' in line and any(
                word.isupper() for word in line.split('=')[0].split()
            ) and not line.strip().startswith('#')
        )
        
        if not has_constants:
            pytest.fail(
                "Non sembra che tu abbia estratto costanti ben nominate. "
                "Cerca valori come 0.85, 0.75, 100, 500, 0.22 e convertili in costanti CONST_CASE."
            )
    
    def test_no_magic_numbers_in_discount_function(self):
        """
        Controlla che la funzione calculate_discount non contenga
        numeri magici schiantati.
        
        (Questo è un check semplice — se vedi numeri come 0.85, 0.75,
        significa che non li hai ancora estratti a costanti!)
        """
        import shop_discount
        import inspect
        
        source = inspect.getsource(shop_discount.calculate_discount)
        
        # Lista di valori che DOVREBBERO essere stati estratti
        magic_values = ['0.85', '0.75', '0.9', '0.95', '0.98', '10', '5', '100']
        
        found_magic = [val for val in magic_values if val in source]
        
        if found_magic:
            pytest.fail(
                f"⚠️  Attenzione! Trovati numeri magici: {", ".join(found_magic)}. "
                "Converti questi numeri in costanti ben nominate (CONST_CASE)"
            )

    def test_no_bad_variable_names_in_total_order_function(self):
        """
        Controlla che la funzione calculate_total_order non contenga
        variabili di una sola lettera.
        
        (Questo è un check superficiale — si accerta solo che di non trovare
        i nomi originari, assicurati però che i nomi da te scelti siano chiari
        e significativi!)
        """
        import shop_discount
        import inspect
        
        source = inspect.getsource(shop_discount.calculate_total_order)
        
        # Lista di nomi che DOVREBBERO essere stati cambiati
        var_names = "dpqst"
        # Trova istanze dei nomi circondati da caratteri non-parola
        found_vars = [var for var in var_names if re.search(f"\\b{var}\\b", source)]
        
        if found_vars:
            pytest.fail(
                f"⚠️  Attenzione! Trovate variabili di una lettera: {", ".join(found_vars)}. "
                "Rinomina queste variabili con nomi significativi"
            )