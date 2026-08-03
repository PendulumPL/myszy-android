from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.units import mm


OUT = Path(__file__).resolve().parents[1] / "output" / "pdf" / "portfolio-demo-bank-statement.pdf"


def rounded_card(pdf, x, y, w, h, fill=colors.white, radius=10 * mm):
    pdf.setFillColor(fill)
    pdf.roundRect(x, y, w, h, radius, stroke=0, fill=1)


def make_pdf():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    width, height = A4
    pdf = canvas.Canvas(str(OUT), pagesize=A4)
    pdf.setTitle("Myszy Bank - fikcyjny wyciag do testu importu PDF")

    cream = colors.HexColor("#FFF8F1")
    terracotta = colors.HexColor("#C86F52")
    sage = colors.HexColor("#6F8F78")
    ink = colors.HexColor("#34302E")
    line = colors.HexColor("#E8D9CF")

    pdf.setFillColor(cream)
    pdf.rect(0, 0, width, height, stroke=0, fill=1)

    left = 18 * mm
    right = width - 18 * mm
    card_w = right - left

    # Header card.
    rounded_card(pdf, left, 225 * mm, card_w, 43 * mm)
    pdf.setFillColor(terracotta)
    pdf.setFont("Helvetica-Bold", 22)
    pdf.drawString(28 * mm, 253 * mm, "Myszy Bank - DEMO")
    pdf.setFillColor(sage)
    pdf.setFont("Helvetica-Bold", 10)
    pdf.drawRightString(right - 8 * mm, 253 * mm, "KONTO TESTOWE")
    pdf.setFillColor(ink)
    pdf.setFont("Helvetica", 10)
    pdf.drawString(28 * mm, 244 * mm, "FIKCYJNY WYCIAG BANKOWY - NIE JEST DOKUMENTEM BANKOWYM")
    pdf.drawRightString(right - 8 * mm, 244 * mm, "Myszy DEV / portfolio")

    # Human-readable table.
    pdf.setFillColor(sage)
    pdf.setFont("Helvetica-Bold", 10)
    pdf.drawString(24 * mm, 207 * mm, "DATA")
    pdf.drawString(70 * mm, 207 * mm, "OPIS TRANSAKCJI")
    pdf.drawRightString(177 * mm, 207 * mm, "KWOTA")

    rows = [
        ("01.08.2026", "Myszy Market - zakupy do norki", "-51,00 PLN"),
        ("02.08.2026", "Kino Myszy - bilety", "-86,00 PLN"),
        ("03.08.2026", "Kawa i ciasto - test", "-34,00 PLN"),
        ("04.08.2026", "Myszy Taxi - przejazd", "-44,90 PLN"),
        ("05.08.2026", "Myszy Market - domowe okruszki", "-118,30 PLN"),
    ]
    y = 195 * mm
    pdf.setFont("Helvetica", 10)
    for date, description, amount in rows:
        pdf.setStrokeColor(line)
        pdf.line(22 * mm, y + 5 * mm, 188 * mm, y + 5 * mm)
        pdf.setFillColor(ink)
        pdf.drawString(24 * mm, y, date)
        pdf.drawString(70 * mm, y, description)
        pdf.setFillColor(terracotta)
        pdf.setFont("Helvetica-Bold", 10)
        pdf.drawRightString(177 * mm, y, amount)
        pdf.setFont("Helvetica", 10)
        y -= 13 * mm

    # Parser-friendly appendix. Keep every field on its own line and inside the card.
    appendix_y = 50 * mm
    appendix_h = 91 * mm
    rounded_card(pdf, left, appendix_y, card_w, appendix_h)
    pdf.setFillColor(sage)
    pdf.setFont("Helvetica-Bold", 10)
    pdf.drawString(26 * mm, appendix_y + appendix_h - 13 * mm, "DANE DO TESTU IMPORTU")

    blocks = [
        ("01.08.2026 02.08.2026", "Transakcja kart", "Opis transakcji: Myszy Market - zakupy", "Kwota: -51,00"),
        ("02.08.2026 03.08.2026", "BLIK", "Odbiorca: Kino Myszy", "Kwota: -86,00"),
        ("03.08.2026 04.08.2026", "Transakcja kart", "Opis transakcji: Kawa i ciasto", "Kwota: -34,00"),
        ("04.08.2026 05.08.2026", "BLIK", "Odbiorca: Myszy Taxi", "Kwota: -44,90"),
    ]
    pdf.setFillColor(ink)
    pdf.setFont("Helvetica", 8.5)
    block_y = appendix_y + appendix_h - 23 * mm
    for date_line, kind, detail, amount in blocks:
        for line_text in (date_line, kind, detail, amount):
            pdf.drawString(26 * mm, block_y, line_text)
            block_y -= 3.8 * mm
        block_y -= 1.2 * mm

    pdf.setFillColor(terracotta)
    pdf.setFont("Helvetica-Bold", 9)
    pdf.drawString(22 * mm, 31 * mm, "DEMO ONLY - plik przygotowany do testowania importu PDF w aplikacji Myszy.")
    pdf.setFillColor(ink)
    pdf.setFont("Helvetica", 8)
    pdf.drawString(22 * mm, 24 * mm, "Nie zawiera prawdziwego banku, rachunku, numeru konta ani danych osobowych.")
    pdf.drawRightString(188 * mm, 12 * mm, "1 / 1")
    pdf.save()


if __name__ == "__main__":
    make_pdf()
    print(OUT)
