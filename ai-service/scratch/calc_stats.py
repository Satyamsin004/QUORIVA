import hashlib
import json
import os
import platform
import sys
from typing import Dict, Any

import numpy as np
import scipy
import scipy.stats

from app.evaluation.datasets.loader import BenchmarkDataLoader
from app.evaluation.datasets.schemas import DatasetSplit
from app.evaluation.retrieval.ablation import RetrievalAblationRunner
from app.evaluation.metrics.retrieval import compute_ndcg_at_k, compute_mrr, compute_recall_at_k

def compute_sha256(filepath: str) -> str:
    h = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(8192):
            h.update(chunk)
    return h.hexdigest()

def inspect_details():
    loader = BenchmarkDataLoader()
    corpus = loader.load_corpus()
    test_cases = loader.load_split(DatasetSplit.TEST)
    ans_cases = [c for c in test_cases if c.relevant_chunk_ids]

    ablation_runner = RetrievalAblationRunner(corpus)
    ablation_results, stage_predictions = ablation_runner.run_ablation(test_cases)

    dense_preds = stage_predictions["Dense Only"]
    dense_ndcgs = [compute_ndcg_at_k(dense_preds.get(c.case_id, []), set(c.relevant_chunk_ids), 10, c.graded_relevance) for c in ans_cases]

    # Check multi-target queries for Recall@1 vs MRR explanation
    print("Multi-target queries in TEST split:")
    for c in ans_cases:
        rel_count = len(c.relevant_chunk_ids)
        r1 = compute_recall_at_k(dense_preds.get(c.case_id, []), set(c.relevant_chunk_ids), 1)
        mrr = compute_mrr(dense_preds.get(c.case_id, []), set(c.relevant_chunk_ids))
        if rel_count > 1:
            print(f"  [{c.case_id}] {c.subject}: targets={rel_count}, Recall@1={r1:.4f}, MRR={mrr:.4f}")

    print("\nStatistical Analysis (N=20, df=19):")
    stages = [
        ("Keyword-Only", stage_predictions["Keyword Only"]),
        ("Union", stage_predictions["Dense + Keyword (No RRF/Rerank)"]),
        ("RRF (k=60)", stage_predictions["Dense + Keyword + RRF"]),
        ("Cross-Encoder", stage_predictions["Dense + Keyword + RRF + Reranking"])
    ]

    for name, preds in stages:
        scores = [compute_ndcg_at_k(preds.get(c.case_id, []), set(c.relevant_chunk_ids), 10, c.graded_relevance) for c in ans_cases]
        diffs = [s - d for s, d in zip(scores, dense_ndcgs)]
        mean_d = np.mean(diffs)
        std_d = np.std(diffs, ddof=1)
        sem_d = scipy.stats.sem(diffs)
        t_stat, p_val = scipy.stats.ttest_rel(scores, dense_ndcgs)
        ci_unadj = scipy.stats.t.interval(0.95, df=19, loc=mean_d, scale=sem_d)
        # Bonferroni adjusted (alpha = 0.05 / 4 = 0.0125 -> 1 - 0.0125 = 0.9875 confidence)
        ci_bonf = scipy.stats.t.interval(1.0 - 0.0125, df=19, loc=mean_d, scale=sem_d)

        print(f"\nStage: {name}")
        print(f"  Mean diff: {mean_d:+.4f}")
        print(f"  Std diff (sample s_d): {std_d:.4f}")
        print(f"  SEM: {sem_d:.4f}")
        print(f"  t-statistic: {t_stat:+.4f}")
        print(f"  p-value: {p_val:.4f}")
        print(f"  Unadjusted 95% CI: [{ci_unadj[0]:+.4f}, {ci_unadj[1]:+.4f}]")
        print(f"  Bonferroni 98.75% CI: [{ci_bonf[0]:+.4f}, {ci_bonf[1]:+.4f}]")

    # Hashes
    base_dir = os.path.join("ai-service", "app", "evaluation", "datasets")
    files = ["corpus.json", "dev_set.json", "val_set.json", "test_set.json"]
    print("\nDataset Hashes (SHA-256):")
    for f in files:
        fp = os.path.join(base_dir, f)
        if os.path.exists(fp):
            print(f"  {f}: {compute_sha256(fp)}")

    # Runtime info
    print("\nRuntime Info:")
    print(f"  Python: {sys.version.split()[0]}")
    print(f"  SciPy: {scipy.__version__}")
    print(f"  NumPy: {np.__version__}")
    print(f"  Platform: {platform.platform()}")
    print(f"  Processor: {platform.processor()}")
    print(f"  CPU Count: {os.cpu_count()} logical cores")

if __name__ == "__main__":
    inspect_details()

