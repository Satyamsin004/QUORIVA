"""
CS Scholar AI - Ingestion Text Normalizer
Preserves code blocks, programming language indentation, and mathematical expressions.
"""
import re
from typing import List


class TextNormalizer:
    """
    Normalizes extracted raw text while strictly preserving:
    1. Code snippets, syntax, and indentation.
    2. Mathematical expressions, KaTeX/LaTeX delimiters, and unicode math symbols.
    """

    # Unicode space character replacements (excluding standard space \u0020)
    UNICODE_SPACES = {
        "\u00A0": " ",  # Non-breaking space
        "\u2000": " ",  # En quad
        "\u2001": " ",  # Em quad
        "\u2002": " ",  # En space
        "\u2003": " ",  # Em space
        "\u2004": " ",  # Three-per-em space
        "\u2005": " ",  # Four-per-em space
        "\u2006": " ",  # Six-per-em space
        "\u2007": " ",  # Figure space
        "\u2008": " ",  # Punctuation space
        "\u2009": " ",  # Thin space
        "\u200A": " ",  # Hair space
        "\u202F": " ",  # Narrow no-break space
        "\u205F": " ",  # Medium mathematical space
        "\u3000": " ",  # Ideographic space
        "\u200B": "",   # Zero-width space
        "\uFEFF": "",   # Zero-width no-break space (BOM)
    }

    # Unicode smart quotation replacements
    SMART_QUOTES = {
        "\u2018": "'",  # Left single quote
        "\u2019": "'",  # Right single quote
        "\u201A": "'",  # Single low-9 quote
        "\u201B": "'",  # Single high-reversed-9 quote
        "\u201C": '"',  # Left double quote
        "\u201D": '"',  # Right double quote
        "\u201E": '"',  # Double low-9 quote
        "\u201F": '"',  # Double high-reversed-9 quote
        "\u00AB": '"',  # Left-pointing double angle quotation mark
        "\u00BB": '"',  # Right-pointing double angle quotation mark
    }

    CODE_FENCE_REGEX = re.compile(r"^\s*```")
    INDENTED_CODE_REGEX = re.compile(r"^( {4,}|\t+)")

    @classmethod
    def normalize_unicode_characters(cls, text: str) -> str:
        """Replace non-standard unicode whitespace and smart punctuation with standard equivalents."""
        if not text:
            return ""

        for char, replacement in cls.UNICODE_SPACES.items():
            if char in text:
                text = text.replace(char, replacement)

        for char, replacement in cls.SMART_QUOTES.items():
            if char in text:
                text = text.replace(char, replacement)

        return text

    @classmethod
    def clean(cls, text: str) -> str:
        """
        Normalize and clean extracted text.
        Maintains code indentation and math formulas while stripping redundant empty lines and trailing spaces.
        """
        if not text:
            return ""

        text = cls.normalize_unicode_characters(text)
        raw_lines = text.splitlines()

        cleaned_lines: List[str] = []
        in_code_block = False

        for line in raw_lines:
            # Check for markdown code fences
            if cls.CODE_FENCE_REGEX.match(line):
                in_code_block = not in_code_block
                cleaned_lines.append(line.rstrip())
                continue

            if in_code_block:
                # Inside explicit code fence: preserve exact indentation, only trim trailing carriage returns / spaces
                cleaned_lines.append(line.rstrip("\r\n"))
            else:
                # Outside code fence: if line starts with 4+ spaces or tabs, preserve indentation (likely code snippet)
                if cls.INDENTED_CODE_REGEX.match(line):
                    cleaned_lines.append(line.rstrip())
                else:
                    # Normal prose line: strip leading/trailing whitespace
                    stripped = line.strip()
                    cleaned_lines.append(stripped)

        # Collapse more than 2 consecutive blank lines into a single blank line (standard markdown paragraph separation)
        result_lines: List[str] = []
        consecutive_empty = 0

        for line in cleaned_lines:
            if not line:
                consecutive_empty += 1
                if consecutive_empty <= 1:
                    result_lines.append("")
            else:
                consecutive_empty = 0
                result_lines.append(line)

        return "\n".join(result_lines).strip()

