"""
CS Scholar AI - Text Normalizer Unit Tests
"""
from app.ingestion.normalizer import TextNormalizer


def test_unicode_normalization():
    # Test non-breaking spaces and smart quotes
    text = "“Smart\u00A0quotes”\u200B and ‘single’ dashes"
    normalized = TextNormalizer.clean(text)
    assert '"Smart quotes"' in normalized
    assert "'single'" in normalized
    assert "\u00A0" not in normalized
    assert "\u200B" not in normalized


def test_code_block_indentation_preserved():
    code_text = (
        "Here is a Python function:\n\n"
        "```python\n"
        "def quicksort(arr):\n"
        "    if len(arr) <= 1:\n"
        "        return arr\n"
        "    pivot = arr[len(arr) // 2]\n"
        "    left = [x for x in arr if x < pivot]\n"
        "    middle = [x for x in arr if x == pivot]\n"
        "    right = [x for x in arr if x > pivot]\n"
        "    return quicksort(left) + middle + quicksort(right)\n"
        "```\n\n"
        "End of algorithm explanation."
    )
    cleaned = TextNormalizer.clean(code_text)
    assert "    if len(arr) <= 1:" in cleaned
    assert "        return arr" in cleaned
    assert "    pivot = arr[len(arr) // 2]" in cleaned


def test_math_expressions_preserved():
    math_text = (
        "Consider the recurrence relation:\n\n"
        "$$T(n) = 2T(n/2) + O(n)$$\n\n"
        "By the Master Theorem, where $a=2$, $b=2$, and $f(n) = O(n)$,\n"
        "we have $\\log_b(a) = \\log_2(2) = 1$.\n"
        "Therefore, $\\sum_{i=1}^n i = \\frac{n(n+1)}{2}$ and $\\int_0^1 x^2 dx = \\frac{1}{3}$."
    )
    cleaned = TextNormalizer.clean(math_text)
    assert "$$T(n) = 2T(n/2) + O(n)$$" in cleaned
    assert "$\\log_b(a) = \\log_2(2) = 1$" in cleaned
    assert "$\\sum_{i=1}^n i = \\frac{n(n+1)}{2}$" in cleaned
    assert "$\\int_0^1 x^2 dx = \\frac{1}{3}$" in cleaned


def test_excessive_newlines_collapsed():
    messy_text = "Heading\n\n\n\n\nParagraph 1\n\n\n\n\n\n\nParagraph 2"
    cleaned = TextNormalizer.clean(messy_text)
    assert "\n\n\n" not in cleaned
    assert "Heading\n\nParagraph 1\n\nParagraph 2" == cleaned

