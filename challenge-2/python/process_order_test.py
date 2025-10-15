import process_order as test_subject
import pytest


class MockDatabase:
    """Database fasullo per i test"""

    def __init__(self):
        self.orders = []
        self._fail_next = False

    def save_order(self, order):
        if self._fail_next:
            self._fail_next = False
            raise Exception("[Test] email send failure")

        self.orders.append(order)


class MockEmailService:
    """Servizio email fasullo per i test"""

    def __init__(self):
        self.sent_emails = []
        self._fail_next = False

    def send(self, to, subject, body):
        if self._fail_next:
            self._fail_next = False
            raise Exception("[Test] email send failure")

        self.sent_emails.append({"to": to, "subject": subject, "body": body})


class TestOrderValidation:
    """Test: la validazione deve funzionare."""

    mock_database = MockDatabase()
    mock_email = MockEmailService()

    def test_order_without_items(self):
        """Ordine senza articoli."""

        result = test_subject.process_order(
            {"customer_name": "Mario", "customer_email": "mario@test.com", "items": []},
            self.mock_database,
            self.mock_email,
        )
        assert result is None, "Ordine vuoto dovrebbe essere rifiutato"

    def test_order_without_email(self):
        """Ordine senza email."""

        result = test_subject.process_order(
            {
                "customer_name": "Mario",
                "items": [{"name": "Prodotto", "price": 10, "quantity": 1}],
            },
            self.mock_database,
            self.mock_email,
        )
        assert result is None, "Ordine senza email dovrebbe essere rifiutato"


class TestOrderCalculation:
    """Test: il calcolo del totale deve essere corretto."""

    mock_database = MockDatabase()
    mock_email = MockEmailService()

    def test_basic_order(self):
        """Il totale deve essere calcolato correttamente."""

        order = {
            "customer_name": "Mario",
            "customer_email": "mario@test.com",
            "is_vip_customer": False,
            "items": [
                {"name": "Prodotto A", "price": 100, "quantity": 2},  # 200
                {"name": "Prodotto B", "price": 50, "quantity": 1},  # 50
            ],
        }
        # Subtotale: 250, nessuno sconto, tasse 22%
        # Totale: 250 * 1.22 = 305

        result = test_subject.process_order(
            order,
            self.mock_database,
            self.mock_email,
        )
        assert result is not None, "Ordine valido dovrebbe essere accettato"
        assert result["subtotal"] == 250, (
            f"Subtotale dovrebbe essere 250, è {result['subtotal']}"
        )
        assert abs(result["total"] - 305.0) < 0.01, (
            f"Totale dovrebbe essere ~305, è {result['total']}"
        )

    def test_vip_discount(self):
        """Lo sconto VIP deve essere applicato correttamente"""

        # Ordine VIP di €100
        order = {
            "customer_name": "Mario VIP",
            "customer_email": "mario@test.com",
            "is_vip_customer": True,
            "items": [
                {"name": "Prodotto", "price": 100, "quantity": 1},
            ],
        }
        # Subtotale: 100, sconto VIP 15% = 85, tasse 22% = 103.7

        result = test_subject.process_order(
            order,
            self.mock_database,
            self.mock_email,
        )
        assert result is not None
        assert result["vip"] is True
        assert result["subtotal"] == 85, (
            f"Subtotale VIP dovrebbe essere 85, è {result['subtotal']}"
        )

    def test_large_order_discount(self):
        """Lo sconto per ordini grandi deve essere applicato"""

        # Ordine di €600 (senza VIP)
        order = {
            "customer_name": "Mario Grosso",
            "customer_email": "mario@test.com",
            "is_vip_customer": False,
            "items": [
                {"name": "Prodotto Caro", "price": 600, "quantity": 1},
            ],
        }
        # Subtotale: 600, sconto grandi ordini 10% = 540, tasse 22% = 658.8

        result = test_subject.process_order(
            order,
            self.mock_database,
            self.mock_email,
        )
        assert result is not None
        assert result["subtotal"] == 540, (
            f"Subtotale con sconto dovrebbe essere 540, è {result['subtotal']}"
        )


class TestDatabaseStorage:
    """Test: l'ordine deve essere salvato nel database."""

    mock_email = MockEmailService()

    def test_save_order(self):
        """L'ordine viene salvato correttamente."""

        order = {
            "customer_name": "Mario",
            "customer_email": "mario@test.com",
            "items": [
                {"name": "Prodotto", "price": 50, "quantity": 1},
            ],
        }

        mock_database = MockDatabase()

        result = test_subject.process_order(
            order,
            mock_database,
            self.mock_email,
        )
        assert result is not None
        assert len(mock_database.orders) == 1, (
            "Ordine dovrebbe essere salvato nel database"
        )
        assert mock_database.orders[0] == {
            **order,
            "status": "pending",
            "subtotal": 50,
            "tax": 11.0,
            "total": 61.0,
            "vip": False,
        }

    def test_save_order_error(self):
        """Se l'ordine non viene salvato, la funzione non ritorna niente."""

        order = {
            "customer_name": "Mario",
            "customer_email": "mario@test.com",
            "items": [
                {"name": "Prodotto", "price": 50, "quantity": 1},
            ],
        }

        mock_database = MockDatabase()
        mock_database._fail_next = True

        result = test_subject.process_order(
            order,
            mock_database,
            self.mock_email,
        )
        assert result is None
        assert len(mock_database.orders) == 0, (
            "Ordine non dovrebbe essere salvato nel database"
        )


class TestEmailSending:
    """Test: Dopo l'ordine deve essere inviata un'email di conferma."""

    mock_database = MockDatabase()

    def test_send_email(self):
        """L'email di conferma viene inviata correttamente."""

        order = {
            "customer_name": "Mario",
            "customer_email": "mario@test.com",
            "items": [
                {"name": "Prodotto", "price": 50, "quantity": 1},
            ],
        }

        mock_email = MockEmailService()
        result = test_subject.process_order(
            order,
            self.mock_database,
            mock_email,
        )
        assert result is not None
        assert len(mock_email.sent_emails) == 1, "Email dovrebbe essere stata inviata"
        assert mock_email.sent_emails[0] == {
            "to": "mario@test.com",
            "subject": "Ordine confermato - €61.00",
            "body": """
Grazie Mario!

Il tuo ordine è stato confermato.
Totale: €61.00

Dettagli:
- Prodotto x1: €50.00
""",
        }

    def test_send_email_error(self):
        """Se il servizio email ritorna un errore, l'ordine risulta comunque accettato."""

        order = {
            "customer_name": "Mario",
            "customer_email": "mario@test.com",
            "items": [
                {"name": "Prodotto", "price": 50, "quantity": 1},
            ],
        }

        mock_email = MockEmailService()
        mock_email._fail_next = True

        result = test_subject.process_order(
            order,
            self.mock_database,
            mock_email,
        )
        assert result is not None, "Errore di invio email non deve essere bloccante"
        assert len(mock_email.sent_emails) == 0, (
            "Ordine non dovrebbe essere salvato nel database"
        )


class TestLogging:
    """Test: L'ordine deve essere loggato."""

    mock_database = MockDatabase()
    mock_email = MockEmailService()

    def test_order_logged(self):
        """L'ordine viene loggato correttamente."""

        order = {
            "customer_name": "Mario",
            "customer_email": "mario@test.com",
            "items": [
                {"name": "Prodotto", "price": 50, "quantity": 1},
            ],
        }

        # Tronchiamo il file di log... vero che ci starebbe bene una costante qui?
        with open("orders.log", "w"):
            pass

        result = test_subject.process_order(
            order,
            self.mock_database,
            self.mock_email,
        )
        assert result is not None

        with open("orders.log") as log:
            logged = log.read()
            assert logged == "[ORDINE] Mario - €61.00 - VIP: False\n", (
                "Ordine dovrebbe essere loggato"
            )


EXPECTED_HELPERS = [
    "validate_order",
    "calculate_totals",
    "save_order",
    "send_confirmation",
    "log_order",
]


class TestCodeQuality:
    """Test che verificano se hai riorganizzato correttamente il codice."""

    def test_expected_helpers_exist(self):
        """
        Verifica che il file contenga delle funzioni "helper" per
        incapsulare ciascuna responsabilità.

        Questo test cerca delle funzioni con nomi scelti arbitrariamente,
        niente affatto obbligatori!
        Se hai usato nomi diversi, modifica la costante EXPECTED_HELPERS
        nel file di test per far passare questo test.
        """
        missing_helpers = [
            helper for helper in EXPECTED_HELPERS if not hasattr(test_subject, helper)
        ]
        if missing_helpers:
            pytest.fail(
                f"⚠️  Attenzione! Mancano alcune funzioni attese: {', '.join(missing_helpers)}"
                "Spezza `process_order` in funzioni con responsabilità singola."
            )

    def test_process_order_exists(self):
        """Verifica che la funzione `process_order` esista ancora."""

        assert hasattr(test_subject, "process_order"), (
            "⚠️  Non hai più la funzione `process_order`. "
            "Dovrebbe restare come coordinatore che chiama le funzioni più piccole."
        )

    def test_process_order_signature(self):
        """Verifica che la firma della funzione `process_order` non sia cambiata."""

        import inspect

        sig = inspect.signature(test_subject.process_order)
        expected = ["order_data", "database", "email_service"]
        assert expected == sig.parameters, (
            f"⚠️  Parametri errati in process_order: attesi {expected}, trovati {sig.parameters}"
        )

    @pytest.mark.parametrize("func_name", EXPECTED_HELPERS)
    def test_helpers_have_docstrings(self, func_name):
        """
        Verifica che le funzioni "helper" aggiunte siano documentate.

        (Questo è un test 'parametrico': vuol dire che viene eseguito
        una volta per ogni nome nella lista EXPECTED_HELPERS)
        """

        if hasattr(test_subject, func_name):
            func = getattr(test_subject, func_name)
            assert func.__doc__, (
                f"⚠️  Aggiungi una docstring a `{func_name}` per descriverne la responsabilità."
            )

    def test_helpers_called_in_process_order(self):
        """
        Verifica che ciascuna funzione "helper", una volta implementata
        sia usata nella funzione `process_order`.
        """

        import inspect

        source = inspect.getsource(test_subject.process_order)
        missing_calls = [name for name in EXPECTED_HELPERS if name not in source]
        if missing_calls:
            pytest.fail(
                f"⚠️  Alcune funzioni non sono richiamate da `process_order`: {', '.join(missing_calls)}"
            )
