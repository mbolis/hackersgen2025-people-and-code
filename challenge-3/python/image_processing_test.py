import os

import pytest
from image_processing import (
    batch_op,
    get_exif,
    img_op,
)
from PIL import Image

# ======================== Fixture ========================
# ---------- Creano immagini temporanee di test -----------


@pytest.fixture
def test_image(tmp_path):
    """Crea un'immagine RGB di test 800x600"""
    img_path = tmp_path / "test_image.jpg"
    img = Image.new("RGB", (800, 600), color="red")
    img.save(img_path)
    return str(img_path)


@pytest.fixture
def test_image_with_exif(tmp_path):
    """Crea un'immagine con metadati EXIF simulati"""
    img_path = tmp_path / "test_exif.jpg"
    img = Image.new("RGB", (800, 600), color="blue")

    # Aggiungi EXIF orientation (valore 6 = ruotata 270°)
    exif = img.getexif()
    exif[306] = "2025:10:15 20:12:30"  # Tag 306 = DateTime
    exif[274] = 6  # Tag 274 = Orientation

    img.save(img_path, exif=exif)
    return str(img_path)


@pytest.fixture
def multiple_test_images(tmp_path):
    """Crea 3 immagini di test per batch processing"""
    images = []
    for i in range(3):
        img_path = tmp_path / f"test_{i}.jpg"
        img = Image.new("RGB", (800, 600), color="red")
        img.save(img_path)
        images.append(str(img_path))
    return images


class TestImgOp:
    """Test per la funzione img_op"""

    def test_basic_processing(self, test_image):
        """Test elaborazione base senza modifiche"""
        result = img_op(test_image, {"orient": False})

        assert isinstance(result, dict)
        assert result["success"] is True
        assert "path" in result
        assert os.path.exists(result["path"])

    def test_resize_operation(self, test_image):
        """Test operazione resize"""
        result = img_op(test_image, {"orient": False, "size": (400, 300)})

        assert isinstance(result, dict)
        assert result["success"] is True

        # Verifica dimensioni
        with Image.open(result["path"]) as img:
            assert img.size[0] <= 400
            assert img.size[1] <= 300

    def test_thumbnail_operation(self, test_image, tmp_path):
        """Test creazione thumbnail"""
        thumb_path = tmp_path / "thumb.jpg"
        result = img_op(test_image, {"thumb": (200, 200), "out": str(thumb_path)})

        assert result is True
        assert os.path.exists(thumb_path)

        with Image.open(thumb_path) as img:
            assert img.size[0] <= 200
            assert img.size[1] <= 200

    def test_thumbnail_without_aspect_ratio(self, test_image, tmp_path):
        """Test thumbnail senza mantenere aspect ratio"""
        thumb_path = tmp_path / "thumb_stretched.jpg"
        result = img_op(
            test_image,
            {"thumb": (200, 200), "maintain_aspect": False, "out": str(thumb_path)},
        )

        assert result is True

        with Image.open(thumb_path) as img:
            assert img.size == (200, 200)

    def test_rotation_operation(self, test_image):
        """Test rotazione immagine"""
        result = img_op(test_image, {"rotate": 90})

        assert result is not None
        assert isinstance(result, dict)

        # L'immagine era 800x600, ruotata dovrebbe essere 600x800
        with Image.open(result["path"]) as img:
            assert img.size == (600, 800)

    def test_rotation_with_output_path(self, test_image, tmp_path):
        """Test rotazione con path output specificato"""
        output = tmp_path / "rotated.jpg"
        result = img_op(test_image, {"rotate": 180, "out": str(output)})

        assert isinstance(result, str)
        assert result == str(output)

        # L'immagine era 800x600, ruotata dovrebbe essere 600x800
        with Image.open(result) as img:
            assert img.size == (600, 800)

    def test_format_conversion(self, test_image):
        """Test cambio formato"""
        result = img_op(test_image, {"fmt": "png"})

        assert isinstance(result, dict)
        assert result["success"] is True
        assert result["path"].endswith(".png")
        assert os.path.exists(result["path"])

    def test_nonexistent_file(self):
        """Test con file inesistente"""
        result = img_op("/path/non/esistente.jpg", {"orient": True}, raise_err=False)

        assert isinstance(result, dict)
        assert result["success"] is False
        assert "error" in result

    def test_exif_orientation(self, test_image_with_exif):
        """Test applicazione orientamento EXIF"""
        result = img_op(test_image_with_exif, {"orient": True})

        assert isinstance(result, dict)
        assert result["success"] is True
        # L'immagine dovrebbe essere stata ruotata
        with Image.open(result["path"]) as rotated:
            assert rotated.size == (600, 800)


class TestGetExif:
    """Test per get_exif"""

    def test_get_exif_basic(self, test_image):
        """Test estrazione metadati base"""
        metadata = get_exif(test_image)

        # Un'immagine semplice può non avere EXIF
        assert isinstance(metadata, dict)
        assert "error" not in metadata

    def test_get_exif_with_data(self, test_image_with_exif):
        """Test estrazione metadati EXIF"""
        metadata = get_exif(test_image_with_exif)

        # Dovrebbe contenere almeno il tag Orientation
        assert isinstance(metadata, dict)
        assert "Orientation" in metadata

    def test_get_exif_with_numeric_tags(self, test_image_with_exif):
        """Test estrazione con tag numerici"""
        metadata = get_exif(test_image_with_exif, use_names=False)

        # Dovrebbe avere chiavi numeriche
        assert isinstance(metadata, dict)
        assert 274 in metadata

    def test_get_exif_specific_tags(self, test_image_with_exif):
        """Test estrazione tag specifici"""
        metadata = get_exif(test_image_with_exif, tag_nums=[274])

        assert isinstance(metadata, dict)
        assert len(metadata) == 1
        assert "Orientation" in metadata

    def test_get_exif_nonexistent_file(self):
        """Test con file inesistente"""
        metadata = get_exif("/nonexistent.jpg")

        assert metadata == {
            "error": "[Errno 2] No such file or directory: '/nonexistent.jpg'"
        }


class TestBatchOp:
    """Test per batch_op"""

    def test_batch_process_multiple_images(self, multiple_test_images, tmp_path):
        """Test processamento batch di più immagini"""
        output_dir = tmp_path / "output"

        ops = {"size": (400, 300), "qual": 80}

        results = batch_op(multiple_test_images, ops, str(output_dir))

        # Verifica che tutte le immagini siano state processate
        assert len(results) == 3
        assert all(results.values())  # Tutti True

        # Verifica che i file di output esistano
        assert os.path.exists(output_dir)
        assert len(os.listdir(output_dir)) == 3

    def test_batch_with_format_conversion(self, multiple_test_images, tmp_path):
        """Test batch con conversione formato"""
        output_dir = tmp_path / "output_png"

        ops = {"fmt": "png", "qual": 90}

        results = batch_op(multiple_test_images, ops, str(output_dir))

        assert all(results.values())

        # Verifica che i file siano PNG
        for file in os.listdir(output_dir):
            assert file.endswith(".png")

    def test_batch_with_custom_exif_params(self, multiple_test_images, tmp_path):
        """Test batch con parametri EXIF personalizzati"""
        output_dir = tmp_path / "output_custom"

        ops = {"orient": True}

        results = batch_op(
            multiple_test_images,
            ops,
            str(output_dir),
            exif_tag=274,
            rotation_vals={3: 180, 6: 270, 8: 90},
        )

        assert len(results) == 3
        assert all(results.values()), (
            f"Operazioni fallite: {[k for k, v in results.items() if not v]}"
        )
        # Verifica esistenza dei file di output
        output_files = os.listdir(output_dir)
        assert len(output_files) == 3, (
            f"File di ouput: attesi 3, trovati {len(output_files)}"
        )


# ==================== TEST CHE DIMOSTRANO PROBLEMI ====================


class TestInterfaceProblems:
    """Questi test mostrano i problemi dell'interfaccia attuale"""

    def test_problem_1_inconsistent_return_types(self, test_image, tmp_path):
        """
        PROBLEMA 1: img_op restituisce tipi diversi a seconda dell'operazione

        Quando chiami img_op non sai mai cosa aspettarti:
        - dict per operazioni normali
        - bool per thumbnail
        - str per rotazione con output
        - None per errori in alcuni casi

        Questo rende impossibile scrivere codice robusto senza controllare
        ogni volta il tipo del risultato!
        """
        # Caso 1: restituisce dict
        result1 = img_op(test_image, {"orient": True})
        assert isinstance(result1, dict)
        print(f"Tipo 1: {type(result1)}")

        # Caso 2: restituisce bool (per thumbnail)
        thumb_path = tmp_path / "thumb.jpg"
        result2 = img_op(test_image, {"thumb": (200, 200), "out": str(thumb_path)})
        assert isinstance(result2, bool)
        print(f"Tipo 2: {type(result2)}")

        # Caso 3: restituisce str (per rotazione con output)
        output = tmp_path / "rotated.jpg"
        result3 = img_op(test_image, {"rotate": 90, "out": str(output)})
        assert isinstance(result3, str)
        print(f"Tipo 3: {type(result3)}")

        # Caso 4: con errore in modalità thumbnail restituisce False
        result4 = img_op("/nonexistent.jpg", {"thumb": (100, 100)}, raise_err=False)
        assert result4 == {"success": False, "error": "File not found"}
        print(f"Tipo 4 (errore thumbnail): {type(result4)}")

    def test_problem_2_cryptic_ops_dictionary(self, test_image):
        """
        PROBLEMA 2: il dizionario ops ha chiavi abbreviate e poco chiare

        Chi usa la libreria deve ricordarsi:
        - 'orient' vs 'orientation'
        - 'fmt' vs 'format'
        - 'qual' vs 'quality'
        - 'thumb' vs 'thumbnail'
        - 'out' vs 'output' o 'output_path'

        Ogni volta devi consultare la documentazione!
        """
        result = img_op(
            test_image,
            {
                "orient": True,  # orientation? oriented? apply_orientation?
                "fmt": "png",  # format? file_format?
                "qual": 90,  # quality? qual_factor?
                "out": test_image,  # output? output_path? destination?
            },
        )
        assert isinstance(result, dict)
        assert result["success"] is True

    def test_problem_3_exposed_exif_details(self, test_image_with_exif):
        """
        PROBLEMA 3: dettagli EXIF ancora esposti all'utente

        L'utente deve conoscere:
        - Il numero del tag EXIF (274 = Orientation)
        - Il mapping dei valori EXIF ai gradi {3: 180, 6: 270, 8: 90}

        Questi sono dettagli implementativi che dovrebbero essere nascosti!
        Chi si ricorda che il valore EXIF 6 significa "ruota 270 gradi"?
        """
        result = img_op(
            test_image_with_exif,
            {"orient": True},
            exif_orient=274,  # Che cos'è 274? Chi lo sa senza guardare la documentazione?
            rot_map={3: 180, 6: 270, 8: 90},  # Valori magici!
        )
        assert isinstance(result, dict)

    def test_problem_4_inconsistent_parameter_naming(self, test_image, tmp_path):
        """
        PROBLEMA 4: nomi dei parametri inconsistenti tra funzioni

        - img_op usa 'rsample'
        - batch_op usa 'resample_method'

        Stesso concetto, nomi diversi. Confusionario!
        """
        # img_op usa 'rsample'
        result1 = img_op(test_image, {"rotate": 45}, rsample=3)
        assert result1 is not None, "OK img_op con parametro rsample"

        # batch_op usa 'resample_method'
        out_dir = str(tmp_path / "out")
        result2 = batch_op([test_image], {"rotate": 45}, out_dir, resample_method=3)
        assert all(result2.values()), "OK batch_op con parametro resample_method"

        # Stesso parametro, nomi diversi!

    def test_problem_5_error_handling_chaos(self):
        """
        PROBLEMA 5: gestione errori completamente inconsistente

        Quando c'è un errore:
        - img_op in modalità dict: restituisce {"success": False, "error": "..."}
        - img_op in modalità thumbnail: restituisce False
        - img_op in modalità rotate: restituisce None
        - get_exif: restituisce {"error": "..."}

        Ogni funzione gestisce gli errori in modo diverso!
        """
        # img_op modalità dict
        result1 = img_op("/nonexistent.jpg", {"orient": True}, raise_err=False)
        print(f"Errore img_op dict: {result1}")
        assert isinstance(result1, dict) and "error" in result1

        # img_op modalità thumbnail
        result2 = img_op(
            "/nonexistent.jpg",
            {"thumb": (100, 100), "out": "out.jpg"},
            raise_err=False,
        )
        print(f"Errore img_op thumbnail: {result2}")
        assert result2 == {"success": False, "error": "File not found"}

        # img_op modalità rotate
        result3 = img_op("/nonexistent.jpg", {"rotate": 90}, raise_err=False)
        print(f"Errore img_op rotate: {result3}")
        assert result3 is None or (isinstance(result3, dict) and not result3["success"])

        # get_exif
        result4 = get_exif("/nonexistent.jpg")
        print(f"Errore get_exif: {result4}")
        assert "error" in result4

    def test_problem_6_magic_numbers_everywhere(self, test_image):
        """
        PROBLEMA 6: numeri magici ovunque

        - rsample=3: che significa? (è PIL.Image.BICUBIC)
        - exif_orient=274: perché 274?
        - rot_map valori: perché 3, 6, 8?

        Senza guardare la documentazione PIL, questi numeri non hanno senso!
        """
        img_op(
            test_image,
            {"rotate": 45},
            exif_orient=274,  # ???
            rot_map={3: 180, 6: 270, 8: 90},  # ???
            rsample=3,  # ???
        )

    def test_problem_7_unclear_function_responsibility(self, test_image, tmp_path):
        """
        PROBLEMA 7: img_op fa TUTTO

        Una singola funzione che:
        - Applica orientamento EXIF
        - Ruota manualmente
        - Fa resize
        - Crea thumbnail
        - Converte formato
        - Salva con qualità specifica

        Viola il principio della singola responsabilità!
        È impossibile capire cosa fa senza leggere tutto il codice.
        """
        # Può fare letteralmente tutto questo in una chiamata:
        img_op(
            test_image,
            {
                "orient": True,  # applica EXIF
                "rotate": 45,  # ruota ancora
                "size": (400, 300),  # resize
                "fmt": "png",  # converti formato
                "qual": 90,  # imposta qualità
                "out": str(tmp_path / "everything.png"),  # salva qui
            },
        )

        # Che cosa ha fatto esattamente? Chi lo sa!


# ==================== TEST PER NUOVE FUNZIONI (da completare) ====================


class TestNewInterfaceIdeas:
    """
    Questi test rappresentano come DOVREBBE essere la nuova interfaccia.
    Al momento falliranno perché le funzioni non esistono ancora.

    È compito dei partecipanti implementarle!
    """

    def test_auto_orient_image(self, test_image_with_exif):
        """
        Funzione proposta: auto_orient_image(image_path, output_path=None) -> str

        Dovrebbe automaticamente ruotare l'immagine in base ai dati EXIF,
        nascondendo completamente i dettagli dei tag EXIF all'utente.

        Esempio d'uso:
            oriented_path = auto_orient_image("photo.jpg")
            # Fatto! Niente tag EXIF, niente rot_map, niente complessità
        """
        try:
            from image_processing import auto_orient_image

            result = auto_orient_image(test_image_with_exif)

            assert result is not None
            assert isinstance(result, str)
            assert os.path.exists(result)
        except ImportError:
            pytest.skip("auto_orient_image non ancora implementata")

    def test_create_thumbnail_simple(self, test_image, tmp_path):
        """
        Funzione proposta: create_thumbnail(image_path, max_size, output_path) -> ImageResult

        Interfaccia semplice e chiara per creare thumbnail.
        Restituisce sempre lo stesso tipo di oggetto, con informazioni strutturate.

        Esempio d'uso:
            result = create_thumbnail("photo.jpg", max_size=200, output_path="thumb.jpg")
            if result.success:
                print(f"Thumbnail creata: {result.output_path}")
        """
        try:
            from image_processing import create_thumbnail

            output = tmp_path / "thumb.jpg"
            result = create_thumbnail(test_image, max_size=200, output_path=str(output))

            # Il risultato dovrebbe essere sempre dello stesso tipo
            assert hasattr(result, "success") or "success" in result
            assert hasattr(result, "output_path") or "output_path" in result

            if hasattr(result, "success"):
                assert result.success is True
                assert os.path.exists(result.output_path)
            else:
                assert result["success"] is True
                assert os.path.exists(result["output_path"])

        except ImportError:
            pytest.skip("create_thumbnail non ancora implementata")

    def test_resize_image_simple(self, test_image, tmp_path):
        """
        Funzione proposta: resize_image(image_path, max_width=None, max_height=None, output_path=None) -> ImageResult

        Resize semplice e intuitivo, senza dover creare dizionari complicati.

        Esempio d'uso:
            result = resize_image("photo.jpg", max_width=800)
            # Semplice e chiaro!
        """
        try:
            from image_processing import resize_image

            result = resize_image(test_image, max_width=400)

            assert hasattr(result, "success") or "success" in result
            if hasattr(result, "success"):
                assert result.success is True
            else:
                assert result["success"] is True

        except ImportError:
            pytest.skip("resize_image non ancora implementata")

    def test_prepare_for_web(self, test_image, tmp_path):
        """
        Funzione proposta: prepare_for_web(image_path, max_width=1920, quality=85, output_path=None) -> ImageResult

        Caso d'uso comune: preparare immagine per il web.
        Auto-orienta, ridimensiona, ottimizza qualità, tutto in una chiamata chiara.

        Esempio d'uso:
            result = prepare_for_web("photo.jpg", max_width=1200, quality=80)
            # Perfetto per il web, nessuna complessità!
        """
        try:
            from image_processing import prepare_for_web

            output = tmp_path / "web_ready.jpg"
            result = prepare_for_web(
                test_image, max_width=800, quality=85, output_path=str(output)
            )

            assert hasattr(result, "success") or "success" in result
            if hasattr(result, "success"):
                assert result.success is True
                assert os.path.exists(result.output_path)
            else:
                assert result["success"] is True
                assert os.path.exists(result["output_path"])

        except ImportError:
            pytest.skip("prepare_for_web non ancora implementata")


"""
🎯 HAI FINITO DI RI-INGEGNERIZZARE?

Ottimo lavoro! Ora è il momento di dimostrare che la tua nuova interfaccia
è davvero migliore della vecchia.

COSA HAI VISTO NEI TEST SOPRA?

I test in TestInterfaceProblems mostrano 7 problemi gravi:
1. ❌ Tipi di ritorno inconsistenti (dict/bool/str/None)
2. ❌ Chiavi del dizionario ops criptiche ('fmt', 'qual', 'thumb')
3. ❌ Dettagli EXIF esposti (tag 274, rot_map)
4. ❌ Nomi parametri inconsistenti (rsample vs resample_method)
5. ❌ Gestione errori caotica (ogni funzione fa come vuole)
6. ❌ Numeri magici ovunque (3, 274, {3: 180, 6: 270, 8: 90})
7. ❌ Una funzione che fa troppo (img_op)

La tua nuova interfaccia dovrebbe risolvere TUTTI questi problemi!

PROSSIMI PASSI:

1. Implementa le funzioni suggerite in TestNewInterfaceIdeas (o inventa le tue!)
2. Aggiungi i tuoi test qui sotto nella classe TestMyNewInterface
3. I tuoi test dovrebbero dimostrare che:
   ✅ Ogni funzione ha un nome chiaro e auto-esplicativo
   ✅ I parametri sono intuitivi (no abbreviazioni, no dizionari complessi)
   ✅ Tutte le funzioni restituiscono lo stesso tipo di risultato
   ✅ La gestione errori è consistente ovunque
   ✅ Non ci sono dettagli implementativi esposti (EXIF, numeri magici)
   ✅ Ogni funzione ha una singola responsabilità chiara

4. Esempi di test che potresti scrivere:

   def test_clear_function_names(self, test_image):
       '''Dimostra che i nomi delle tue funzioni sono auto-esplicativi'''
       # Invece di img_op con dizionario criptico:
       result = rotate_image(test_image, degrees=90)
       
       # Il nome dice ESATTAMENTE cosa fa!
       assert result.success is True

   def test_consistent_return_types(self, test_image):
       '''Dimostra che tutte le tue funzioni restituiscono lo stesso tipo'''
       result1 = rotate_image(test_image, degrees=90)
       result2 = resize_image(test_image, max_width=800)
       result3 = create_thumbnail(test_image, max_size=200, output_path="thumb.jpg")

       # Tutti restituiscono lo stesso schema coerente
       for result in (result1, result2, result3):
           assert "success" in result
           assert isinstance(result["success"], bool)

           from PIL import Image

           assert "image" in result
           assert isinstance(result["image"], Image.Image)
"""
