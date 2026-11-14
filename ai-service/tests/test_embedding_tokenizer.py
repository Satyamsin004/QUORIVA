"""
CS Scholar AI - Embedding Tokenizer & Segmenter Unit Tests
"""
from app.embeddings.tokenizer import EmbeddingTokenizer


def test_tokenizer_token_counting():
    tokenizer = EmbeddingTokenizer()
    text = "Relational database management systems guarantee ACID properties."
    count = tokenizer.count_tokens(text)
    assert count > 5
    assert count < 20


def test_chunk_fits_within_limits_single_segment():
    tokenizer = EmbeddingTokenizer()
    short_text = "Operating systems manage processor scheduling and virtual memory paging."
    segments = tokenizer.segment_text(short_text, max_tokens=100, overlap_tokens=10)
    assert len(segments) == 1
    assert segments[0][0] == short_text
    assert segments[0][1] > 0


def test_oversized_chunk_segmented_without_data_loss():
    tokenizer = EmbeddingTokenizer()
    # Create text with 50 words
    words = [f"word{i}" for i in range(50)]
    long_text = " ".join(words)

    # Segment with max_tokens=15 and overlap=5
    segments = tokenizer.segment_text(long_text, max_tokens=15, overlap_tokens=5)
    assert len(segments) > 1

    # Verify all segments are non-empty and have bounded tokens
    for seg_text, seg_tokens in segments:
        assert len(seg_text.strip()) > 0
        assert seg_tokens <= 15

    # Verify first token appears in first segment, and last token in last segment
    assert "word0" in segments[0][0]
    assert "word49" in segments[-1][0]


def test_empty_text_returns_empty_list():
    tokenizer = EmbeddingTokenizer()
    assert tokenizer.segment_text("") == []
    assert tokenizer.segment_text("   ") == []
