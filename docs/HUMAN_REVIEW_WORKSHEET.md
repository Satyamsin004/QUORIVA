# CS Scholar AI — Academic RAG Double-Blind Human Evaluation Worksheet

## Evaluation Protocol & Instructions
This worksheet supports independent expert evaluation of AI Academic Tutor answers.
All model metadata, pipeline stage tags, and latency indicators have been omitted to preserve double-blind evaluation integrity.

### 6-Dimensional Rubric (Score 1 to 5)
1. **Factual Correctness (1-5)**: Is every technical statement rigorously correct according to standard CS textbooks?
2. **Citation Accuracy (1-5)**: Do the cited source tags `[S1]`, `[S2]` directly support the claims they are attached to?
3. **Completeness (1-5)**: Does the answer address all parts of the question, or omit critical conditions/steps?
4. **Clarity & Tone (1-5)**: Is the response lucid, structured, grammatically sound, and pedagogically constructive?
5. **Hallucination Freedom (1-5)**: 5 = zero unsupported or fabricated claims; 1 = multiple fabricated concepts.
6. **Pedagogical Suitability (1-5)**: Would an undergraduate or graduate CS student learn the concept accurately from this answer?

### Categorical Classification Labels
- `[ ] CORRECT`: Fully accurate, complete, and verified against sources.
- `[ ] PARTIALLY_CORRECT`: Accurate core answer with minor omissions.
- `[ ] INCORRECT`: Contains factual or algorithmic errors.
- `[ ] UNSUPPORTED`: Unsubstantiated claims absent from provided documents.
- `[ ] INCORRECT_CITATION`: Wrong source tag or citation points to unrelated section.
- `[ ] INCOMPLETE`: Missed crucial aspects of the question prompt.
- `[ ] CORRECT_ABSTENTION`: Appropriately refused to hallucinate on unanswerable query.
- `[ ] INCORRECT_ABSTENTION`: Refused to answer despite clear evidence in context.

---

### Case 01: [eval-test-ans-01] Subject: Programming Fundamentals
**Question:** What specific information is stored within an activation record on the call stack during a function call?

**Expected Ground Truth:** An activation record (stack frame) on the call stack stores local variables, function parameters, and the return address to resume after completion.

**Generated Tutor Answer:**
> An activation record (stack frame) on the call stack stores local variables, function parameters, and the return address to resume after completion [S2].

**Context Citations:** [S2] Call Stack and Recursion Fundamentals

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 02: [eval-test-ans-02] Subject: Data Structures and Algorithms
**Question:** Why does Dijkstra's algorithm require non-negative edge weights to guarantee finding the shortest path?

**Expected Ground Truth:** Dijkstra's greedy choice assumes that once a node is marked visited with minimum tentative distance, no subsequent path can reduce that distance, an invariant broken by negative edge weights.

**Generated Tutor Answer:**
> Dijkstra's greedy choice assumes that once a node is marked visited with minimum tentative distance, no subsequent path can reduce that distance, an invariant broken by negative edge weights [S1].

**Context Citations:** [S1] Dijkstra's Shortest Path Algorithm

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 03: [eval-test-ans-03] Subject: Database Management Systems
**Question:** Why do B+ trees link all leaf nodes in a doubly-linked list?

**Expected Ground Truth:** Linking leaf nodes in a doubly-linked list enables efficient sequential range scans across records without traversing upper tree levels.

**Generated Tutor Answer:**
> Linking leaf nodes in a doubly-linked list enables efficient sequential range scans across records without traversing upper tree levels [S1].

**Context Citations:** [S1] B+ Tree Index Structures in Relational Engines

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 04: [eval-test-ans-04] Subject: Operating Systems
**Question:** Which Coffman condition is eliminated by enforcing a strict global resource ordering?

**Expected Ground Truth:** Enforcing a strict global resource ordering eliminates the Circular Wait condition, as processes must request resources in strictly ascending numerical order.

**Generated Tutor Answer:**
> Enforcing a strict global resource ordering eliminates the Circular Wait condition, as processes must request resources in strictly ascending numerical order [S1].

**Context Citations:** [S1] Coffman Deadlock Conditions and Prevention

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 05: [eval-test-ans-05] Subject: Computer Networks
**Question:** What default transport port and protocol does DNS use for queries under 512 bytes?

**Expected Ground Truth:** DNS predominantly uses UDP port 53 for standard queries under 512 bytes to minimize latency overhead.

**Generated Tutor Answer:**
> DNS predominantly uses UDP port 53 for standard queries under 512 bytes to minimize latency overhead [S1].

**Context Citations:** [S1] DNS Hierarchical Resolution

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 06: [eval-test-ans-06] Subject: Cybersecurity
**Question:** What role does the database query compiler play in making prepared statements immune to SQL injection?

**Expected Ground Truth:** Prepared statements cause the database engine to parse and compile the SQL query template before binding untrusted parameter values as literal data.

**Generated Tutor Answer:**
> Prepared statements cause the database engine to parse and compile the SQL query template before binding untrusted parameter values as literal data [S1].

**Context Citations:** [S1] SQL Injection Mechanics and Defense

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 07: [eval-test-ans-07] Subject: Software Engineering
**Question:** Explain the Open/Closed Principle and describe how polymorphism enables extension without modification.

**Expected Ground Truth:** The Open/Closed Principle states software entities should be open for extension but closed for modification, achieved through polymorphic interfaces where new behaviors are added via new subclasses.

**Generated Tutor Answer:**
> The Open/Closed Principle states software entities should be open for extension but closed for modification, achieved through polymorphic interfaces where new behaviors are added via new subclasses [S1].

**Context Citations:** [S1] SOLID Design Principles

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 08: [eval-test-ans-08] Subject: Artificial Intelligence and Machine Learning
**Question:** How does the chain rule enable backward propagation of errors from loss to input layer?

**Expected Ground Truth:** The calculus chain rule allows computing composite derivatives by multiplying intermediate partial derivatives backwards through successive network layers.

**Generated Tutor Answer:**
> The calculus chain rule allows computing composite derivatives by multiplying intermediate partial derivatives backwards through successive network layers [S1].

**Context Citations:** [S1] Backpropagation and Gradient Descent

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 09: [eval-test-ans-09] Subject: Cloud and Distributed Systems
**Question:** How do randomized election timeouts in Raft prevent split votes among candidate nodes?

**Expected Ground Truth:** Raft randomizes follower election timeouts across an interval (e.g. 150-300ms) so one follower times out first, becoming a Candidate and winning a majority before competitors.

**Generated Tutor Answer:**
> Raft randomizes follower election timeouts across an interval (e.g [S1]. 150-300ms) so one follower times out first, becoming a Candidate and winning a majority before competitors [S1].

**Context Citations:** [S1] The Raft Distributed Consensus Protocol

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 10: [eval-test-ans-10] Subject: Computer Architecture
**Question:** How does data forwarding (bypassing) resolve Read-After-Write (RAW) data hazards in pipelined processors?

**Expected Ground Truth:** Data forwarding routes calculated results directly from the Execution (EX) or Memory (MEM) pipeline stages to the Decode/Execute stage of subsequent instructions without waiting for register write-back.

**Generated Tutor Answer:**
> Data forwarding routes calculated results directly from the Execution (EX) or Memory (MEM) pipeline stages to the Decode/Execute stage of subsequent instructions without waiting for register write-back [S1].

**Context Citations:** [S1] Instruction Pipelining and Hazard Resolution

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 11: [eval-test-ans-11] Subject: Compiler Design
**Question:** How does an LR(1) parser use a lookahead token to resolve shift-reduce conflicts?

**Expected Ground Truth:** An LR(1) parser inspects 1 lookahead token to determine whether to shift the token onto the parse stack or reduce a completed production rule based on canonical LR items.

**Generated Tutor Answer:**
> An LR(1) parser inspects 1 lookahead token to determine whether to shift the token onto the parse stack or reduce a completed production rule based on canonical LR items [S1].

**Context Citations:** [S1] Context-Free Grammars and LL(1) vs LR(1) Parsing

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 12: [eval-test-ans-12] Subject: Theory of Computation
**Question:** What does Rice's Theorem imply about undecidability of program behavior from Turing machines?

**Expected Ground Truth:** Rice's Theorem proves that any non-trivial semantic property of partial recursive functions computed by Turing machines is undecidable.

**Generated Tutor Answer:**
> Rice's Theorem proves that any non-trivial semantic property of partial recursive functions computed by Turing machines is undecidable [S1].

**Context Citations:** [S1] Turing Machines and the Halting Problem

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 13: [eval-test-ans-13] Subject: Computer Networks
**Question:** How does the TCP 3-way handshake relate to latency overhead in CORS preflight OPTIONS requests?

**Expected Ground Truth:** Non-simple cross-origin web requests require an automated preflight HTTP OPTIONS round trip over TCP, incurring connection handshake round trips (SYN, SYN-ACK, ACK) and CORS authorization overhead before data transfer.

**Generated Tutor Answer:**
> Non-simple cross-origin web requests require an automated preflight HTTP OPTIONS round trip over TCP, incurring connection handshake round trips (SYN, SYN-ACK, ACK) and CORS authorization overhead before data transfer [S1].

**Context Citations:** [S1] Same-Origin Policy and CORS, [S3] TCP 3-Way Handshake and Connection Termination

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 14: [eval-test-ans-14] Subject: Database Management Systems
**Question:** Compare how B+ tree page sizes are aligned with operating system virtual memory page frames to minimize I/O overhead.

**Expected Ground Truth:** Database B+ tree nodes are sized to match or multiply OS virtual memory page sizes (e.g. 4KB or 8KB), ensuring that fetching an index node corresponds exactly to virtual page translations without wasted I/O.

**Generated Tutor Answer:**
> Database B+ tree nodes are sized to match or multiply OS virtual memory page sizes (e.g [S1]. 4KB or 8KB), ensuring that fetching an index node corresponds exactly to virtual page translations without wasted I/O [S1].

**Context Citations:** [S1] Paging and Translation Lookaside Buffers (TLB), [S6] B+ Tree Index Structures in Relational Engines

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 15: [eval-test-ans-15] Subject: Computer Architecture
**Question:** How does hardware cache line invalidation in the MESI protocol impact thread synchronization and deadlock detection in multicore systems?

**Expected Ground Truth:** When multiple cores synchronize shared resources (e.g. mutexes), writes cause MESI cache lines to invalidate in peer caches, while improper resource lock ordering across threads can satisfy the Coffman circular wait condition leading to deadlock.

**Generated Tutor Answer:**
> When multiple cores synchronize shared resources (e.g [S1]. mutexes), writes cause MESI cache lines to invalidate in peer caches, while improper resource lock ordering across threads can satisfy the Coffman circular wait condition leading to deadlock [S1].

**Context Citations:** [S1] Cache Coherence and the MESI Protocol, [S5] Coffman Deadlock Conditions and Prevention

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 16: [eval-test-ans-16] Subject: Compiler Design
**Question:** Compare LL(1) top-down recursive descent with LR(1) bottom-up shift-reduce parsing in language grammar coverage.

**Expected Ground Truth:** LL(1) top-down parsers require non-left-recursive grammars and predict productions from 1 token lookahead, whereas LR(1) bottom-up parsers handle left-recursive grammars and represent a strictly larger grammar class.

**Generated Tutor Answer:**
> LL(1) top-down parsers require non-left-recursive grammars and predict productions from 1 token lookahead, whereas LR(1) bottom-up parsers handle left-recursive grammars and represent a strictly larger grammar class [S1].

**Context Citations:** [S1] Context-Free Grammars and LL(1) vs LR(1) Parsing

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 17: [eval-test-ans-17] Subject: Cybersecurity
**Question:** Compare the mathematical trapdoor functions underlying RSA factorization versus Elliptic Curve discrete logarithms.

**Expected Ground Truth:** RSA relies on the difficulty of integer factorization of semiprimes (n = p * q), while ECC relies on the discrete logarithm problem over points on an elliptic curve, yielding equivalent security with smaller keys.

**Generated Tutor Answer:**
> RSA relies on the difficulty of integer factorization of semiprimes (n = p * q), while ECC relies on the discrete logarithm problem over points on an elliptic curve, yielding equivalent security with smaller keys [S1].

**Context Citations:** [S1] Public Key Cryptography and RSA

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 18: [eval-test-ans-18] Subject: Web Development
**Question:** Compare microtask and macrotask scheduling priority in the browser event loop.

**Expected Ground Truth:** Microtasks (Promise reactions) have higher execution priority and are completely drained whenever the call stack empties, before the browser renders and before the next macrotask (timers, I/O) executes.

**Generated Tutor Answer:**
> Microtasks (Promise reactions) have higher execution priority and are completely drained whenever the call stack empties, before the browser renders and before the next macrotask (timers, I/O) executes [S1].

**Context Citations:** [S1] Browser DOM and JavaScript Event Loop

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 19: [eval-test-ans-19] Subject: Cloud and Distributed Systems
**Question:** Explain the CAP theorem partition tolerance trade-off and describe the Paxos multi-decree protocol phases.

**Expected Ground Truth:** The documents explain the CAP theorem trade-off between consistency and availability under partition [S1], but do not contain information regarding multi-decree Paxos protocol phases.

**Generated Tutor Answer:**
> The documents explain the CAP theorem trade-off between consistency and availability under partition [S1], but do not contain information regarding multi-decree Paxos protocol phases.

**Context Citations:** [S1] Brewer's CAP Theorem and PACELC

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 20: [eval-test-ans-20] Subject: Data Structures and Algorithms
**Question:** Explain BST worst-case search complexity and describe how splay tree zig-zig rotations maintain amortized bounds.

**Expected Ground Truth:** The documents explain that unbalanced BST search degrades to O(N) worst-case time [S1], but do not contain information on splay tree zig-zig rotations.

**Generated Tutor Answer:**
> The documents explain that unbalanced BST search degrades to O(N) worst-case time [S1], but do not contain information on splay tree zig-zig rotations.

**Context Citations:** [S1] Binary Search Tree Invariants and Traversal

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 21: [eval-test-unans-01] Subject: Artificial Intelligence and Machine Learning
**Question:** What is the decoherence rate of superconducting transmon qubits under cryogenic dilution?

**Expected Ground Truth:** The provided documents do not contain information regarding superconducting transmon qubits or quantum decoherence rates.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 22: [eval-test-unans-02] Subject: Database Management Systems
**Question:** How does Apache Cassandra use hinted handoffs and read repair during partition healing?

**Expected Ground Truth:** The provided documents do not contain information regarding Apache Cassandra hinted handoffs or read repair mechanisms.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 23: [eval-test-unans-03] Subject: Operating Systems
**Question:** Explain the eBPF verifier bytecode static safety checks for kernel ring buffers.

**Expected Ground Truth:** The provided documents do not contain information regarding Linux eBPF bytecode verification or kernel ring buffers.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 24: [eval-test-unans-04] Subject: Computer Networks
**Question:** How does BGP EVPN use VXLAN encapsulation for data center network virtualization?

**Expected Ground Truth:** The provided documents do not contain information regarding BGP EVPN or VXLAN network encapsulation.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 25: [eval-test-unans-05] Subject: Artificial Intelligence and Machine Learning
**Question:** Explain the mathematical derivation of AdamW decoupled weight decay versus L2 regularization.

**Expected Ground Truth:** The provided documents do not contain information regarding AdamW decoupled weight decay or its mathematical derivation.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 26: [eval-test-unans-06] Subject: Compiler Design
**Question:** How does the GraalVM polyglot engine compile JavaScript to native machine code via Truffle AST specialization?

**Expected Ground Truth:** The provided documents do not contain information regarding GraalVM, Truffle AST specialization, or polyglot runtime compilation.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 27: [eval-test-unans-07] Subject: Cybersecurity
**Question:** Explain zero-knowledge succinct non-interactive arguments of knowledge (zk-SNARKs) quadratic arithmetic programs.

**Expected Ground Truth:** The provided documents do not contain information regarding zk-SNARKs or quadratic arithmetic program transformations.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 28: [eval-test-unans-08] Subject: Web Development
**Question:** How does WebAssembly linear memory interact with JavaScript SharedArrayBuffer under the Atomics memory model?

**Expected Ground Truth:** The provided documents do not contain information regarding WebAssembly linear memory, SharedArrayBuffer, or JavaScript Atomics.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 29: [eval-test-distract-01] Subject: Data Structures and Algorithms
**Question:** How does Dijkstra's algorithm handle negative weight cycles by updating the predecessor array?

**Expected Ground Truth:** The provided documents state that Dijkstra's algorithm strictly requires non-negative edge weights and fails on negative weights; handling negative weight cycles requires Bellman-Ford, which is not in the corpus.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 30: [eval-test-distract-02] Subject: Computer Architecture
**Question:** Explain how branch prediction in instruction pipelining uses the Spectre speculative execution gadget to leak kernel memory.

**Expected Ground Truth:** The provided documents explain pipelining hazards and branch prediction, but do not contain information on Spectre speculative execution exploits.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 31: [eval-test-distract-03] Subject: Operating Systems
**Question:** How does the Linux CFS scheduler calculate real-time priority task preemptions under the SCHED_FIFO scheduling class?

**Expected Ground Truth:** The provided documents explain CFS virtual runtime for standard tasks, but do not contain information on the SCHED_FIFO real-time scheduling class.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 32: [eval-test-distract-04] Subject: Computer Networks
**Question:** Explain how TCP Slow Start calculates the initial congestion window using Cubic polynomial curve coefficients.

**Expected Ground Truth:** The provided documents explain standard TCP Slow Start and AIMD, but do not contain information regarding TCP Cubic polynomial coefficients.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 33: [eval-test-conflict-01] Subject: Cloud and Distributed Systems
**Question:** Can a network partition in a distributed system guarantee both strict linearizable consistency and 100% availability?

**Expected Ground Truth:** Based on Brewer's CAP theorem in the provided documents, a distributed system under network partition mathematically cannot guarantee both consistency and availability simultaneously.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---

### Case 34: [eval-test-conflict-02] Subject: Theory of Computation
**Question:** Has an efficient polynomial-time algorithm O(n^2) been proven for solving all NP-complete problems?

**Expected Ground Truth:** The provided documents state that whether P = NP remains the foremost unsolved problem in theoretical computer science, and no polynomial-time algorithm is known for NP-complete problems.

**Generated Tutor Answer:**
> Based on the provided documents, there is insufficient evidence to answer this question.

**Context Citations:** None (Abstained or no citations)

**Categorical Verdict:**
- [ ] CORRECT
- [ ] PARTIALLY_CORRECT
- [ ] INCORRECT
- [ ] UNSUPPORTED
- [ ] INCORRECT_CITATION
- [ ] INCOMPLETE
- [ ] CORRECT_ABSTENTION
- [ ] INCORRECT_ABSTENTION

**Dimensional Rubric Scores (1 to 5):**
- Factual Correctness (1-5): [ ]
- Citation Accuracy (1-5): [ ]
- Completeness (1-5): [ ]
- Clarity & Tone (1-5): [ ]
- Hallucination Freedom (1-5): [ ]
- Pedagogical Suitability (1-5): [ ]

**Reviewer Notes / Evidence Analysis:**
____________________________________________________________________________

---
