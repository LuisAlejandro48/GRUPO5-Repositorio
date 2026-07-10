import html
import re
import textwrap
from pathlib import Path


ROOT = Path(__file__).resolve().parent
HTML_FILE = ROOT / "imports_reciclaje11.html"
PDF_FILE = ROOT / "imports_reciclaje11.pdf"


def texto_desde_html(source):
    raw = source.read_text(encoding="utf-8")
    raw = re.sub(r"<style.*?</style>", "", raw, flags=re.S)
    raw = re.sub(r"<h1>(.*?)</h1>", r"\n# \1\n", raw, flags=re.S)
    raw = re.sub(r"<h2>(.*?)</h2>", r"\n## \1\n", raw, flags=re.S)
    raw = re.sub(r"<h3>(.*?)</h3>", r"\n### \1\n", raw, flags=re.S)
    raw = re.sub(r"<p.*?>(.*?)</p>", r"\1\n", raw, flags=re.S)
    raw = re.sub(r"<code>(.*?)</code>", r"`\1`", raw, flags=re.S)
    raw = re.sub(r"<[^>]+>", "", raw)
    raw = html.unescape(raw)
    raw = re.sub(r"[ \t]+", " ", raw)
    raw = re.sub(r"\n{3,}", "\n\n", raw)
    return raw.strip().splitlines()


def cortar_lineas(lines):
    result = []
    for line in lines:
        line = line.strip()
        if not line:
            result.append(("", "blank"))
        elif line.startswith("# "):
            result.append((line[2:], "title"))
        elif line.startswith("## "):
            result.append((line[3:], "section"))
        elif line.startswith("### "):
            result.append((line[4:], "file"))
        else:
            for part in textwrap.wrap(line, width=94):
                result.append((part, "text"))
    return result


def pdf_text(value):
    data = value.encode("cp1252", errors="replace")
    out = bytearray()
    for byte in data:
        if byte in (40, 41, 92):
            out.extend(b"\\" + bytes([byte]))
        elif byte < 32 or byte > 126:
            out.extend(f"\\{byte:03o}".encode("ascii"))
        else:
            out.append(byte)
    return bytes(out)


def stream_para_pagina(items):
    parts = [b"BT\n"]
    y = 780
    for text, kind in items:
        if kind == "blank":
            y -= 8
            continue
        if kind == "title":
            size = 22
            leading = 28
            x = 50
        elif kind == "section":
            size = 15
            leading = 21
            x = 50
            y -= 8
        elif kind == "file":
            size = 12
            leading = 17
            x = 58
        else:
            size = 9
            leading = 12
            x = 66
        parts.append(f"/F1 {size} Tf\n1 0 0 1 {x} {y} Tm\n(".encode("ascii"))
        parts.append(pdf_text(text))
        parts.append(b") Tj\n")
        y -= leading
    parts.append(b"ET\n")
    return b"".join(parts)


def paginar(items):
    pages = []
    current = []
    y = 780
    for text, kind in items:
        needed = {
            "title": 28,
            "section": 29,
            "file": 17,
            "text": 12,
            "blank": 8,
        }[kind]
        if y - needed < 45 and current:
            pages.append(current)
            current = []
            y = 780
        current.append((text, kind))
        y -= needed
    if current:
        pages.append(current)
    return pages


def crear_pdf(pages):
    objects = []

    def add(obj):
        objects.append(obj)
        return len(objects)

    catalog_id = add(b"<< /Type /Catalog /Pages 2 0 R >>")
    pages_id = add(b"")
    font_id = add(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>")
    page_ids = []

    for page_items in pages:
        stream = stream_para_pagina(page_items)
        stream_id = add(
            b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"endstream"
        )
        page_id = add(
            b"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
            b"/Resources << /Font << /F1 " + str(font_id).encode("ascii") + b" 0 R >> >> "
            b"/Contents " + str(stream_id).encode("ascii") + b" 0 R >>"
        )
        page_ids.append(page_id)

    kids = b" ".join(str(page_id).encode("ascii") + b" 0 R" for page_id in page_ids)
    objects[pages_id - 1] = (
        b"<< /Type /Pages /Kids [" + kids + b"] /Count " + str(len(page_ids)).encode("ascii") + b" >>"
    )

    output = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets = [0]
    for index, obj in enumerate(objects, start=1):
        offsets.append(len(output))
        output.extend(f"{index} 0 obj\n".encode("ascii"))
        output.extend(obj)
        output.extend(b"\nendobj\n")

    xref = len(output)
    output.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
    output.extend(b"0000000000 65535 f \n")
    for offset in offsets[1:]:
        output.extend(f"{offset:010d} 00000 n \n".encode("ascii"))
    output.extend(
        b"trailer\n<< /Size " + str(len(objects) + 1).encode("ascii")
        + b" /Root " + str(catalog_id).encode("ascii") + b" 0 R >>\nstartxref\n"
        + str(xref).encode("ascii") + b"\n%%EOF\n"
    )
    PDF_FILE.write_bytes(output)


if __name__ == "__main__":
    crear_pdf(paginar(cortar_lineas(texto_desde_html(HTML_FILE))))
    print(PDF_FILE)
