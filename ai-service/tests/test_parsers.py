"""
CS Scholar AI - Ingestion Parsers Unit Tests
"""
import os
import tempfile
import pytest
import pymupdf
import docx
import pptx

from app.ingestion.parsers.pdf_parser import PDFParser
from app.ingestion.parsers.docx_parser import DOCXParser
from app.ingestion.parsers.pptx_parser import PPTXParser
from app.ingestion.factory import ParserFactory
from app.ingestion.exceptions import EmptyDocumentError, UnsupportedFormatError, ParserError


@pytest.fixture
def temp_pdf():
    doc = pymupdf.open()
    page1 = doc.new_page()
    page1.insert_text((50, 72), "Chapter 1: Relational Database Models\nRelational algebra defines mathematical operations.")
    page2 = doc.new_page()
    page2.insert_text((50, 72), "Chapter 2: Query Optimization\nCost-based query optimizers evaluate execution trees.")

    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".pdf")
    temp_file_path = temp_file.name
    temp_file.close()

    doc.save(temp_file_path)
    doc.close()

    yield temp_file_path

    if os.path.exists(temp_file_path):
        os.remove(temp_file_path)


@pytest.fixture
def temp_docx():
    doc = docx.Document()
    doc.add_heading("Algorithms and Complexity", level=1)
    doc.add_paragraph("An algorithm is a finite sequence of well-defined instructions.")
    doc.add_heading("Sorting Algorithms", level=2)
    doc.add_paragraph("Quicksort has an average time complexity of O(n log n).")

    # Add a table
    table = doc.add_table(rows=2, cols=2)
    table.rows[0].cells[0].text = "Algorithm"
    table.rows[0].cells[1].text = "Time"
    table.rows[1].cells[0].text = "MergeSort"
    table.rows[1].cells[1].text = "O(n log n)"

    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".docx")
    temp_file_path = temp_file.name
    temp_file.close()

    doc.save(temp_file_path)

    yield temp_file_path

    if os.path.exists(temp_file_path):
        os.remove(temp_file_path)


@pytest.fixture
def temp_pptx():
    prs = pptx.Presentation()
    slide_layout = prs.slide_layouts[0]  # Title slide
    slide1 = prs.slides.add_slide(slide_layout)
    slide1.shapes.title.text = "Computer Architecture"
    slide1.placeholders[1].text = "Pipelining and Superscalar Processors"

    bullet_layout = prs.slide_layouts[1]
    slide2 = prs.slides.add_slide(bullet_layout)
    slide2.shapes.title.text = "RISC vs CISC"
    slide2.placeholders[1].text = "RISC features simple instructions and single-cycle execution."

    temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=".pptx")
    temp_file_path = temp_file.name
    temp_file.close()

    prs.save(temp_file_path)

    yield temp_file_path

    if os.path.exists(temp_file_path):
        os.remove(temp_file_path)


def test_pdf_parser(temp_pdf):
    parser = PDFParser()
    assert parser.can_parse("PDF", temp_pdf) is True

    parsed = parser.parse(temp_pdf, metadata={"title": "DB Systems"})
    assert parsed.total_pages == 2
    assert len(parsed.elements) == 2
    assert parsed.elements[0].page_number == 1
    assert "Relational Database Models" in parsed.elements[0].content
    assert parsed.elements[1].page_number == 2
    assert "Query Optimization" in parsed.elements[1].content


def test_docx_parser(temp_docx):
    parser = DOCXParser()
    assert parser.can_parse("DOCX", temp_docx) is True

    parsed = parser.parse(temp_docx)
    assert len(parsed.elements) >= 3
    content_types = [e.element_type for e in parsed.elements]
    assert "heading" in content_types
    assert "paragraph" in content_types
    assert "table" in content_types

    table_elem = next(e for e in parsed.elements if e.element_type == "table")
    assert "| Algorithm | Time |" in table_elem.content
    assert "| MergeSort | O(n log n) |" in table_elem.content


def test_pptx_parser(temp_pptx):
    parser = PPTXParser()
    assert parser.can_parse("PPTX", temp_pptx) is True

    parsed = parser.parse(temp_pptx)
    assert parsed.total_slides == 2
    assert len(parsed.elements) == 2
    assert parsed.elements[0].slide_number == 1
    assert "Computer Architecture" in parsed.elements[0].content
    assert parsed.elements[1].slide_number == 2
    assert "RISC vs CISC" in parsed.elements[1].content


def test_parser_factory():
    factory = ParserFactory()
    assert isinstance(factory.get_parser("PDF"), PDFParser)
    assert isinstance(factory.get_parser("DOCX"), DOCXParser)
    assert isinstance(factory.get_parser("PPTX"), PPTXParser)
    assert isinstance(factory.get_parser("", "test.pdf"), PDFParser)
    assert isinstance(factory.get_parser("", "test.docx"), DOCXParser)
    assert isinstance(factory.get_parser("", "test.pptx"), PPTXParser)

    with pytest.raises(UnsupportedFormatError):
        factory.get_parser("XLSX", "test.xlsx")


def test_empty_docx_detection():
    doc = docx.Document()
    temp_empty = tempfile.NamedTemporaryFile(delete=False, suffix=".docx")
    temp_empty_path = temp_empty.name
    temp_empty.close()

    doc.save(temp_empty_path)
    try:
        parser = DOCXParser()
        with pytest.raises(EmptyDocumentError):
            parser.parse(temp_empty_path)
    finally:
        if os.path.exists(temp_empty_path):
            os.remove(temp_empty_path)


def test_blank_page_pdf_detection():
    doc = pymupdf.open()
    doc.new_page()  # Blank page with no text
    temp_empty = tempfile.NamedTemporaryFile(delete=False, suffix=".pdf")
    temp_empty_path = temp_empty.name
    temp_empty.close()

    doc.save(temp_empty_path)
    doc.close()

    try:
        parser = PDFParser()
        # Should parse with page_warning placeholder or raise EmptyDocumentError
        parsed = parser.parse(temp_empty_path)
        assert len(parsed.elements) == 1
        assert parsed.elements[0].element_type == "page_warning"
    finally:
        if os.path.exists(temp_empty_path):
            os.remove(temp_empty_path)
