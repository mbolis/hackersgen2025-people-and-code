import os
from typing import Callable

import pytest
from PIL import Image
from steganography import (
    batch_proc,
    proc_meta,
)

# ================ Fixture (immagini di test) ================


@pytest.fixture
def test_png(tmp_path):
    """Crea un'immagine PNG di test 100x100 rossa"""
    png_path = tmp_path / "test.png"

    img = Image.new("RGB", (100, 100), color=(255, 0, 0))
    img.save(png_path, "PNG")

    return png_path


@pytest.fixture
def test_png_with_metadata(tmp_path):
    """Crea un'immagine PNG con metadati già incorporati"""
    png_path = tmp_path / "test_with_meta.png"

    img = Image.new("RGB", (100, 100), color=(0, 0, 255))
    img.save(png_path, "PNG")

    # Incorpora metadati
    metadata = {
        "author": "Test Author",
        "title": "Test Image",
        "date": "2025-10-15",
    }

    proc_meta(png_path, {"embed": True, "data": metadata, "overwrite": True})

    return png_path


@pytest.fixture
def multiple_test_pngs(tmp_path):
    """Crea 3 immagini PNG di test per batch processing"""
    images = []
    for i in range(3):
        png_path = tmp_path / f"test_{i}.png"
        img = Image.new("RGB", (100, 100), color=(0, 255, 0))
        img.save(png_path, "PNG")
        images.append(png_path)
    return images


# =========================== Test ===========================


class TestProcMeta:
    """Test per la funzione proc_meta"""

    def test_embed_basic_metadata(self, test_png):
        """Test incorporamento metadati base"""
        metadata = {"author": "John Doe", "year": 2025}

        result = proc_meta(test_png, {"embed": True, "data": metadata})

        assert isinstance(result, dict)
        assert result["success"] is True
        assert "path" in result
        assert os.path.exists(result["path"])

    def test_embed_with_output_path(self, test_png, tmp_path):
        """Test incorporamento con path output specificato"""
        with open(test_png, "rb") as file:
            original_bytes = file.read()

        metadata = {"title": "Test Image"}
        output = tmp_path / "embedded.png"

        result = proc_meta(test_png, {"embed": True, "data": metadata, "out": output})

        assert result is True
        assert os.path.exists(output)

        with open(test_png, "rb") as file:
            current_bytes = file.read()
        assert original_bytes == current_bytes

        with open(output, "rb") as file:
            saved_bytes = file.read()
        assert original_bytes != saved_bytes

    def test_embed_with_overwrite(self, test_png):
        """Test incorporamento sovrascrivendo il file originale"""
        with open(test_png, "rb") as file:
            original_bytes = file.read()

        metadata = {"description": "Overwritten"}

        result = proc_meta(
            test_png, {"embed": True, "data": metadata, "overwrite": True}
        )

        assert isinstance(result, dict)
        assert result["success"] is True

        with open(test_png, "rb") as file:
            current_bytes = file.read()
        assert original_bytes != current_bytes

    def test_extract_metadata(self, test_png_with_metadata):
        """Test estrazione metadati"""
        result = proc_meta(test_png_with_metadata, {"extract": True})

        assert result is not None
        assert isinstance(result, dict)
        assert "author" in result
        assert result["author"] == "Test Author"

    def test_extract_nonexistent_metadata(self, test_png):
        """Test estrazione quando non ci sono metadati"""
        result = proc_meta(test_png, {"extract": True}, raise_err=False)

        assert result is None

    def test_verify_with_metadata(self, test_png_with_metadata):
        """Test verifica presenza metadati"""
        result = proc_meta(test_png_with_metadata, {"verify": True})

        assert result is True

    def test_verify_without_metadata(self, test_png):
        """Test verifica quando non ci sono metadati"""
        result = proc_meta(test_png, {"verify": True})

        assert result is False

    def test_update_metadata(self, test_png_with_metadata, tmp_path):
        """Test aggiornamento metadati esistenti"""
        output = tmp_path / "updated.png"

        result = proc_meta(
            test_png_with_metadata,
            {"update": {"author": "New Author"}, "out": output},
        )

        assert isinstance(result, dict)
        assert result["success"] is True

        # Verifica che i metadati siano stati aggiornati
        extracted = proc_meta(output, {"extract": True})
        assert isinstance(extracted, dict)
        assert extracted["author"] == "New Author"
        assert extracted["title"] == "Test Image"  # Altri metadati preservati

    def test_nonexistent_file(self, tmp_path):
        """Test con file inesistente"""
        result = proc_meta(
            tmp_path / "nonexistent.png", {"extract": True}, raise_err=False
        )

        assert isinstance(result, dict)
        assert result["success"] is False
        assert result["error"] == "File not found"

    def test_invalid_file_format(self, tmp_path):
        """Test con file non-PNG"""
        text_file = tmp_path / "notpng.txt"
        text_file.write_text("not a png")

        result = proc_meta(text_file, {"extract": True}, raise_err=False)

        assert isinstance(result, dict)
        assert result["success"] is False
        assert result["error"] == "Not PNG"

    def test_different_channels(self, test_png):
        """Test incorporamento su canali RGB diversi"""
        with Image.open(test_png) as original:
            metadata = {"channel_test": "value"}

            # Canale rosso (0)
            result_r = proc_meta(test_png, {"embed": True, "data": metadata, "ch": 0})
            assert isinstance(result_r, dict)
            assert result_r["success"] is True
            with Image.open(result_r["path"]) as embed_r:
                assert not self._channel_eq(original, embed_r, (0,)), "Canale rosso"
                assert self._channel_eq(original, embed_r, (1, 2))

            # Canale verde (1)
            result_g = proc_meta(test_png, {"embed": True, "data": metadata, "ch": 1})
            assert isinstance(result_g, dict)
            assert result_g["success"] is True
            with Image.open(result_g["path"]) as embed_r:
                assert not self._channel_eq(original, embed_r, (1,)), "Canale verde"
                assert self._channel_eq(original, embed_r, (0, 2))

            # Tutti i canali (-1)
            result_all = proc_meta(
                test_png, {"embed": True, "data": metadata, "ch": -1}
            )
            assert isinstance(result_all, dict)
            assert result_all["success"] is True
            with Image.open(result_all["path"]) as embed_r:
                assert not self._channel_eq(original, embed_r, (0, 1, 2)), (
                    "Tutti i canali"
                )

    def _channel_eq(self, a: Image.Image, b: Image.Image, channels):
        a_channels = a.split()
        b_channels = b.split()

        for ch in channels:
            if a_channels[ch] != b_channels[ch]:
                return False
        return True

    def test_large_metadata(self, test_png):
        """Test incorporamento di metadati più grandi"""
        metadata = {
            "title": "A" * 100,
            "description": "B" * 200,
            "tags": ["tag1", "tag2", "tag3"] * 10,
        }

        result = proc_meta(test_png, {"embed": True, "data": metadata})

        assert isinstance(result, dict)
        assert result["success"] is True

        # Verifica estrazione
        extracted = proc_meta(result["path"], {"extract": True})
        assert extracted == metadata

    def test_nested_metadata(self, test_png):
        """Test con metadati JSON annidati"""
        metadata = {
            "info": {
                "author": "John",
                "contact": {"email": "john@example.com", "phone": "123-456-7890"},
            },
            "stats": [1, 2, 3, 4, 5],
        }

        result = proc_meta(test_png, {"embed": True, "data": metadata})
        assert isinstance(result, dict)
        assert result["success"] is True

        extracted = proc_meta(result["path"], {"extract": True})
        assert extracted == metadata


class TestBatchProc:
    """Test per batch_proc"""

    def test_batch_embed_multiple_images(self, multiple_test_pngs, tmp_path):
        """Test incorporamento batch di più immagini"""
        output_dir = tmp_path / "output"

        metadata = {"batch": "test", "number": 42}
        ops = {"embed": True, "data": metadata}

        results = batch_proc(multiple_test_pngs, ops, output_dir)

        # Verifica che tutte le immagini siano state processate
        assert len(results) == 3
        assert all(v is True for v in results.values()), (
            f"Operazioni fallite: {[k for k, v in results.items() if not v]}"
        )

        # Verifica che i file di output esistano
        output_files = os.listdir(output_dir)
        assert len(output_files) == 3, (
            f"File di output: attesi 3, trovati {len(output_files)}"
        )

        # Verifica che i metadati siano stati effettivamente incorporati
        for output_file in output_files:
            output_path = os.path.join(output_dir, output_file)
            extracted = proc_meta(output_path, {"extract": True})
            assert extracted == metadata

    def test_batch_with_custom_params(self, multiple_test_pngs, tmp_path):
        """Test batch con parametri personalizzati"""
        output_dir = tmp_path / "output_custom"

        metadata = {"custom": True}
        ops = {"embed": True, "data": metadata}

        results = batch_proc(
            multiple_test_pngs,
            ops,
            output_dir,
            channel_idx=1,
        )

        # Verifica che tutte le immagini siano state processate
        assert len(results) == 3
        assert all(v is True for v in results.values()), (
            f"Operazioni fallite: {[k for k, v in results.items() if not v]}"
        )

        # Verifica che i file di output esistano
        output_files = os.listdir(output_dir)
        assert len(output_files) == 3, (
            f"File di output: attesi 3, trovati {len(output_files)}"
        )

        # Verifica che i metadati siano stati effettivamente incorporati
        for output_file in os.listdir(output_dir):
            output_path = os.path.join(output_dir, output_file)
            extracted = proc_meta(
                output_path,
                {"extract": True, "ch": 1},
                hdr_sz=32,
                bp=1,
            )
            assert extracted == metadata

    def test_batch_with_errors(self, multiple_test_pngs, tmp_path):
        """Test batch handling quando alcuni file falliscono"""
        output_dir = tmp_path / "output_errors"

        # Aggiungi un path non valido alla lista
        paths_with_error = multiple_test_pngs + ["nonexistent.png"]

        metadata = {"test": "value"}
        ops = {"embed": True, "data": metadata}

        results = batch_proc(paths_with_error, ops, output_dir)

        # Verifica che le immagini valide siano processate
        assert len(results) == 4
        assert sum(results.values()) == 3  # Solo 3 su 4 hanno successo
        assert results["nonexistent.png"] is False


# ==================== TEST CHE DIMOSTRANO PROBLEMI ====================


class TestInterfaceProblems:
    """Questi test mostrano i problemi dell'interfaccia attuale"""

    def test_problem_1_inconsistent_return_types(self, test_png, tmp_path):
        """
        PROBLEMA 1: proc_meta restituisce tipi diversi a seconda dell'operazione

        Quando chiami proc_meta non sai mai cosa aspettarti:
        - dict per operazioni normali
        - bool per embed con output
        - None per extract fallito
        - str per embed con verify

        Questo rende impossibile scrivere codice robusto senza controllare
        ogni volta il tipo del risultato!
        """
        # Caso 1: restituisce dict
        result1 = proc_meta(test_png, {"embed": True, "data": {"test": 1}})
        assert isinstance(result1, dict)
        print(f"Tipo 1: {type(result1)}")

        # Caso 2: restituisce bool (per embed con output)
        output = tmp_path / "out.png"
        result2 = proc_meta(
            test_png, {"embed": True, "data": {"test": 1}, "out": output}
        )
        assert isinstance(result2, bool)
        print(f"Tipo 2: {type(result2)}")

        # Caso 3: restituisce None (per extract fallito)
        result3 = proc_meta(test_png, {"extract": True}, raise_err=False)
        assert result3 is None
        print(f"Tipo 3: {type(result3)}")

        # Caso 4: restituisce Path (per embed con verify)
        output2 = tmp_path / "verified.png"
        result4 = proc_meta(
            test_png,
            {"embed": True, "data": {"test": 1}, "out": output2, "verify": True},
        )
        assert isinstance(result4, os.PathLike)
        print(f"Tipo 4: {type(result4)}")

        # Caso 5: restituisce bool (per verify senza embed!)
        result5 = proc_meta(test_png, {"verify": True})
        assert isinstance(result5, bool)
        print(f"Tipo 5: {type(result5)}")

    def test_problem_2_cryptic_ops_dictionary(self, test_png):
        """
        PROBLEMA 2: il dizionario ops ha chiavi abbreviate e poco chiare

        Chi usa la libreria deve ricordarsi:
        - 'embed' vs 'embedding' vs 'write'
        - 'extract' vs 'extraction' vs 'read'
        - 'out' vs 'output' o 'output_path'
        - 'data' vs 'metadata' vs 'meta'
        - 'ch' vs 'channel'

        Ogni volta devi consultare la documentazione!
        """
        result = proc_meta(
            test_png,
            {
                "embed": True,  # write? save? store?
                "data": {"key": "value"},  # metadata? meta? content?
                "out": test_png,  # output? output_path? destination?
                "overwrite": True,  # replace? force?
                "ch": 0,  # channel? color? component?
            },
        )
        assert result is True

    def test_problem_3_exposed_implementation_details(self, test_png):
        """
        PROBLEMA 3: dettagli implementativi esposti all'utente

        L'utente deve conoscere:
        - Il numero magico (0x4D455441)
        - La dimensione dell'header in bit (32)
        - Il bit plane da usare (ossia quale bit di ogni byte: 1 = LSB)
        - L'encoding (utf-8)

        Questi sono dettagli implementativi che dovrebbero essere nascosti!
        Chi si ricorda che 0x4D455441 è "META" in ASCII?
        """
        result = proc_meta(
            test_png,
            {"embed": True, "data": {"test": 1}},
            hdr_sz=32,  # Che cos'è 32? Chi lo sa senza guardare la documentazione?
            bp=1,  # Bit plane? LSB? MSB? Cosa significa?
            enc="utf-8",  # Perché devo specificarlo?
            magic=0x4D455441,  # Numero magico! Ma chi lo conosce?
        )
        assert isinstance(result, dict)

    def test_problem_4_inconsistent_parameter_naming(self, test_png, tmp_path):
        """
        PROBLEMA 4: nomi dei parametri inconsistenti tra funzioni

        - proc_meta usa 'hdr_sz', batch_proc usa 'header_sz'
        - proc_meta usa 'bp', batch_proc usa 'bit_plane'
        - proc_meta usa 'ch' (nell'ops dict!), batch_proc usa 'channel_idx'

        Stesso concetto, nomi diversi. Che confusione!
        """
        # proc_meta usa 'hdr_sz' e 'bp'
        result1 = proc_meta(
            test_png, {"embed": True, "data": {"test": 1}}, hdr_sz=32, bp=1
        )
        assert isinstance(result1, dict)
        assert result1["success"] is True

        # batch_proc usa 'header_sz' e 'bit_plane'
        out_dir = tmp_path / "out"
        result2 = batch_proc(
            [test_png],
            {"embed": True, "data": {"test": 1}},
            out_dir,
            header_sz=32,
            bit_plane=1,
        )
        assert all(v is True for v in result2.values())

        # Stesso parametro, nomi diversi!

    def test_problem_5_error_handling_chaos(self, tmp_path):
        """
        PROBLEMA 5: gestione errori completamente inconsistente

        Quando c'è un errore:
        - proc_meta in modalità embed: restituisce {"success": False, "error": "..."}
        - proc_meta in modalità extract: restituisce None
        - proc_meta in modalità verify: restituisce False
        - proc_meta con raise_err=True: lancia un'eccezione

        Ogni operazione gestisce gli errori in modo diverso!
        """
        # proc_meta modalità embed
        result1 = proc_meta(tmp_path / "nonexistent.png", {"embed": True, "data": {}})
        print(f"Errore proc_meta embed: {result1}")
        assert isinstance(result1, dict) and "error" in result1

        # proc_meta modalità extract
        result2 = proc_meta(tmp_path / "nonexistent.png", {"extract": True})
        print(f"Errore proc_meta extract: {result2}")
        assert isinstance(result2, dict) and "error" in result2

        # proc_meta modalità verify
        result3 = proc_meta(tmp_path / "nonexistent.png", {"verify": True})
        print(f"Errore proc_meta verify: {result3}")
        assert result3 is False

        # proc_meta con raise_err
        with pytest.raises(Exception) as error4:
            proc_meta(tmp_path / "nonexistent.png", {"verify": True}, raise_err=True)
        print(f"Errore proc_meta raise_err: {error4}")

    def test_problem_6_magic_numbers_everywhere(self, test_png):
        """
        PROBLEMA 6: numeri magici ovunque

        - bp=1: che significa? (è il bit meno significativo)
        - hdr_sz=32: perché 32?
        - magic=0x4D455441: perché questo numero?
        - ch=0: cosa significa 0? R? G? B?

        Senza guardare la documentazione, questi numeri non hanno senso!
        """
        proc_meta(
            test_png,
            {"embed": True, "data": {"test": 1}, "ch": 0},
            hdr_sz=32,  # ???
            bp=1,  # ???
            magic=0x4D455441,  # ???
        )

    def test_problem_7_unclear_function_responsibility(self, test_png, tmp_path):
        """
        PROBLEMA 7: proc_meta fa TUTTO

        Una singola funzione che:
        - Incorpora metadati
        - Estrae metadati
        - Verifica presenza metadati
        - Aggiorna metadati esistenti
        - Salva con path custom

        Viola il principio della singola responsabilità!
        È impossibile capire cosa fa senza leggere tutto il codice.
        """
        # Può fare letteralmente tutto questo tramite dizionario ops:
        proc_meta(
            test_png,
            {
                "embed": True,  # incorpora
                "data": {"key": "value"},  # questi dati
                "out": tmp_path / "everything.png",  # salva qui
                "verify": True,  # e verifica
                "ch": 0,  # sul canale rosso
            },
        )

        # Che cosa ha fatto esattamente? Chi lo sa!

    def test_problem_8_channel_parameter_confusion(self, test_png):
        """
        PROBLEMA 8: il parametro channel non è intuitivo

        - ch=0: canale rosso
        - ch=1: canale verde
        - ch=2: canale blu
        - ch=-1: tutti i canali

        Chi si ricorda che -1 significa "tutti"? E perché non 3?
        """
        # Utente confuso: "Voglio usare tutti i canali... ch=3?"
        result = proc_meta(
            test_png,
            {"embed": True, "data": {"test": 1}, "ch": 3},
        )
        assert result == {
            "success": False,
            "error": "list index out of range",
        }  # No, è -1!


# ==================== TEST PER NUOVE FUNZIONI (da completare) ====================


class TestNewInterfaceIdeas:
    """
    Questi test rappresentano come DOVREBBE essere la nuova interfaccia.
    Al momento falliranno perché le funzioni non esistono ancora.

    È compito dei partecipanti implementarle!
    """

    def _import(self, function_name: str) -> Callable:
        import steganography

        func = vars(steganography).get(function_name)
        if func is None:
            pytest.skip(f"{function_name} non ancora implementata")
        return func

    def test_embed_metadata_simple(self, test_png, tmp_path):
        """
        Funzione proposta: embed_metadata(image_path, metadata, output_path=None) -> MetadataResult

        Dovrebbe incorporare metadati in modo semplice e diretto,
        nascondendo completamente i dettagli implementativi.

        Esempio d'uso:
            result = embed_metadata("photo.png", {"author": "John"})
            # Fatto! Niente magic numbers, niente complessità
        """
        embed_metadata = self._import("embed_metadata")

        output = tmp_path / "embedded.png"
        result = embed_metadata(test_png, {"author": "John Doe"}, output_path=output)

        assert result is not None
        # Il risultato dovrebbe essere sempre dello stesso tipo (oggetto o dict)
        assert hasattr(result, "success") or "success" in result
        if hasattr(result, "success"):
            assert result.success is True
            assert os.path.exists(result.output_path)
        else:
            assert result["success"] is True
            assert os.path.exists(result["output_path"])

    def test_extract_metadata_simple(self, test_png_with_metadata):
        """
        Funzione proposta: extract_metadata(image_path) -> dict

        Interfaccia semplice e chiara per estrarre metadati.
        Restituisce sempre un dict (vuoto se non ci sono metadati).

        Esempio d'uso:
            metadata = extract_metadata("photo.png")
            print(metadata.get("author"))
        """
        extract_metadata = self._import("extract_metadata")

        result = extract_metadata(test_png_with_metadata)

        assert isinstance(result, dict)
        assert "author" in result
        assert result["author"] == "Test Author"

    def test_has_metadata_simple(self, test_png, test_png_with_metadata):
        """
        Funzione proposta: has_metadata(image_path) -> bool

        Verifica semplice se l'immagine contiene metadati nascosti.

        Esempio d'uso:
            if has_metadata("photo.png"):
                print("Questa immagine ha metadati!")
        """
        has_metadata = self._import("has_metadata")

        # Immagine senza metadati
        assert has_metadata(test_png) is False

        # Immagine con metadati
        assert has_metadata(test_png_with_metadata) is True

    def test_update_metadata_simple(self, test_png_with_metadata, tmp_path):
        """
        Funzione proposta: update_metadata(image_path, new_data, output_path=None) -> MetadataResult

        Aggiorna metadati esistenti o ne crea di nuovi.

        Esempio d'uso:
            result = update_metadata("photo.png", {"year": 2025})
            # I metadati esistenti vengono preservati, solo "year" viene aggiunto/aggiornato
        """
        update_metadata = self._import("update_metadata")

        output = tmp_path / "updated.png"
        result = update_metadata(
            test_png_with_metadata,
            {"author": "New Author"},
            output_path=output,
        )

        assert hasattr(result, "success") or "success" in result
        if hasattr(result, "success"):
            assert result.success is True
        else:
            assert result["success"] is True

        # Verifica che i vecchi metadati siano preservati
        extract_metadata = self._import("extract_metadata")

        updated_meta = extract_metadata(output)
        assert updated_meta["author"] == "New Author"
        assert updated_meta["title"] == "Test Image"  # Preservato

    def test_clear_metadata_simple(self, test_png_with_metadata, tmp_path):
        """
        Funzione proposta: clear_metadata(image_path, output_path=None) -> MetadataResult

        Rimuove i metadati nascosti da un'immagine.

        Esempio d'uso:
            result = clear_metadata("photo.png")
            # Metadati rimossi, tutti i LSB impostati a 0
        """
        clear_metadata = self._import("clear_metadata")

        output = tmp_path / "clean.png"
        result = clear_metadata(test_png_with_metadata, output_path=output)

        assert hasattr(result, "success") or "success" in result
        if hasattr(result, "success"):
            assert result.success is True
        else:
            assert result["success"] is True

        # Verifica che non ci siano più metadati
        has_metadata = self._import("has_metadata")

        assert has_metadata(output) is False

    def test_copy_metadata_between_images(self, test_png_with_metadata, test_png):
        """
        Funzione proposta: copy_metadata(source_path, destination_path, output_path=None) -> MetadataResult

        Copia metadati da un'immagine all'altra.

        Esempio d'uso:
            result = copy_metadata("source.png", "destination.png")
            # Metadati copiati!
        """
        copy_metadata = self._import("copy_metadata")

        result = copy_metadata(test_png_with_metadata, test_png)

        assert hasattr(result, "success") or "success" in result
        if hasattr(result, "success"):
            assert result.success is True
        else:
            assert result["success"] is True

        # Verifica che i metadati siano stati copiati
        extract_metadata = self._import("extract_metadata")

        assert extract_metadata(test_png) == extract_metadata(test_png_with_metadata)


"""
🎯 HAI FINITO LA TUA ANALISI?

Ottimo lavoro! Ora è il momento di dimostrare che la tua nuova interfaccia
è davvero migliore della vecchia.

COSA HAI VISTO NEI TEST SOPRA?

I test in TestInterfaceProblems mostrano 8 problemi gravi:
1. ❌ Tipi di ritorno inconsistenti (dict/bool/None/PathLike)
2. ❌ Chiavi del dizionario ops criptiche ('embed', 'data', 'out', 'ch')
3. ❌ Dettagli implementativi esposti (magic numbers, header size, bit planes)
4. ❌ Nomi parametri inconsistenti (hdr_sz vs header_sz, bp vs bit_plane, ch vs channel_idx)
5. ❌ Gestione errori caotica (ogni funzione fa come vuole)
6. ❌ Numeri magici ovunque (32, 1, 0x4D455441, -1)
7. ❌ Una funzione che fa troppo (proc_meta)
8. ❌ Parametro channel confuso (0, 1, 2, -1)

La tua nuova interfaccia dovrebbe risolvere TUTTI questi problemi!

PROSSIMI PASSI:

1. Implementa le funzioni suggerite in TestNewInterfaceIdeas (o inventa le tue!)
2. Aggiungi i tuoi test qui sotto nella classe TestMyNewInterface
3. I tuoi test dovrebbero dimostrare che:
   ✅ Ogni funzione ha un nome chiaro e auto-esplicativo
   ✅ I parametri sono intuitivi (no abbreviazioni, no dizionari complessi)
   ✅ Tutte le funzioni restituiscono lo stesso tipo di risultato
   ✅ La gestione errori è consistente ovunque
   ✅ Non ci sono dettagli implementativi esposti (magic numbers, bit planes)
   ✅ Ogni funzione ha una singola responsabilità chiara

4. Esempi di test che potresti scrivere:

   def test_consistent_return_types(self, test_png, test_png_with_metadata):
       '''Dimostra che tutte le tue funzioni restituiscono lo stesso tipo'''
       result1 = embed_metadata(test_png, {"test": 1})
       result2 = extract_metadata(test_png_with_metadata)
       result3 = has_metadata(test_png)

       # Tutti restituiscono schemi coerenti e prevedibili
       assert isinstance(result1, dict) or hasattr(result1, "success")
       assert isinstance(result2, dict)
       assert isinstance(result3, bool)

   def test_consistent_error_handling(self):
       '''Dimostra che gli errori sono gestiti in modo uniforme'''
       result1 = embed_metadata("nonexistent.png", {})
       result2 = extract_metadata("nonexistent.png")
       result3 = has_metadata("nonexistent.png")
       
       # Tutti gestiscono gli errori nello stesso modo
       # (es. tutti restituiscono success=False, o tutti sollevano eccezioni)

BONUS: Considera di aggiungere funzioni di convenienza per casi d'uso comuni:
- add_copyright(image_path, copyright_text): aggiunge copyright come metadato
- add_author_info(image_path, name, email): aggiunge info autore
- add_creation_date(image_path, date=None): aggiunge data (default: oggi)
- get_image_info(image_path): restituisce sia metadati che info base sull'immagine
- batch_embed_metadata(image_paths, metadata, output_dir): incorpora gli stessi metadati in più immagini

Buon lavoro! 🚀
"""
