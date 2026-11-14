"""
CS Scholar AI - Synthetic Evaluation Dataset
Standardized set of Computer Science retrieval queries and simulated ground truth documents.
"""
from dataclasses import dataclass
from typing import List, Set
from uuid import UUID, uuid5, NAMESPACE_DNS


@dataclass
class EvaluationSample:
    query_id: str
    query: str
    topic: str
    relevant_chunk_ids: Set[UUID]


# Deterministic UUID generation for synthetic test corpus
CORPUS_NAMESPACE = uuid5(NAMESPACE_DNS, "corpus.csscholar.test")

SYNTHETIC_CHUNKS = {
    "bst_definition": uuid5(CORPUS_NAMESPACE, "chunk_bst_definition"),
    "bst_operations": uuid5(CORPUS_NAMESPACE, "chunk_bst_operations"),
    "avl_rotations": uuid5(CORPUS_NAMESPACE, "chunk_avl_rotations"),
    "tcp_handshake": uuid5(CORPUS_NAMESPACE, "chunk_tcp_handshake"),
    "tcp_congestion": uuid5(CORPUS_NAMESPACE, "chunk_tcp_congestion"),
    "db_normalization_1nf_2nf": uuid5(CORPUS_NAMESPACE, "chunk_db_normalization_1nf_2nf"),
    "db_normalization_3nf_bcnf": uuid5(CORPUS_NAMESPACE, "chunk_db_normalization_3nf_bcnf"),
    "dijkstra_algorithm": uuid5(CORPUS_NAMESPACE, "chunk_dijkstra_algorithm"),
    "virtual_memory_paging": uuid5(CORPUS_NAMESPACE, "chunk_virtual_memory_paging"),
    "deadlock_conditions": uuid5(CORPUS_NAMESPACE, "chunk_deadlock_conditions"),
}

CS_BENCHMARK_SAMPLES: List[EvaluationSample] = [
    EvaluationSample(
        query_id="q1",
        query="What is the time complexity of searching in a balanced binary search tree?",
        topic="Data Structures",
        relevant_chunk_ids={SYNTHETIC_CHUNKS["bst_definition"], SYNTHETIC_CHUNKS["bst_operations"]}
    ),
    EvaluationSample(
        query_id="q2",
        query="Explain TCP slow start and congestion avoidance mechanisms",
        topic="Computer Networks",
        relevant_chunk_ids={SYNTHETIC_CHUNKS["tcp_congestion"]}
    ),
    EvaluationSample(
        query_id="q3",
        query="Difference between Third Normal Form (3NF) and Boyce-Codd Normal Form (BCNF)",
        topic="Database Systems",
        relevant_chunk_ids={SYNTHETIC_CHUNKS["db_normalization_3nf_bcnf"]}
    ),
    EvaluationSample(
        query_id="q4",
        query="Dijkstra's single source shortest path algorithm using priority queue",
        topic="Algorithms",
        relevant_chunk_ids={SYNTHETIC_CHUNKS["dijkstra_algorithm"]}
    ),
    EvaluationSample(
        query_id="q5",
        query="Virtual memory page replacement algorithms and page fault handling",
        topic="Operating Systems",
        relevant_chunk_ids={SYNTHETIC_CHUNKS["virtual_memory_paging"]}
    ),
]

