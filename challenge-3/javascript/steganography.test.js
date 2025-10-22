/**
 * Test suite per la libreria di steganografia
 * 
 * Questa suite testa l'interfaccia attuale della libreria Steganography
 * e dimostra i problemi presenti nell'API.
 */

import fs from 'fs';
import path from 'path';
import os from 'os';
import { PNG } from 'pngjs';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { procMeta, batchProc } from './steganography';

describe('Steganography Tests', () => {
  let tmpDir;

  beforeEach(() => {
    tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'stego-test-'));
  });

  afterEach(() => {
    if (fs.existsSync(tmpDir)) {
      fs.rmSync(tmpDir, { recursive: true, force: true });
    }
  });

  // ================ Helper Functions ================

  /**
   * Crea un'immagine PNG di test 100x100 rossa
   */
  function createTestPNG() {
    const pngPath = path.join(tmpDir, 'test.png');

    const png = new PNG({
      width: 100,
      height: 100,
      colorType: 2, // color, no alpha
      bgColor: { red: 255, green: 0, blue: 0 },
    });

    const buffer = PNG.sync.write(png);
    fs.writeFileSync(pngPath, buffer);

    return pngPath;
  }

  /**
   * Crea un'immagine PNG con metadati già incorporati
   */
  function createTestPNGWithMetadata() {
    const pngPath = path.join(tmpDir, 'test_with_meta.png');

    const png = new PNG({
      width: 100,
      height: 100,
      colorType: 2, // color, no alpha
      bgColor: { red: 0, green: 0, blue: 255 },
    });

    const buffer = PNG.sync.write(png);
    fs.writeFileSync(pngPath, buffer);

    const metadata = {
      author: 'Test Author',
      title: 'Test Image',
      date: '2025-10-15'
    };

    procMeta(pngPath, {
      embed: true,
      data: metadata,
      overwrite: true
    });

    return pngPath;
  }

  /**
   * Crea 3 immagini PNG di test per batch processing
   */
  function createMultipleTestPNGs() {
    const images = [];
    for (let i = 0; i < 3; i++) {
      const pngPath = path.join(tmpDir, `test_${i}.png`);

      const png = new PNG({
        width: 100,
        height: 100,
        colorType: 2, // color, no alpha
        bgColor: { red: 0, green: 255, blue: 0 },
      });

      const buffer = PNG.sync.write(png);
      fs.writeFileSync(pngPath, buffer);
      images.push(pngPath);
    }
    return images;
  }

  // =========================== Test ===========================

  describe('Test per la funzione procMeta', () => {

    it('Test incorporamento metadati base', () => {
      const testPng = createTestPNG();

      const metadata = {
        author: 'John Doe',
        year: 2025
      };

      const result = procMeta(testPng, {
        embed: true,
        data: metadata
      });

      expect(result).toMatchObject({ success: true });
      expect(fs.existsSync(result.path)).toBe(true);
    });

    it('Test incorporamento con path output specificato', () => {
      const testPng = createTestPNG();
      const originalBytes = fs.readFileSync(testPng);

      const metadata = { title: 'Test Image' };
      const output = path.join(tmpDir, 'embedded.png');

      const result = procMeta(testPng, {
        embed: true,
        data: metadata,
        out: output
      });

      expect(result).toBe(true);
      expect(fs.existsSync(output)).toBe(true);

      const currentBytes = fs.readFileSync(testPng);
      expect(currentBytes).toEqual(originalBytes);

      const savedBytes = fs.readFileSync(output);
      expect(savedBytes).not.toEqual(originalBytes);
    });

    it('Test incorporamento sovrascrivendo il file originale', () => {
      const testPng = createTestPNG();
      const originalBytes = fs.readFileSync(testPng);

      const metadata = { description: 'Overwritten' };

      const result = procMeta(testPng, {
        embed: true,
        data: metadata,
        overwrite: true
      });

      expect(result).toMatchObject({ success: true });

      const currentBytes = fs.readFileSync(testPng);
      expect(currentBytes).not.toEqual(originalBytes);
    });

    it('Test estrazione metadati', () => {
      const testPngWithMetadata = createTestPNGWithMetadata();

      const result = procMeta(testPngWithMetadata, {
        extract: true
      });

      expect(result).toMatchObject({ author: 'Test Author' });
    });

    it('Test estrazione quando non ci sono metadati', () => {
      const testPng = createTestPNG();

      const result = procMeta(testPng, {
        extract: true
      });

      expect(result).toBeNull();
    });

    it('Test verifica presenza metadati', () => {
      const testPngWithMetadata = createTestPNGWithMetadata();

      const result = procMeta(testPngWithMetadata, {
        verify: true
      });

      expect(result).toBe(true);
    });

    it('Test verifica quando non ci sono metadati', () => {
      const testPng = createTestPNG();

      const result = procMeta(
        testPng,
        { verify: true },
        { raiseErr: false },
      );

      expect(result).toBe(false);
    });

    it('Test aggiornamento metadati esistenti', () => {
      const testPngWithMetadata = createTestPNGWithMetadata();
      const output = path.join(tmpDir, 'updated.png');

      const updateData = {
        author: 'New Author'
      };

      const result = procMeta(testPngWithMetadata, {
        update: updateData,
        out: output
      });

      expect(result).toMatchObject({ success: true });

      // Verifica che i metadati siano stati aggiornati
      const extracted = procMeta(output, {
        extract: true
      });

      expect(extracted).toMatchObject({
        author: 'New Author',
        title: 'Test Image', // Altri metadati preservati
      });
    });

    it('Test con file inesistente', () => {
      const result = procMeta(
        '/nonexistent.png',
        { extract: true },
        { raiseErr: false },
      );

      expect(result).toMatchObject({ success: false, error: 'File not found' });
    });

    it('Test con file non-PNG', () => {
      const textFile = path.join(tmpDir, 'notpng.txt');
      fs.writeFileSync(textFile, 'not a png');

      const result = procMeta(
        textFile,
        { extract: true },
        { raiseErr: false },
      );

      expect(result).toMatchObject({ success: false, error: 'Not PNG' });
    });

    it('Test incorporamento su canali RGB diversi', () => {
      const testPng = createTestPNG();
      const original = readPNG(testPng);

      const metadata = {
        channel_test: 'value'
      };

      // Canale rosso (0)
      const resultR = procMeta(testPng, {
        embed: true,
        data: metadata,
        ch: 0
      });

      expect(resultR).toMatchObject({ success: true });

      const embedR = readPNG(resultR.path);
      expect(channelEquals(original, embedR, [0])).toBe(false); // Canale rosso modificato
      expect(channelEquals(original, embedR, [1, 2])).toBe(true);

      // Canale verde (1)
      const resultG = procMeta(testPng, {
        embed: true,
        data: metadata,
        ch: 1
      });

      expect(resultG).toMatchObject({ success: true });

      const embedG = readPNG(resultG.path);
      expect(channelEquals(original, embedG, [1])).toBe(false); // Canale verde modificato
      expect(channelEquals(original, embedG, [0, 2])).toBe(true);

      // Tutti i canali (-1)
      const resultAll = procMeta(testPng, {
        embed: true,
        data: metadata,
        ch: -1
      });

      expect(resultAll).toMatchObject({ success: true });

      const embedAll = readPNG(resultAll.path);
      expect(channelEquals(original, embedAll, [0, 1, 2])).toBe(false); // Tutti i canali modificati
    });

    function readPNG(filePath) {
      const buffer = fs.readFileSync(filePath);
      return PNG.sync.read(buffer);
    }

    function channelEquals(a, b, channels) {
      if (a.width !== b.width || a.height !== b.height) {
        return false;
      }

      const aChannels = splitChannels(a);
      const bChannels = splitChannels(b);

      for (const ch of channels) {
        for (let i = 0; i < aChannels[ch].length; i++) {
          if (aChannels[ch][i] !== bChannels[ch][i]) {
            return false;
          }
        }
      }
      return true;
    }

    function splitChannels(png) {
      const size = png.width * png.height;
      const channels = [
        new Uint8Array(size),
        new Uint8Array(size),
        new Uint8Array(size)
      ];

      for (let i = 0; i < size; i++) {
        const idx = i << 2;
        channels[0][i] = png.data[idx];
        channels[1][i] = png.data[idx + 1];
        channels[2][i] = png.data[idx + 2];
      }

      return channels;
    }

    it('Test incorporamento di metadati più grandi', () => {
      const testPng = createTestPNG();

      const tags = [];
      for (let i = 0; i < 30; i++) {
        tags.push('tag' + ((i % 3) + 1));
      }

      const metadata = {
        title: 'A'.repeat(100),
        description: 'B'.repeat(200),
        tags: [...'x'.repeat(10)].flatMap(() => ['tag1', 'tag2', 'tag3']),
      };

      const result = procMeta(testPng, {
        embed: true,
        data: metadata
      });

      expect(result).toMatchObject({ success: true });

      // Verifica estrazione
      const extracted = procMeta(result.path, {
        extract: true
      });

      expect(extracted).toEqual(metadata);
    });

    it('Test con metadati JSON annidati', () => {
      const testPng = createTestPNG();

      const metadata = {
        info: {
          author: 'John',
          contact: {
            email: 'john@example.com',
            phone: '123-456-7890'
          }
        },
        stats: [1, 2, 3, 4, 5]
      };

      const result = procMeta(testPng, {
        embed: true,
        data: metadata
      });

      expect(result).toMatchObject({ success: true });

      const extracted = procMeta(result.path, {
        extract: true
      });

      expect(extracted).toEqual(metadata);
    });
  });

  describe('Test per batchProc', () => {

    it('Test incorporamento batch di più immagini', () => {
      const multipleTestPngs = createMultipleTestPNGs(tmpDir, 3);
      const outputDir = path.join(tmpDir, 'output');

      const metadata = {
        batch: 'test',
        number: 42
      };

      const results = batchProc(
        multipleTestPngs,
        { embed: true, data: metadata },
        outputDir
      );

      // Verifica che tutte le immagini siano state processate
      expect(Object.keys(results)).toHaveLength(3);

      const failedOps = Object.entries(results)
        .filter(([_, success]) => !success)
        .map(([path, _]) => path);
      expect(failedOps, `Operazioni fallite: ${failedOps}`).toEqual([]);

      // Verifica che i file di output esistano
      const outputFiles = fs.readdirSync(outputDir);
      expect(outputFiles, `File di output: attesi 3, trovati ${outputFiles.length}`).toHaveLength(3);

      // Verifica che i metadati siano stati effettivamente incorporati
      for (const outputFile of outputFiles) {
        const outputPath = path.join(outputDir, outputFile);
        const extracted = procMeta(outputPath, {
          extract: true
        });
        expect(extracted).toEqual(metadata);
      }
    });

    it('Test batch con parametri personalizzati', () => {
      const multipleTestPngs = createMultipleTestPNGs(tmpDir, 3);
      const outputDir = path.join(tmpDir, 'output_custom');

      const metadata = {
        custom: true
      };

      const results = batchProc(
        multipleTestPngs,
        { embed: true, data: metadata },
        outputDir,
        { channelIdx: 1 },
      );

      // Verifica che tutte le immagini siano state processate
      expect(Object.keys(results)).toHaveLength(3);

      const failedOps = Object.entries(results)
        .filter(([_, success]) => !success)
        .map(([path, _]) => path);
      expect(failedOps, `Operazioni fallite: ${failedOps}`).toEqual([]);

      // Verifica che i file di output esistano
      const outputFiles = fs.readdirSync(outputDir);
      expect(outputFiles, `File di output: attesi 3, trovati ${outputFiles.length}`).toHaveLength(3);

      // Verifica che i metadati siano stati effettivamente incorporati
      for (const outputFile of outputFiles) {
        const outputPath = path.join(outputDir, outputFile);
        const extracted = procMeta(outputPath, {
          extract: true,
          ch: 1,
        }, { hdrSz: 32, bp: 1 });
        expect(extracted).toEqual(metadata);
      }
    });

    it('Test batch handling quando alcuni file falliscono', () => {
      const multipleTestPngs = createMultipleTestPNGs(tmpDir, 3);
      const outputDir = path.join(tmpDir, 'output_errors');

      // Aggiungi un path non valido alla lista
      const pathsWithError = [...multipleTestPngs, '/nonexistent.png'];

      const metadata = {
        test: 'value'
      };

      const results = batchProc(
        pathsWithError,
        { embed: true, data: metadata },
        outputDir
      );

      // Verifica che le immagini valide siano processate
      expect(Object.keys(results)).toHaveLength(4);

      const successCount = Object.values(results).filter(v => v).length;
      expect(successCount).toBe(3); // Solo 3 su 4 hanno successo
      expect(results['/nonexistent.png']).toBe(false);
    });
  });

  // ==================== TEST CHE DIMOSTRANO PROBLEMI ====================

  describe('TestInterfaceProblems - Questi test mostrano i problemi dell\'interfaccia attuale', () => {

    it('PROBLEMA 1: procMeta restituisce tipi diversi a seconda dell\'operazione', () => {
      /*
       * Quando chiami procMeta non sai mai cosa aspettarti:
       * - object per operazioni normali
       * - boolean per embed con output
       * - null per extract fallito
       * - string per embed con verify
       * 
       * Questo rende impossibile scrivere codice robusto senza controllare
       * ogni volta il tipo del risultato!
       */
      const testPng = createTestPNG();

      // Caso 1: restituisce object
      const result1 = procMeta(testPng, {
        embed: true,
        data: { test: 1 }
      });
      expect(typeof result1).toBe('object');
      console.log('Tipo 1:', typeof result1);

      // Caso 2: restituisce boolean (per embed con output)
      const output = path.join(tmpDir, 'out.png');
      const result2 = procMeta(testPng, {
        embed: true,
        data: { test: 1 },
        out: output
      });
      expect(typeof result2).toBe('boolean');
      console.log('Tipo 2:', typeof result2);

      // Caso 3: restituisce null (per extract fallito)
      const result3 = procMeta(testPng, {
        extract: true
      });
      expect(result3).toBeNull();
      console.log('Tipo 3: null');

      // Caso 4: restituisce string (per embed con verify)
      const output2 = path.join(tmpDir, 'verified.png');
      const result4 = procMeta(testPng, {
        embed: true,
        data: { test: 1 },
        out: output2,
        verify: true
      });
      expect(typeof result4).toBe('string');
      console.log('Tipo 4:', typeof result4);

      // Caso 5: restituisce boolean (per verify senza embed!)
      const result5 = procMeta(testPng, {
        verify: true
      });
      expect(typeof result5).toBe('boolean');
      console.log('Tipo 5:', typeof result5);
    });

    it('PROBLEMA 2: la mappa ops ha chiavi abbreviate e poco chiare', () => {
      /*
       * Chi usa la libreria deve ricordarsi:
       * - 'embed' vs 'embedding' vs 'write'
       * - 'extract' vs 'extraction' vs 'read'
       * - 'out' vs 'output' o 'outputPath'
       * - 'data' vs 'metadata' vs 'meta'
       * - 'ch' vs 'channel'
       *
       * Ogni volta devi consultare la documentazione!
       */
      const testPng = createTestPNG();

      const result = procMeta(testPng, {
        embed: true,              // write? save? store?
        data: { key: 'value' },   // metadata? meta? content?
        out: testPng,             // output? outputPath? destination?
        overwrite: true,          // replace? force?
        ch: 0                     // channel? color? component?
      });

      expect(result).toBe(true);
    });

    it('PROBLEMA 3: dettagli implementativi esposti all\'utente', () => {
      /*
       * L'utente deve conoscere:
       * - Il numero magico (0x4D455441)
       * - La dimensione dell'header in bit (32)
       * - Il bit plane da usare (ossia quale bit di ogni byte: 1 = LSB)
       * - L'encoding (utf-8)
       *
       * Questi sono dettagli implementativi che dovrebbero essere nascosti!
       * Chi si ricorda che 0x4D455441 è "META" in ASCII?
       */
      const testPng = createTestPNG();

      const result = procMeta(
        testPng,
        {
          embed: true,
          data: { test: 1 }
        },
        {
          hdrSz: 32,         // Che cos'è 32? Chi lo sa senza guardare la documentazione?
          bp: 1,             // Bit plane? LSB? MSB? Cosa significa?
          enc: 'utf-8',      // Perché devo specificarlo?
          magic: 0x4D455441, // Numero magico! Ma chi lo conosce?
        }
      );

      expect(typeof result).toBe('object');
    });

    it('PROBLEMA 4: nomi dei parametri inconsistenti tra funzioni', () => {
      /*
       * - procMeta usa 'hdrSz', batchProc usa 'headerSz'
       * - procMeta usa 'bp', batchProc usa 'bitPlane'
       * - procMeta usa 'ch' (nell'ops dict!), batchProc usa 'channelIdx'
       *
       * Stesso concetto, nomi diversi. Che confusione!
       */
      const testPng = createTestPNG();

      // procMeta usa hdrSz e bp (come parametri di funzione)
      const result1 = procMeta(
        testPng,
        {
          embed: true,
          data: { test: 1 },
        },
        { hdrSz: 32, bp: 1 },
      );

      expect(result1).toMatchObject({ success: true });

      // batchProc usa headerSz e bitPlane (come parametri di funzione)
      const outDir = path.join(tmpDir, 'out');
      const result2 = batchProc(
        [testPng],
        { embed: true, data: { test: 1 } },
        outDir,
        { headerSz: 32, bitPlane: 1 },
      );

      for (const success of Object.values(result2)) {
        expect(success).toBe(true);
      }

      // Stesso parametro, nomi diversi!
    });

    it('PROBLEMA 5: gestione errori completamente inconsistente', () => {
      /*
       * Quando c'è un errore:
       * - procMeta in modalità embed: restituisce {"success": false, "error": "..."}
       * - procMeta in modalità extract: restituisce null
       * - procMeta in modalità verify: restituisce false
       * - procMeta con raiseErr=rrue: lancia un'eccezione
       *
       * Ogni operazione gestisce gli errori in modo diverso!
       */

      // procMeta modalità embed
      const result1 = procMeta('/nonexistent.png', {
        embed: true,
        data: {}
      });
      console.log('Errore procMeta embed:', result1);
      expect(typeof result1).toBe('object');
      expect(result1.error).toBeTruthy();

      // procMeta modalità extract
      const result2 = procMeta('/nonexistent.png', {
        extract: true
      });
      console.log('Errore procMeta extract:', result2);
      expect(typeof result2).toBe('object');
      expect(result2.error).toBeTruthy();

      // procMeta modalità verify
      const result3 = procMeta('/nonexistent.png', {
        verify: true
      });
      console.log('Errore procMeta verify:', result3);
      expect(result3).toBe(false);

      // procMeta con raiseErr
      expect(() => {
        procMeta('/nonexistent.png', {
          verify: true
        }, { raiseErr: true });
      }).toThrow();
    });

    it('PROBLEMA 6: numeri magici ovunque', () => {
      /*
       * - bp=1: che significa? (è il bit meno significativo)
       * - hdrSz=32: perché 32?
       * - magic=0x4D455441: perché questo numero?
       * - ch=0: cosa significa 0? R? G? B?
       *
       * Senza guardare la documentazione, questi numeri non hanno senso!
       */
      const testPng = createTestPNG();

      procMeta(
        testPng,
        {
          embed: true,
          data: { test: 1 },
          ch: 0
        },
        {
          hdrSz: 32,         // ???
          bp: 1,             // ???
          magic: 0x4D455441, // ???
        });
    });

    it('PROBLEMA 7: procMeta fa TUTTO', () => {
      /*
       * Una singola funzione che:
       * - Incorpora metadati
       * - Estrae metadati
       * - Verifica presenza metadati
       * - Aggiorna metadati esistenti
       * - Salva con path custom
       *
       * Viola il principio della singola responsabilità!
       * È impossibile capire cosa fa senza leggere tutto il codice.
       */

      const testPng = createTestPNG();
      const output = path.join(tmpDir, 'everything.png');

      procMeta(testPng, {
        embed: true,            // incorpora
        data: { key: 'value' }, // questi dati
        out: output,            // salva qui
        verify: true,           // e verifica
        ch: 0                   // sul canale rosso
      });

      // Che cosa ha fatto esattamente? Chi lo sa!
    });

    it('PROBLEMA 8: il parametro channel non è intuitivo', () => {
      /*
       * - ch=0: canale rosso
       * - ch=1: canale verde
       * - ch=2: canale blu
       * - ch=-1: tutti i canali
       *
       * Chi si ricorda che -1 significa "tutti"? E perché non 3?
       */
      const testPng = createTestPNG();

      // Utente confuso: "Voglio usare tutti i canali... ch=3?"
      const result = procMeta(testPng, {
        embed: true,
        data: { test: 1 },
        ch: 3
      });

      expect(result).toEqual({
        success: false,
        error: 'Index 3 out of bounds',
      }); // No, è -1!
    });
  });

  // ==================== TEST PER NUOVE FUNZIONI (da completare) ====================

  describe('TestNewInterfaceIdeas - Questi test rappresentano come DOVREBBE essere la nuova interfaccia. Al momento falliranno perché le funzioni non esistono ancora.', async () => {
    // Importa il modulo sotto test come oggetto
    const stego = await import('./steganography');

    // Salta il test se la funzione 'embedMetadata' non è implementata
    it.skipIf(!stego.embedMetadata)('Funzione proposta: embedMetadata(imagePath, metadata, outputPath=null) -> MetadataResult', () => {
      /*
       * Dovrebbe incorporare metadati in modo semplice e diretto,
       * nascondendo completamente i dettagli implementativi.
       *
       * Esempio d'uso:
       *     const result = embedMetadata('photo.png', {author: 'John'});
       *     // Fatto! Niente magic numbers, niente complessità
       */

      const testPng = createTestPNG();
      const output = path.join(tmpDir, 'embedded.png');

      const result = stego.embedMetadata(testPng, { author: 'John Doe' }, output);
      expect(result).toMatchObject({ success: true });
      expect(fs.existsSync(result.outputPath)).toBe(true);
    });

    it.skipIf(!stego.extractMetadata)('Funzione proposta: extractMetadata(imagePath) -> object', () => {
      /*
       * Interfaccia semplice e chiara per estrarre metadati.
       * Restituisce sempre un oggetto (vuoto se non ci sono metadati).
       *
       * Esempio d'uso:
       *     const metadata = extractMetadata('photo.png');
       *     console.log(metadata.author);
       */

      const testPng = createTestPNGWithMetadata();

      const result = stego.extractMetadata(testPng);
      expect(result).toMatchObject({ author: 'Test Author' });
    });

    it.skipIf(!stego.hasMetadata)('Funzione proposta: hasMetadata(imagePath) -> boolean', () => {
      /*
       * Verifica semplice se l'immagine contiene metadati nascosti.
       *
       * Esempio d'uso:
       *     if (hasMetadata('photo.png')) {
       *         console.log('Questa immagine ha metadati!');
       *     }
       */

      const testPng = createTestPNG();
      const testPngWithMetadata = createTestPNGWithMetadata();

      // Immagine senza metadati
      expect(stego.hasMetadata(testPng)).toBe(false);

      // Immagine con metadati
      expect(stego.hasMetadata(testPngWithMetadata)).toBe(true);
    });

    it.skipIf(!stego.updateMetadata)('Funzione proposta: updateMetadata(imagePath, metadata, outputPath=null) -> MetadataResult', () => {
      /*
       * Aggiorna metadati esistenti o ne crea di nuovi.
       *
       * Esempio d'uso:
       *     const result = updateMetadata('photo.png', {year: 2025});
       *     // I metadati esistenti vengono preservati, solo "year" viene aggiunto/aggiornato
       */

      const testPng = createTestPNGWithMetadata();
      const output = path.join(tmpDir, 'updated.png');

      const result = stego.updateMetadata(testPng, { author: 'New Author' }, output);
      expect(result).toMatchObject({ success: true });

      // Verifica che i vecchi metadati siano preservati
      const updatedMeta = stego.extractMetadata(output);
      expect(updatedMeta).toMatchObject({
        author: 'New Author',
        title: 'Test Image',
      });
    });

    it.skipIf(!stego.clearMetadata)('Funzione proposta: clearMetadata(imagePath, outputPath=null) -> MetadataResult', () => {
      /*
       * Rimuove i metadati nascosti da un'immagine.
       *
       * Esempio d'uso:
       *     const result = clearMetadata('photo.png');
       *     // Metadati rimossi, tutti i LSB impostati a 0
       */

      const testPng = createTestPNGWithMetadata();
      const output = path.join(tmpDir, 'clean.png');

      const result = stego.clearMetadata(testPng, output);
      expect(result).toMatchObject({ success: true });

      // Verifica che non ci siano più metadati
      expect(stego.hasMetadata(output)).toBe(false);
    });

    it.skipIf(!stego.copyMetadata)('Funzione proposta: copyMetadata(sourcePath, destinationPath, outputPath=null) -> MetadataResult', () => {
      /*
       * Copia metadati da un'immagine all'altra.
       *
       * Esempio d'uso:
       *     const result = copyMetadata('source.png', 'destination.png');
       *     // Metadati copiati!
       */

      const testPng = createTestPNG();
      const testPngWithMetadata = createTestPNGWithMetadata();

      const result = stego.copyMetadata(testPngWithMetadata, testPng);
      expect(result).toMatchObject({ success: true });

      // Verifica che non ci siano più metadati
      const sourceMetadata = stego.extractMetadata(testPngWithMetadata);
      const destinationMetadata = stego.extractMetadata(testPng);
      expect(destinationMetadata).toEqual(sourceMetadata);
    });
  });
});

/*
🎯 HAI FINITO LA TUA ANALISI?

Ottimo lavoro! Ora è il momento di dimostrare che la tua nuova interfaccia
è davvero migliore della vecchia.

COSA HAI VISTO NEI TEST SOPRA?

I test in TestInterfaceProblems mostrano 8 problemi gravi:
1. ❌ Tipi di ritorno inconsistenti (object/boolean/null/string)
2. ❌ Chiavi del dizionario ops criptiche ('embed', 'data', 'out', 'ch')
3. ❌ Dettagli implementativi esposti (magic numbers, header size, bit planes)
4. ❌ Nomi parametri inconsistenti (hdrSz vs headerSz, bp vs bitPlane, ch vs channelIdx)
5. ❌ Gestione errori caotica (ogni funzione fa come vuole)
6. ❌ Numeri magici ovunque (32, 1, 0x4D455441, -1)
7. ❌ Una funzione che fa troppo (procMeta)
8. ❌ Parametro channel confuso (0, 1, 2, -1)

La tua nuova interfaccia dovrebbe risolvere TUTTI questi problemi!

PROSSIMI PASSI:

1. Implementa le funzioni suggerite in TestNewInterfaceIdeas (o inventa le tue!)
2. Aggiungi i tuoi test qui sotto in una nuova describe('TestMyNewInterface', ...)
3. I tuoi test dovrebbero dimostrare che:
   ✅ Ogni funzione ha un nome chiaro e auto-esplicativo
   ✅ I parametri sono intuitivi (no abbreviazioni, no dizionari complessi)
   ✅ Tutte le funzioni restituiscono lo stesso tipo di risultato
   ✅ La gestione errori è consistente ovunque
   ✅ Non ci sono dettagli implementativi esposti (magic numbers, bit planes)
   ✅ Ogni funzione ha una singola responsabilità chiara

4. Esempi di test che potresti scrivere:

   it('test consistent return types', () => {
       // Dimostra che tutte le tue funzioni restituiscono lo stesso tipo
       const result1 = embedMetadata(testPng, { test: 1 });
       const result2 = extractMetadata(testPngWithMetadata);
       const result3 = hasMetadata(testPng);
       
       // Tutti restituiscono schemi coerenti e prevedibili
       expect(typeof result1).toBe('object');
       expect(result1).toHaveProperty('success');
       expect(typeof result2).toBe('object');
       expect(typeof result3).toBe('boolean');
   });
   
   it('test consistent error handling', () => {
       // Dimostra che gli errori sono gestiti in modo uniforme
       const result1 = embedMetadata('/nonexistent.png', {});
       const result2 = extractMetadata('/nonexistent.png');
       const result3 = hasMetadata('/nonexistent.png');
       
       // Tutti gestiscono gli errori nello stesso modo
       // (es. tutti restituiscono success=false, o tutti sollevano eccezioni)
   });

BONUS: Considera di aggiungere funzioni di convenienza per casi d'uso comuni:
- addCopyright(imagePath, copyrightText): aggiunge copyright come metadato
- addAuthorInfo(imagePath, name, email): aggiunge info autore
- addCreationDate(imagePath, date): aggiunge data (default: oggi)
- getImageInfo(imagePath): restituisce sia metadati che info base sull'immagine
- batchEmbedMetadata(imagePaths, metadata, outputDir): incorpora gli stessi metadati in più immagini

Buon lavoro! 🚀
*/
