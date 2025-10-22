/**
 * Steganography Library - BEFORE refactoring
 *
 * Questa libreria permette di nascondere metadati JSON in immagini PNG
 * usando la tecnica LSB (Least Significant Bit).
 * PROBLEMA: le interfacce sono troppo complesse e "trapelano" dettagli implementativi.
 *
 * Il tuo compito: riprogettare l'interfaccia per renderla semplice e intuitiva.
 */

import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs';
import { extname, basename, dirname, join } from 'path';
import { PNG } from 'pngjs';

/**
 * Processa metadati su un'immagine PNG. Può fare embed, extract, verify,
 * e update a seconda delle chiavi nel dizionario ops.
 *
 * @param {string} p - percorso file
 * @param {Object} ops - dizionario operazioni con chiavi: 'embed', 'extract', 'verify',
 *                       'data', 'out', 'ch', 'overwrite'
 * @param {number} hdrSz - dimensione header in bit
 * @param {number} bp - bit plane da usare (1 = LSB)
 * @param {string} enc - encoding per serializzazione JSON
 * @param {number} magic - numero magico per identificare metadati validi
 * @param {boolean} raiseErr - solleva eccezioni invece di restituire errori
 *
 * @returns {Object|boolean|null|string} Dipende dall'operazione: object con metadati, bool per successo,
 *                                        null per errore, o stringa path
 */
export function procMeta(
  p,
  ops,
  {
    hdrSz = 32,
    bp = 1,
    enc = 'utf-8',
    magic = 0x4D455441,
    raiseErr = false,
  } = {},
) {
  try {
    if (!existsSync(p)) {
      if (raiseErr) {
        throw new Error(`File not found: ${p}`);
      }
      if (ops.verify) {
        return false;
      }
      return { success: false, error: 'File not found' };
    }

    const buffer = readFileSync(p);
    let img;

    try {
      img = PNG.sync.read(buffer);
    } catch (e) {
      if (raiseErr) {
        throw new Error('Not a valid PNG file');
      }
      if (ops.verify) {
        return false;
      }
      return { success: false, error: 'Not PNG' };
    }

    // Extract
    if (ops.extract) {
      const ch = ops.ch ?? -1;
      const extracted = extBits(img, hdrSz, bp, ch, magic, enc);
      if (extracted === null) {
        if (raiseErr) {
          throw new Error('No metadata found');
        }
        return null;
      }
      return extracted;
    }

    // Embed
    if (ops.embed) {
      if (!ops.data) {
        if (raiseErr) {
          throw new Error('No data to embed');
        }
        return { success: false, error: 'No data' };
      }

      const metaData = ops.data;
      const ch = ops.ch ?? -1;

      const modifiedImg = embBits(img, metaData, hdrSz, bp, ch, magic, enc);

      let outputPath = ops.out ?? p;
      if (!ops.overwrite && outputPath === p) {
        const ext = extname(p);
        const base = basename(p, ext);
        const dir = dirname(p);
        outputPath = join(dir, `${base}_embedded${ext}`);
      }

      const outBuffer = PNG.sync.write(modifiedImg);
      writeFileSync(outputPath, outBuffer);

      if (ops.verify) {
        return outputPath;
      } else if (ops.data && ops.out) {
        return true;
      } else {
        return {
          success: true,
          path: outputPath,
          size: JSON.stringify(metaData).length
        };
      }
    }

    // Update (extract + modify + embed)
    if (ops.update) {
      const ch = ops.ch ?? -1;
      let existing = extBits(img, hdrSz, bp, ch, magic, enc);

      if (existing === null) {
        existing = {};
      }

      Object.assign(existing, ops.update);

      const modifiedImg = embBits(img, existing, hdrSz, bp, ch, magic, enc);

      const outputPath = ops.out ?? p;
      const outBuffer = PNG.sync.write(modifiedImg);
      writeFileSync(outputPath, outBuffer);

      return { success: true, path: outputPath };
    }

    // Verify
    if (ops.verify) {
      const ch = ops.ch ?? -1;
      const hasMeta = checkMeta(img, hdrSz, bp, ch, magic);
      return hasMeta;
    }

    return { success: false, error: 'No valid operation' };

  } catch (ex) {
    if (raiseErr) {
      throw ex;
    }
    if (ops.verify) {
      return false;
    } else if (ops.extract) {
      return null;
    }
    return { success: false, error: ex.message };
  }
}

/**
 * Nasconde metadati JSON nei bit meno significativi dell'immagine
 *
 * @param {PNG} img
 * @param {Object} metadata
 * @param {number} headerSize
 * @param {number} bitPlane
 * @param {number} channel
 * @param {number} magicNum
 * @param {string} encoding
 *
 * @returns {PNG}
 */
function embBits(
  img,
  metadata,
  headerSize,
  bitPlane,
  channel,
  magicNum,
  encoding,
) {
  const jsonStr = JSON.stringify(metadata);
  const jsonBytes = Buffer.from(jsonStr, encoding);

  // Crea header: magic number (4 bytes) + lunghezza (4 bytes)
  const payloadLen = jsonBytes.length;
  const header = Buffer.alloc(8);
  header.writeUInt32BE(magicNum, 0);
  header.writeUInt32BE(payloadLen, 4);

  const fullPayload = Buffer.concat([header, jsonBytes]);

  // Converti in lista di bit
  const bits = [];
  for (const byte of fullPayload) {
    for (let i = 7; i >= 0; i--) {
      bits.push((byte >> i) & 1);
    }
  }

  // Create a copy to modify
  const modified = new PNG({
    width: img.width,
    height: img.height
  });
  img.data.copy(modified.data);

  const width = modified.width;
  const height = modified.height;
  let bitIdx = 0;

  // Define a bit-mask to force the steganographic data bit to 0
  const dataBit = bitPlane - 1;
  const mask = ~(1 << dataBit);

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      if (bitIdx >= bits.length) {
        break;
      }

      const idx = (width * y + x) << 2;
      const pixel = modified.data.slice(idx, idx + 3);

      // Embed in specific channel or all channels
      if (channel === -1) {
        // All channels
        for (let c = 0; c < 3; c++) {
          if (bitIdx >= bits.length) {
            break;
          }
          pixel[c] = (pixel[c] & mask) | (bits[bitIdx] << dataBit);
          bitIdx++;
        }
      } else if (channel >= 3) {
        throw new RangeError(`Index ${channel} out of bounds`)
      } else {
        // Specific channel
        pixel[channel] = (pixel[channel] & mask) | (bits[bitIdx] << dataBit);
        bitIdx++;
      }

      // The buffer `modified` was already updated during the loop
    }
  }

  return modified;
}

/**
 * Estrae metadati JSON nascosti nell'immagine
 *
 * @param {PNG} img
 * @param {number} headerSize
 * @param {number} bitPlane
 * @param {number} channel
 * @param {number} magicNum
 * @param {string} encoding
 *
 * @returns {Object|null}
 */
function extBits(
  img,
  headerSize,
  bitPlane,
  channel,
  magicNum,
  encoding,
) {
  const bits = readBits(img, headerSize, bitPlane, channel, -1);

  // Decode header (8 bytes = 64 bits)
  const headerBytes = bitsToBytes(bits.slice(0, 64));
  if (headerBytes.length < 4) {
    return null;
  }

  const foundMagic = headerBytes.readUInt32BE(0);
  if (foundMagic !== magicNum) {
    return null;
  }

  // Extract payload
  if (headerBytes.length < 8) {
    return null;
  }

  const payloadLen = headerBytes.readUInt32BE(4);
  const neededBits = payloadLen * 8;
  if (64 + neededBits > bits.length) {
    return null;
  }

  const payloadBytes = bitsToBytes(bits.slice(64, 64 + neededBits));

  try {
    const jsonStr = payloadBytes.toString(encoding);
    return JSON.parse(jsonStr);
  } catch {
    return null;
  }
}

/**
 * @param {PNG} img
 * @param {number} headerSize
 * @param {number} bitPlane
 * @param {number} channel
 * @param {number} bitn
 *
 * @returns {Array<number>}
 */
function readBits(
  img,
  headerSize,
  bitPlane,
  channel,
  bitn = -1,
) {
  const width = img.width;
  const height = img.height;

  const bits = [];
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      if (bitn >= 0 && bits.length >= bitn) {
        break;
      }

      const idx = (width * y + x) << 2;
      const pixel = img.data.slice(idx, idx + 3);

      if (channel === -1) {
        // Extract from all channels
        for (let c = 0; c < 3; c++) {
          if (bitn >= 0 && bits.length >= bitn) {
            break;
          }
          const bit = (pixel[c] >> (bitPlane - 1)) & 1;
          bits.push(bit);
        }
      } else if (channel >= 3) {
        throw new RangeError(`Index ${channel} out of bounds`)
      } else {
        // Extract from specific channel
        const bit = (pixel[channel] >> (bitPlane - 1)) & 1;
        bits.push(bit);
      }
    }
  }
  return bits;
}

/**
 * Verifica se l'immagine contiene metadati validi
 *
 * @param {PNG} img
 * @param {number} headerSize
 * @param {number} bitPlane
 * @param {number} channel
 * @param {number} magicNum
 *
 * @returns {boolean}
 */
function checkMeta(
  img,
  headerSize,
  bitPlane,
  channel,
  magicNum,
) {
  const width = img.width;
  const height = img.height;

  // Extract only magic number (first 32 bits)
  const bits = readBits(img, headerSize, bitPlane, channel, 32);

  const headerBytes = bitsToBytes(bits);
  if (headerBytes.length < 4) {
    return false;
  }

  const foundMagic = headerBytes.readUInt32BE(0);
  return foundMagic === magicNum;
}

/**
 * Converte lista di bit in bytes
 *
 * @param {Array<number>} bits
 * @returns {Buffer}
 */
function bitsToBytes(bits) {
  const result = [];
  for (let i = 0; i < bits.length; i += 8) {
    let byte = 0;
    for (let j = 0; j < 8; j++) {
      if (i + j < bits.length) {
        byte = (byte << 1) | bits[i + j];
      } else {
        byte = byte << 1;
      }
    }
    result.push(byte);
  }
  return Buffer.from(result);
}

/**
 * Processa batch di immagini con stesse operazioni.
 *
 * @param {string[]} paths - lista percorsi immagini
 * @param {Object} ops - dizionario operazioni (vedi procMeta)
 * @param {string} outDir - directory output
 * @param {number} headerSz - dimensione header in bit
 * @param {number} bitPlane - bit plane da usare
 * @param {number} channelIdx - indice canale RGB (0=R, 1=G, 2=B, -1=tutti)
 *
 * @returns {Object} dict {pathOriginale: successBoolean}
 */
export function batchProc(
  paths,
  ops,
  outDir,
  {
    headerSz = 32,
    bitPlane = 1,
    channelIdx = -1,
  } = {},
) {
  const results = {};

  if (!existsSync(outDir)) {
    mkdirSync(outDir, { recursive: true });
  }

  for (const imgPath of paths) {
    try {
      const filename = basename(imgPath);
      const outputPath = join(outDir, filename);

      const opsCopy = { ...ops };
      opsCopy.out = outputPath;
      opsCopy.ch = channelIdx;

      const result = procMeta(
        imgPath,
        opsCopy,
        { hdrSz: headerSz, bp: bitPlane },
      );

      if (typeof result === 'object' && result !== null && !Array.isArray(result)) {
        results[imgPath] = result.success || false;
      } else if (typeof result === 'boolean') {
        results[imgPath] = result;
      } else if (typeof result === 'string') {
        results[imgPath] = existsSync(result);
      } else {
        results[imgPath] = false;
      }
    } catch {
      results[imgPath] = false;
    }
  }

  return results;
}
