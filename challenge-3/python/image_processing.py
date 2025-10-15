"""
Image Processing Library - BEFORE refactoring

Questa libreria fa da wrapper a PIL per operazioni comuni sulle immagini.
PROBLEMA: le interfacce sono troppo complesse e "trapelano" dettagli implementativi.

Il tuo compito: riprogettare l'interfaccia per renderla semplice e intuitiva.
"""

import os
from typing import Dict, Optional, Union

from PIL import ExifTags, Image


def img_op(
    p: str,
    ops: Dict,
    exif_orient: int = 274,
    rot_map: Optional[Dict[int, int]] = None,
    rsample: int = 3,
    raise_err: bool = False,
) -> Union[Dict, bool, None, str]:
    """
    Esegue operazioni su un'immagine. Può fare resize, rotazione, conversione formato,
    e creazione thumbnail a seconda delle chiavi nel dizionario ops.

    Args:
        p: percorso file
        ops: dizionario operazioni con chiavi: 'orient', 'rotate', 'thumb', 'size',
             'fmt', 'qual', 'out', 'expand', 'maintain_aspect'
        exif_orient: numero tag EXIF orientamento
        rot_map: mappa valori EXIF -> gradi
        rsample: metodo ricampionamento per rotazioni (0-5 vedi PIL)
        raise_err: solleva eccezioni invece di restituire errori

    Returns:
        Dipende dall'operazione: dict con info, bool per successo, None per errore,
        o stringa path
    """
    if rot_map is None:
        rot_map = {3: 180, 6: 270, 8: 90}

    try:
        if not os.path.exists(p):
            if raise_err:
                raise FileNotFoundError(f"File not found: {p}")
            return {"success": False, "error": "File not found"}

        with Image.open(p) as img:
            # Orientamento EXIF
            if ops.get("orient", False):
                exif = img.getexif()
                if exif and exif_orient in exif:
                    v = exif[exif_orient]
                    if v in rot_map:
                        img = img.rotate(rot_map[v], expand=True)

            # Rotazione manuale
            if "rotate" in ops:
                deg = ops["rotate"]
                img = img.rotate(deg, expand=ops.get("expand", True), resample=rsample)

            # Thumbnail o resize
            if "thumb" in ops:
                w, h = ops["thumb"]
                if ops.get("maintain_aspect", True):
                    img.thumbnail((w, h))
                else:
                    img = img.resize((w, h))
            elif "size" in ops:
                img.thumbnail(ops["size"])

            # Salvataggio
            output_path = ops.get("out", p)
            if "fmt" in ops:
                base, _ = os.path.splitext(p)
                output_path = f"{base}.{ops['fmt']}"

            quality = ops.get("qual", 85)
            img.save(output_path, quality=quality)

            # Tipo di return basato su ops
            if "thumb" in ops:
                return True
            elif "rotate" in ops and "out" in ops:
                return output_path
            else:
                return {
                    "success": True,
                    "path": output_path,
                    "size": img.size,
                    "format": img.format,
                }

    except Exception as ex:
        if raise_err:
            raise
        if "thumb" in ops:
            return False
        elif "rotate" in ops:
            return None
        return {"success": False, "error": str(ex)}


def get_exif(p: str, tag_nums: Optional[list] = None, use_names: bool = True) -> Dict:
    """
    Estrae dati EXIF da immagine.

    Args:
        p: percorso immagine
        tag_nums: lista tag numerici da estrarre (None = tutti)
        use_names: converte numeri tag in nomi leggibili

    Returns:
        dizionario metadati
    """
    try:
        with Image.open(p) as img:
            exif = img.getexif()
            if not exif:
                return {}

            result = {}
            for tag, value in exif.items():
                if tag_nums and tag not in tag_nums:
                    continue

                if use_names:
                    tag_name = ExifTags.TAGS.get(tag, tag)
                    result[tag_name] = value
                else:
                    result[tag] = value

            return result
    except Exception as e:
        return {"error": str(e)}


def batch_op(
    paths: list,
    ops: Dict,
    out_dir: str,
    exif_tag: int = 274,
    rotation_vals: Optional[Dict[int, int]] = None,
    resample_method: int = 3,
) -> Dict[str, bool]:
    """
    Processa batch di immagini con stesse operazioni.

    Args:
        paths: lista percorsi immagini
        ops: dizionario operazioni (vedi img_op)
        out_dir: directory output
        exif_tag: tag EXIF per orientamento (default 274)
        rotation_vals: mappa valori EXIF a gradi rotazione
        resample_method: metodo ricampionamento (0-5)

    Returns:
        dict {path_originale: success_bool}
    """
    results = {}

    if not os.path.exists(out_dir):
        os.makedirs(out_dir)

    if rotation_vals is None:
        rotation_vals = {3: 180, 6: 270, 8: 90}

    for img_path in paths:
        try:
            filename = os.path.basename(img_path)
            output_path = os.path.join(out_dir, filename)

            ops_copy = ops.copy()
            ops_copy["out"] = output_path

            result = img_op(
                img_path,
                ops_copy,
                exif_orient=exif_tag,
                rot_map=rotation_vals,
                rsample=resample_method,
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
