"""
Steganography Library - BEFORE refactoring

Questa libreria permette di nascondere metadati JSON in immagini PNG
usando la tecnica LSB (Least Significant Bit).
PROBLEMA: le interfacce sono troppo complesse e "trapelano" dettagli implementativi.

Il tuo compito: riprogettare l'interfaccia per renderla semplice e intuitiva.
"""

import json
import os
from typing import Dict, Optional, Union

from PIL import Image


def proc_meta(
    p: str | os.PathLike,
    ops: Dict,
    hdr_sz: int = 32,
    bp: int = 1,
    enc: str = "utf-8",
    magic: int = 0x4D455441,
    raise_err: bool = False,
) -> Union[Dict, bool, None, str]:
    """
    Processa metadati su un'immagine PNG. Può fare embed, extract, verify,
    e update a seconda delle chiavi nel dizionario ops.

    Args:
        p: percorso file
        ops: dizionario operazioni con chiavi: 'embed', 'extract', 'verify',
             'data', 'out', 'ch', 'overwrite'
        hdr_sz: dimensione header in bit
        bp: bit plane da usare (1 = LSB)
        enc: encoding per serializzazione JSON
        magic: numero magico per identificare metadati validi
        raise_err: solleva eccezioni invece di restituire errori

    Returns:
        Dipende dall'operazione: dict con metadati, bool per successo,
        None per errore, o stringa path
    """
    try:
        if not os.path.exists(p):
            if raise_err:
                raise FileNotFoundError(f"File not found: {p}")
            if ops.get("verify"):
                return False
            return {"success": False, "error": "File not found"}

        try:
            img = Image.open(p)
        except:
            if raise_err:
                raise ValueError("Not a valid PNG file")
            if ops.get("verify"):
                return False
            return {"success": False, "error": "Not PNG"}

        if img.format != "PNG":
            if raise_err:
                raise ValueError("Not a valid PNG file")
            if ops.get("verify"):
                return False
            return {"success": False, "error": "Not PNG"}

        # Convert to RGB if needed
        if img.mode != "RGB":
            img = img.convert("RGB")

        # Extract
        if ops.get("extract", False):
            ch = ops.get("ch", -1)
            extracted = _ext_bits(img, hdr_sz, bp, ch, magic, enc)
            if extracted is None:
                if raise_err:
                    raise ValueError("No metadata found")
                return None
            return extracted

        # Embed
        if ops.get("embed", False):
            if "data" not in ops:
                if raise_err:
                    raise ValueError("No data to embed")
                return {"success": False, "error": "No data"}

            meta_data = ops["data"]
            ch = ops.get("ch", -1)

            modified_img = _emb_bits(img, meta_data, hdr_sz, bp, ch, magic, enc)

            output_path = ops.get("out", p)
            if not ops.get("overwrite", False) and output_path == p:
                base, ext = os.path.splitext(p)
                output_path = f"{base}_embedded{ext}"

            modified_img.save(output_path, "PNG")

            if "verify" in ops and ops["verify"]:
                return output_path
            elif "data" in ops and "out" in ops:
                return True
            else:
                return {
                    "success": True,
                    "path": output_path,
                    "size": len(json.dumps(meta_data)),
                }

        # Update (extract + modify + embed)
        if "update" in ops:
            ch = ops.get("ch", -1)
            existing = _ext_bits(img, hdr_sz, bp, ch, magic, enc)

            if existing is None:
                existing = {}

            existing.update(ops["update"])

            modified_img = _emb_bits(img, existing, hdr_sz, bp, ch, magic, enc)

            output_path = ops.get("out", p)
            modified_img.save(output_path, "PNG")

            return {"success": True, "path": output_path}

        # Verify
        if ops.get("verify", False):
            ch = ops.get("ch", -1)
            has_meta = _check_meta(img, hdr_sz, bp, ch, magic)
            return has_meta

        return {"success": False, "error": "No valid operation"}

    except Exception as ex:
        if raise_err:
            raise
        if ops.get("verify"):
            return False
        elif ops.get("extract"):
            return None
        return {"success": False, "error": str(ex)}


def _emb_bits(
    img: Image.Image,
    metadata: Dict,
    header_size: int,
    bit_plane: int,
    channel: int,
    magic_num: int,
    encoding: str,
) -> Image.Image:
    """Nasconde metadati JSON nei bit meno significativi dell'immagine"""
    json_str = json.dumps(metadata)
    json_bytes = json_str.encode(encoding)

    # Crea header: magic number (4 bytes) + lunghezza (4 bytes)
    payload_len = len(json_bytes)
    header = magic_num.to_bytes(4, "big") + payload_len.to_bytes(4, "big")
    full_payload = header + json_bytes

    # Converti in lista di bit
    bits = []
    for byte in full_payload:
        for i in range(7, -1, -1):
            bits.append((byte >> i) & 1)

    # Create a copy to modify
    modified = img.copy()
    pixels = modified.load()

    width, height = modified.size
    bit_idx = 0

    # Define a bit-mask to force the steganographic data bit to 0
    data_bit = bit_plane - 1
    mask = ~(1 << data_bit)

    for y in range(height):
        for x in range(width):
            if bit_idx >= len(bits):
                break

            pixel = list(pixels[x, y])

            # Embed in specific channel or all channels
            if channel == -1:
                # All channels
                for c in range(3):
                    if bit_idx >= len(bits):
                        break
                    pixel[c] = (pixel[c] & mask) | (bits[bit_idx] << data_bit)
                    bit_idx += 1
            else:
                # Specific channel
                pixel[channel] = (pixel[channel] & mask) | (bits[bit_idx] << data_bit)
                bit_idx += 1

            pixels[x, y] = tuple(pixel)

    return modified


def _ext_bits(
    img: Image.Image,
    header_size: int,
    bit_plane: int,
    channel: int,
    magic_num: int,
    encoding: str,
) -> Optional[Dict]:
    """Estrae metadati JSON nascosti nell'immagine"""
    bits = _read_bits(img, header_size, bit_plane, channel)

    # Decode header (8 bytes = 64 bits)
    header_bytes = _bits_to_bytes(bits[:64])
    found_magic = int.from_bytes(header_bytes[:4], "big")

    if found_magic != magic_num:
        return None

    # Extract payload
    payload_len = int.from_bytes(header_bytes[4:8], "big")
    needed_bits = payload_len * 8
    payload_bytes = _bits_to_bytes(bits[64 : 64 + needed_bits])

    try:
        json_str = payload_bytes.decode(encoding)
        return json.loads(json_str)
    except:
        return None


def _read_bits(
    img: Image.Image,
    header_size: int,
    bit_plane: int,
    channel: int,
    bitn: int = -1,
) -> list[int]:
    pixels = img.load()
    width, height = img.size

    bits = []
    for y in range(height):
        for x in range(width):
            if bitn >= 0 and len(bits) >= bitn:
                break

            pixel = pixels[x, y]

            if channel == -1:
                # Extract from all channels
                for c in range(3):
                    if bitn >= 0 and len(bits) >= bitn:
                        break
                    bit = (pixel[c] >> (bit_plane - 1)) & 1
                    bits.append(bit)
            else:
                # Extract from specific channel
                bit = (pixel[channel] >> (bit_plane - 1)) & 1
                bits.append(bit)
    return bits


def _check_meta(
    img: Image.Image,
    header_size: int,
    bit_plane: int,
    channel: int,
    magic_num: int,
) -> bool:
    """Verifica se l'immagine contiene metadati validi"""
    pixels = img.load()
    width, height = img.size

    # Extract only magic number (first 32 bits)
    bits = _read_bits(img, header_size, bit_plane, channel, 32)

    header_bytes = _bits_to_bytes(bits)
    found_magic = int.from_bytes(header_bytes[:4], "big")

    return found_magic == magic_num


def _bits_to_bytes(bits: list[int]) -> bytes:
    """Converte lista di bit in bytes"""
    result = []
    for i in range(0, len(bits), 8):
        byte = 0
        for j in range(8):
            if i + j < len(bits):
                byte = (byte << 1) | bits[i + j]
            else:
                byte = byte << 1
        result.append(byte)
    return bytes(result)


def batch_proc(
    paths: list[str | os.PathLike],
    ops: Dict,
    out_dir: str | os.PathLike,
    header_sz: int = 32,
    bit_plane: int = 1,
    channel_idx: int = -1,
) -> Dict[str, bool]:
    """
    Processa batch di immagini con stesse operazioni.

    Args:
        paths: lista percorsi immagini
        ops: dizionario operazioni (vedi proc_meta)
        out_dir: directory output
        header_sz: dimensione header in bit
        bit_plane: bit plane da usare
        channel_idx: indice canale RGB (0=R, 1=G, 2=B, -1=tutti)

    Returns:
        dict {path_originale: success_bool}
    """
    results = {}

    if not os.path.exists(out_dir):
        os.makedirs(out_dir)

    for img_path in paths:
        try:
            filename = os.path.basename(img_path)
            output_path = os.path.join(out_dir, filename)

            ops_copy = ops.copy()
            ops_copy["out"] = output_path
            ops_copy["ch"] = channel_idx

            result = proc_meta(
                img_path,
                ops_copy,
                hdr_sz=header_sz,
                bp=bit_plane,
            )

            if isinstance(result, dict):
                results[img_path] = result.get("success", False)
            elif isinstance(result, bool):
                results[img_path] = result
            elif isinstance(result, str):
                results[img_path] = os.path.exists(result)
            else:
                results[img_path] = False
        except:
            results[img_path] = False

    return results
