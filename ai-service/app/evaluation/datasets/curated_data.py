"""
CS Scholar AI - Phase 12.1 Curated Benchmark Dataset Generator
Generates canonical, legally safe academic CS evaluation corpus and partitioned splits
(DEV: 26, VAL: 20, TEST: 34 - Total: 80 cases) across all 13 Computer Science disciplines.
"""
import json
import os
from typing import List, Dict, Tuple
from uuid import UUID, uuid5, NAMESPACE_DNS
from app.evaluation.datasets.schemas import (
    CorpusChunk,
    EvaluationCase,
    QuestionCategory,
    DifficultyLevel,
    ExpectedAnswerability,
    DatasetSplit
)

CORPUS_NAMESPACE = uuid5(NAMESPACE_DNS, "benchmark.csscholar.evaluation")

def create_deterministic_uuid(key: str) -> UUID:
    return uuid5(CORPUS_NAMESPACE, key)

# 1. Authoritative Academic Corpus Chunks across 13 Disciplines
RAW_CORPUS = [
    # 1. Programming Fundamentals
    {
        "id_key": "pf_recursion_stack",
        "doc_key": "doc_pf_fundamentals",
        "subject": "Programming Fundamentals",
        "topic": "Recursion and Memory",
        "title": "Call Stack and Recursion Fundamentals",
        "content": (
            "In programming languages, recursion relies on the execution call stack. Each recursive call "
            "allocates a new stack frame containing the function's local variables, parameters, and return address. "
            "A base case is required to terminate recursive descent; without a valid base case, repeated stack frame "
            "allocations lead to stack overflow errors. The call stack operates in Last-In First-Out (LIFO) order."
        ),
        "page_number": 12,
        "section_heading": "Stack Allocation in Recursive Invocations"
    },
    {
        "id_key": "pf_heap_vs_stack",
        "doc_key": "doc_pf_fundamentals",
        "subject": "Programming Fundamentals",
        "topic": "Memory Management",
        "title": "Stack vs Heap Allocation",
        "content": (
            "Memory in high-level languages is partitioned primarily into the stack and the heap. Stack allocation "
            "is automatic and deterministic with very low overhead, storing local primitives and function call frames. "
            "Stack variables are scoped to function execution lifetimes. In contrast, heap allocation is dynamic and managed "
            "at runtime, storing objects whose lifetimes extend beyond individual function activations. Heap memory requires "
            "explicit manual deallocation or automatic garbage collection, and is subject to memory fragmentation."
        ),
        "page_number": 18,
        "section_heading": "Stack and Heap Memory Dynamics"
    },
    {
        "id_key": "pf_variable_scope",
        "doc_key": "doc_pf_fundamentals",
        "subject": "Programming Fundamentals",
        "topic": "Scope and Lifetime",
        "title": "Lexical Scope and Variable Lifetime",
        "content": (
            "Lexical (static) scoping binds identifiers based on the source code block structure where functions and "
            "variables are declared. In a lexically scoped language, nested inner blocks have access to variables declared "
            "in enclosing outer scopes. Closures capture the lexical environment in which they were created, retaining references "
            "to outer variables even after the enclosing outer function has returned. In contrast, dynamic scoping resolves "
            "variable references based on the runtime call stack hierarchy."
        ),
        "page_number": 24,
        "section_heading": "Lexical Scoping and Closures"
    },

    # 2. Data Structures and Algorithms
    {
        "id_key": "dsa_bst_properties",
        "doc_key": "doc_dsa_trees",
        "subject": "Data Structures and Algorithms",
        "topic": "Binary Search Trees",
        "title": "Binary Search Tree Invariants and Traversal",
        "content": (
            "A Binary Search Tree (BST) maintains the invariant that for any node x, all keys in x's left subtree are strictly "
            "less than x.key, and all keys in x's right subtree are strictly greater than x.key. In-order traversal visits nodes "
            "in monotonically non-decreasing order. Search, insertion, and deletion exhibit O(h) time complexity, where h is tree "
            "height. In balanced trees h = O(log N), whereas in the worst case (e.g. inserting sorted elements) the tree degenerates "
            "into a singly-linked list with height h = O(N)."
        ),
        "page_number": 45,
        "section_heading": "BST Properties and Complexity"
    },
    {
        "id_key": "dsa_avl_rotations",
        "doc_key": "doc_dsa_trees",
        "subject": "Data Structures and Algorithms",
        "topic": "AVL Trees",
        "title": "AVL Self-Balancing and Rotations",
        "content": (
            "An AVL tree is a strictly self-balancing binary search tree where the balance factor of every node—defined as "
            "height(left_subtree) - height(right_subtree)—must be in {-1, 0, +1}. When an insertion or deletion violates this "
            "balance factor invariant, tree rotations restore balance in O(1) time per rotation. The four rotation cases are "
            "Left-Left (single right rotation), Right-Right (single left rotation), Left-Right (left rotation on child followed "
            "by right rotation on parent), and Right-Left (right rotation on child followed by left rotation on parent). "
            "AVL trees guarantee O(log N) worst-case time for search, insert, and delete operations."
        ),
        "page_number": 52,
        "section_heading": "AVL Tree Rotations and Balance Factors"
    },
    {
        "id_key": "dsa_dijkstra_algorithm",
        "doc_key": "doc_dsa_graphs",
        "subject": "Data Structures and Algorithms",
        "topic": "Graph Algorithms",
        "title": "Dijkstra's Shortest Path Algorithm",
        "content": (
            "Dijkstra's algorithm computes the single-source shortest path on a weighted directed or undirected graph with "
            "strictly non-negative edge weights. Using a min-priority queue (such as a Fibonacci heap or binary heap), the algorithm "
            "greedily selects the unvisited vertex u with minimum tentative distance dist[u], marks u as visited, and relaxes all "
            "outgoing edges (u, v): if dist[u] + weight(u, v) < dist[v], then dist[v] is updated. With a binary heap, the time "
            "complexity is O((V + E) log V). The algorithm fails on graphs with negative edge weights, which require Bellman-Ford."
        ),
        "page_number": 88,
        "section_heading": "Dijkstra Algorithm Mechanics and Complexity"
    },

    # 3. Database Management Systems
    {
        "id_key": "db_acid_properties",
        "doc_key": "doc_db_transactions",
        "subject": "Database Management Systems",
        "topic": "Transaction Processing",
        "title": "ACID Properties and Concurrency Isolation",
        "content": (
            "Database transactions guarantee ACID properties: Atomicity (all operations commit or all roll back), "
            "Consistency (transactions preserve database integrity constraints), Isolation (concurrent executions behave as if "
            "executed serially), and Durability (committed changes survive system crashes). SQL-92 defines four isolation levels "
            "to balance concurrency against anomalies: Read Uncommitted (permits dirty reads), Read Committed (prevents dirty reads), "
            "Repeatable Read (prevents non-repeatable reads), and Serializable (prevents phantom reads via predicate locking or 2PL)."
        ),
        "page_number": 105,
        "section_heading": "ACID Transactions and SQL Isolation Levels"
    },
    {
        "id_key": "db_normalization_bcnf",
        "doc_key": "doc_db_design",
        "subject": "Database Management Systems",
        "topic": "Relational Normalization",
        "title": "Third Normal Form vs Boyce-Codd Normal Form",
        "content": (
            "Relational schema normalization eliminates data redundancy and update anomalies. A relation R is in Third Normal Form (3NF) "
            "if for every non-trivial functional dependency X -> A, either X is a superkey of R or A is a prime attribute (part of any candidate key). "
            "Boyce-Codd Normal Form (BCNF) strictly strengthens 3NF: for every non-trivial functional dependency X -> A, X MUST be a superkey of R. "
            "While any relation can be decomposed into 3NF while preserving dependencies and achieving lossless join, BCNF decomposition guarantees "
            "lossless join but cannot always preserve functional dependencies."
        ),
        "page_number": 64,
        "section_heading": "3NF and BCNF Functional Dependencies"
    },
    {
        "id_key": "db_btree_indexing",
        "doc_key": "doc_db_storage",
        "subject": "Database Management Systems",
        "topic": "Indexing and Storage",
        "title": "B+ Tree Index Structures in Relational Engines",
        "content": (
            "B+ trees are self-balancing multi-way search trees optimized for block-based disk storage and database indexing. "
            "Internal nodes store only routing keys and child pointers, maximizing fan-out and minimizing tree height (typically 3 to 4 levels). "
            "All data records or row pointers reside strictly in leaf nodes. All leaf nodes are linked in a contiguous doubly-linked list, "
            "enabling efficient O(log N) point lookups as well as sequential range scans without re-traversing upper tree levels. "
            "Node splits and merges occur when occupancy falls below 50% capacity."
        ),
        "page_number": 142,
        "section_heading": "B+ Tree Architecture and Disk Fan-Out"
    },

    # 4. Operating Systems
    {
        "id_key": "os_virtual_memory",
        "doc_key": "doc_os_memory",
        "subject": "Operating Systems",
        "topic": "Virtual Memory",
        "title": "Paging and Translation Lookaside Buffers (TLB)",
        "content": (
            "Virtual memory provides process address space isolation and abstraction of physical RAM. Memory is divided into fixed-size "
            "virtual pages mapped to physical frames via multi-level page tables. The Memory Management Unit (MMU) translates Virtual Page Numbers (VPN) "
            "to Physical Frame Numbers (PFN). To mitigate memory translation latency, the Translation Lookaside Buffer (TLB) acts as a high-speed "
            "hardware cache of recent VPN-to-PFN mappings. On a TLB miss, page table walks traverse hierarchical directories (e.g. PML4). "
            "When a page is not resident in physical RAM, a page fault exception triggers the OS kernel to load the page from swap."
        ),
        "page_number": 80,
        "section_heading": "Virtual Memory Address Translation and TLB"
    },
    {
        "id_key": "os_deadlock_coffman",
        "doc_key": "doc_os_concurrency",
        "subject": "Operating Systems",
        "topic": "Concurrency and Deadlock",
        "title": "Coffman Deadlock Conditions and Prevention",
        "content": (
            "A deadlock occurs when a set of concurrent processes are permanently blocked because each holds resources while waiting for resources "
            "held by others. Edward Coffman established the four necessary and sufficient conditions for deadlock: "
            "1. Mutual Exclusion (resources cannot be shared simultaneously). "
            "2. Hold and Wait (processes holding allocated resources can request additional ones). "
            "3. No Preemption (resources cannot be forcibly taken from a process). "
            "4. Circular Wait (a closed chain of processes exists where each waits for a resource held by the next). "
            "Deadlock prevention invalidates at least one condition, such as enforcing strict global resource ordering to eliminate circular wait."
        ),
        "page_number": 115,
        "section_heading": "Coffman Conditions and Deadlock Prevention"
    },
    {
        "id_key": "os_process_scheduling",
        "doc_key": "doc_os_cpu",
        "subject": "Operating Systems",
        "topic": "CPU Scheduling",
        "title": "CPU Scheduling Algorithms: Round Robin and CFS",
        "content": (
            "CPU schedulers allocate processor execution time among runnable threads. Round Robin (RR) assigns a fixed time quantum per thread; "
            "if quantum expires before completion, the thread is preempted and moved to the tail of the ready queue. The Linux Completely Fair Scheduler (CFS) "
            "models an ideal multi-tasking CPU on hardware using red-black trees. CFS tracks each task's virtual runtime (vruntime), representing elapsed "
            "execution time normalized by thread static priority (nice value). CFS always schedules the runnable entity with the smallest vruntime."
        ),
        "page_number": 95,
        "section_heading": "CPU Scheduling Policies: Round Robin and Linux CFS"
    },

    # 5. Computer Networks
    {
        "id_key": "net_tcp_handshake",
        "doc_key": "doc_net_transport",
        "subject": "Computer Networks",
        "topic": "Transport Layer",
        "title": "TCP 3-Way Handshake and Connection Termination",
        "content": (
            "The Transmission Control Protocol (TCP) establishes reliable, full-duplex, connection-oriented streams using a 3-way handshake: "
            "1. Client sends SYN (synchronize) with an initial sequence number (ISN_c). "
            "2. Server responds with SYN-ACK, acknowledging ISN_c + 1 and providing its own initial sequence number (ISN_s). "
            "3. Client replies with ACK acknowledging ISN_s + 1, completing connection establishment. "
            "Connection teardown uses a 4-way FIN/ACK handshake. The active closer enters the TIME_WAIT state for 2*MSL (Maximum Segment Lifetime) "
            "to guarantee that late arriving packets from the terminated connection do not corrupt subsequent incarnations."
        ),
        "page_number": 32,
        "section_heading": "TCP Connection Establishment and TIME_WAIT"
    },
    {
        "id_key": "net_tcp_congestion",
        "doc_key": "doc_net_transport",
        "subject": "Computer Networks",
        "topic": "Congestion Control",
        "title": "TCP Congestion Control: AIMD and Slow Start",
        "content": (
            "TCP congestion control prevents network collapse by dynamically adjusting the sender's congestion window (cwnd). "
            "Slow Start exponentially doubles the congestion window (cwnd) every Round Trip Time (RTT) until reaching the slow start threshold (ssthresh). "
            "Congestion Avoidance increases cwnd linearly by 1 Maximum Segment Size (MSS) per RTT (Additive Increase, Multiplicative Decrease - AIMD). "
            "Fast Retransmit retransmits lost packets upon receiving 3 duplicate ACKs without waiting for retransmission timer timeout, followed by Fast Recovery."
        ),
        "page_number": 40,
        "section_heading": "TCP Congestion Control: AIMD and Slow Start"
    },
    {
        "id_key": "net_dns_resolution",
        "doc_key": "doc_net_application",
        "subject": "Computer Networks",
        "topic": "Application Layer",
        "title": "DNS Hierarchical Resolution",
        "content": (
            "The Domain Name System (DNS) translates human-readable hostnames into IP addresses via a hierarchical distributed database. "
            "When a recursive resolver handles a query, it iterates through Root DNS servers, Top-Level Domain (TLD) servers (e.g. .edu, .com), "
            "and authoritative name servers. Responses are cached according to Time-To-Live (TTL) values. DNS queries predominantly use UDP port 53 "
            "for minimal latency overhead, falling back to TCP when responses exceed 512 bytes or during zone transfers."
        ),
        "page_number": 75,
        "section_heading": "Domain Name System Resolution"
    },

    # 6. Software Engineering
    {
        "id_key": "se_solid_principles",
        "doc_key": "doc_se_design",
        "subject": "Software Engineering",
        "topic": "Object-Oriented Design",
        "title": "SOLID Design Principles",
        "content": (
            "The SOLID design principles promote maintainable object-oriented software: "
            "1. Single Responsibility Principle (SRP): A class should have only one reason to change. "
            "2. Open/Closed Principle (OCP): Software entities should be open for extension, but closed for modification. "
            "3. Liskov Substitution Principle (LSP): Subtypes must be substitutable for their base types without altering program correctness. "
            "4. Interface Segregation Principle (ISP): Clients should not be forced to depend on interfaces they do not use. "
            "5. Dependency Inversion Principle (DIP): High-level modules should depend on abstractions, not concrete implementations."
        ),
        "page_number": 15,
        "section_heading": "SOLID Principles in Object-Oriented Architecture"
    },
    {
        "id_key": "se_ci_cd_pipeline",
        "doc_key": "doc_se_devops",
        "subject": "Software Engineering",
        "topic": "DevOps and CI/CD",
        "title": "Continuous Integration and Continuous Delivery",
        "content": (
            "Continuous Integration (CI) is the practice of automating the integration and verification of code changes from multiple contributors "
            "into a shared mainline repository multiple times a day. Automated builds, linting, and unit test suites execute on every commit. "
            "Continuous Delivery (CD) extends CI by ensuring that verified code can be reliably released to production at any time through "
            "automated deployment pipelines with automated rollback capabilities upon deployment canary failure."
        ),
        "page_number": 42,
        "section_heading": "CI/CD Pipeline Engineering"
    },

    # 7. Artificial Intelligence and Machine Learning
    {
        "id_key": "ai_backpropagation",
        "doc_key": "doc_ai_neural_networks",
        "subject": "Artificial Intelligence and Machine Learning",
        "topic": "Deep Learning",
        "title": "Backpropagation and Gradient Descent",
        "content": (
            "Backpropagation is the fundamental algorithm for training feedforward artificial neural networks. Utilizing the calculus chain rule, "
            "backpropagation computes the partial derivative (gradient) of an objective loss function L with respect to every weight w_ij across "
            "hidden and output layers. In the forward pass, activations propagate through non-linear activation functions (e.g. ReLU, sigmoid). "
            "In the backward pass, error deltas propagate backwards from the output layer. Gradient descent optimization updates weights "
            "proportionally to negative gradients: w <- w - eta * grad_w(L)."
        ),
        "page_number": 58,
        "section_heading": "Backpropagation and Chain Rule Derivatives"
    },
    {
        "id_key": "ai_transformer_attention",
        "doc_key": "doc_ai_transformers",
        "subject": "Artificial Intelligence and Machine Learning",
        "topic": "Transformer Architecture",
        "title": "Multi-Head Self-Attention Mechanisms",
        "content": (
            "The Transformer architecture replaces recurrent architectures with self-attention mechanisms. Scaled Dot-Product Attention operates on "
            "Query (Q), Key (K), and Value (V) matrices: Attention(Q, K, V) = softmax((Q * K^T) / sqrt(d_k)) * V, where d_k is the key dimensionality "
            "scaling factor preventing vanishing gradients in softmax. Multi-Head Attention projects Q, K, and V into h distinct representation subspaces, "
            "allowing the model to jointly attend to information from different positions and conceptual aspects."
        ),
        "page_number": 84,
        "section_heading": "Scaled Dot-Product and Multi-Head Attention"
    },

    # 8. Cloud and Distributed Systems
    {
        "id_key": "cloud_cap_theorem",
        "doc_key": "doc_cloud_distributed",
        "subject": "Cloud and Distributed Systems",
        "topic": "Distributed Consensus",
        "title": "Brewer's CAP Theorem and PACELC",
        "content": (
            "Eric Brewer's CAP theorem states that a distributed data store can simultaneously provide at most two of three guarantees: "
            "Consistency (every read receives the most recent write or an error), Availability (every non-failing node returns a non-error response), "
            "and Partition Tolerance (the system continues to operate despite arbitrary network message loss or network partitions). "
            "Because physical network partitions (P) are inevitable in real-world distributed infrastructure, systems must choose between CP "
            "(consistent under partition, refusing unavailable writes) or AP (available under partition, returning stale reads)."
        ),
        "page_number": 33,
        "section_heading": "CAP Theorem Guarantees and Partition Trade-offs"
    },
    {
        "id_key": "cloud_raft_consensus",
        "doc_key": "doc_cloud_distributed",
        "subject": "Cloud and Distributed Systems",
        "topic": "Consensus Protocols",
        "title": "The Raft Distributed Consensus Protocol",
        "content": (
            "Raft is a consensus algorithm designed for understandability, equivalent to Paxos in fault tolerance. Raft decomposes consensus into "
            "three subproblems: Leader Election, Log Replication, and Safety. Nodes exist in one of three states: Follower, Candidate, or Leader. "
            "If followers receive no heartbeat within an election timeout, they transition to Candidate, increment their term number, and vote for themselves. "
            "A candidate achieving a majority vote becomes Leader. The leader accepts client write requests, appends them to its log, and propagates "
            "AppendEntries RPCs to followers. An entry is committed once replicated across a quorum."
        ),
        "page_number": 54,
        "section_heading": "Raft Leader Election and Quorum Replication"
    },

    # 9. Computer Architecture
    {
        "id_key": "arch_pipeline_hazards",
        "doc_key": "doc_arch_cpu",
        "subject": "Computer Architecture",
        "topic": "Instruction Pipelining",
        "title": "Instruction Pipelining and Hazard Resolution",
        "content": (
            "Instruction pipelining overlaps the execution of multiple CPU instructions across stages: Fetch (IF), Decode (ID), Execute (EX), "
            "Memory Access (MEM), and Write-back (WB). Pipelining hazards prevent instruction execution in the designated clock cycle: "
            "1. Structural Hazards (hardware resource conflicts). "
            "2. Data Hazards (Read-After-Write RAW, Write-After-Read WAR, Write-After-Write WAW dependencies, resolved by operand forwarding or pipeline stalls). "
            "3. Control Hazards (branch instruction uncertainty, mitigated by branch predictors and branch delay slots)."
        ),
        "page_number": 92,
        "section_heading": "Pipelining Hazards: Data, Structural, and Control"
    },
    {
        "id_key": "arch_cache_coherence",
        "doc_key": "doc_arch_memory",
        "subject": "Computer Architecture",
        "topic": "Memory Hierarchy",
        "title": "Cache Coherence and the MESI Protocol",
        "content": (
            "In multi-core shared-memory architectures, private L1/L2 caches require coherence protocols to prevent stale reads. "
            "The MESI (Illinois) protocol invalidates or updates lines using four states: "
            "- Modified (M): Cache line is dirty, exclusive to this cache, and must be written back to main memory. "
            "- Exclusive (E): Cache line matches main memory and is present only in this cache. "
            "- Shared (S): Cache line matches main memory and may exist in other core caches. "
            "- Invalid (I): Cache line does not contain valid data. "
            "Bus snooping detects remote read and write broadcasts to trigger state transitions."
        ),
        "page_number": 115,
        "section_heading": "MESI Cache Coherence Protocol and Snooping"
    },

    # 10. Compiler Design
    {
        "id_key": "comp_lexical_cfg",
        "doc_key": "doc_compilers_parsing",
        "subject": "Compiler Design",
        "topic": "Syntax and Parsing",
        "title": "Context-Free Grammars and LL(1) vs LR(1) Parsing",
        "content": (
            "Compilers analyze source syntax using Context-Free Grammars (CFG) defined by 4-tuples G = (V, Sigma, R, S). "
            "Parsing determines derivation trees. LL(1) parsers are top-down, left-to-right parsers constructing leftmost derivations with 1 token lookahead. "
            "LL(1) cannot parse left-recursive grammars. In contrast, LR(1) parsers are bottom-up shift-reduce parsers constructing rightmost derivations "
            "in reverse with 1 token lookahead. LR(1) and LALR(1) grammars strictly subsume LL(1), handling left recursion and unambiguous operator precedence."
        ),
        "page_number": 38,
        "section_heading": "Context-Free Grammars, LL(1) and LR(1) Parsers"
    },
    {
        "id_key": "comp_ssa_form",
        "doc_key": "doc_compilers_optimization",
        "subject": "Compiler Design",
        "topic": "Optimization and Code Generation",
        "title": "Static Single Assignment (SSA) Form",
        "content": (
            "Static Single Assignment (SSA) form is an intermediate representation (IR) property where every variable is assigned exactly once "
            "and every variable is defined before its use. Where control flow paths merge, phi-functions (phi) are inserted to select the appropriate "
            "variable version depending on the predecessor basic block. SSA drastically simplifies compiler optimizations, including Dead Code Elimination (DCE), "
            "Global Value Numbering (GVN), and Constant Propagation."
        ),
        "page_number": 56,
        "section_heading": "Static Single Assignment Form in Optimizing Compilers"
    },

    # 11. Theory of Computation
    {
        "id_key": "toc_turing_halting",
        "doc_key": "doc_toc_automata",
        "subject": "Theory of Computation",
        "topic": "Computability and Automata",
        "title": "Turing Machines and the Halting Problem",
        "content": (
            "A deterministic Turing machine is a 7-tuple model of general computation comprising an infinite tape, a tape head, states, and transition rules. "
            "Alan Turing proved that the Halting Problem—determining whether an arbitrary program M halts on input w—is undecidable using a proof by contradiction "
            "based on Cantor's diagonal argument. Because the Halting Problem is undecidable, Rice's theorem establishes that any non-trivial semantic property "
            "of partial recursive functions is likewise undecidable."
        ),
        "page_number": 72,
        "section_heading": "Turing Machines and Undecidability"
    },
    {
        "id_key": "toc_p_vs_np",
        "doc_key": "doc_toc_complexity",
        "subject": "Theory of Computation",
        "topic": "Computational Complexity",
        "title": "Complexity Classes: P vs NP and NP-Completeness",
        "content": (
            "Computational complexity classifies decision problems by resource requirements. Class P comprises problems solvable by a deterministic "
            "Turing machine in polynomial time O(n^k). Class NP comprises problems whose candidate solutions can be verified in polynomial time. "
            "A problem X is NP-complete if X is in NP and every problem in NP is polynomial-time reducible to X (Cook-Levin theorem proved Boolean SAT is NP-complete). "
            "Whether P = NP remains the foremost unsolved problem in theoretical computer science."
        ),
        "page_number": 90,
        "section_heading": "Complexity Classes P, NP, and NP-Completeness"
    },

    # 12. Cybersecurity
    {
        "id_key": "sec_asymmetric_crypto",
        "doc_key": "doc_security_crypto",
        "subject": "Cybersecurity",
        "topic": "Cryptography",
        "title": "Public Key Cryptography and RSA",
        "content": (
            "Asymmetric (public key) cryptography utilizes key pairs: a public key for encryption or signature verification, and a mathematically "
            "linked private key for decryption or signing. The RSA algorithm derives security from the computational hardness of factoring large "
            "semiprime integers n = p * q. In contrast, Diffie-Hellman and Elliptic Curve Cryptography (ECC) base security on the discrete logarithm problem. "
            "ECC provides equivalent cryptographic strength to RSA with significantly smaller key sizes (e.g. 256-bit ECC vs 3072-bit RSA)."
        ),
        "page_number": 48,
        "section_heading": "Asymmetric Cryptosystems: RSA and ECC"
    },
    {
        "id_key": "sec_sqli_prevention",
        "doc_key": "doc_security_web",
        "subject": "Cybersecurity",
        "topic": "Application Security",
        "title": "SQL Injection Mechanics and Defense",
        "content": (
            "SQL Injection (SQLi) occurs when untrusted user input is directly concatenated into SQL query strings, allowing an attacker to manipulate "
            "the query structure and execute arbitrary SQL commands. SQLi vulnerabilities can lead to data exfiltration, authentication bypass, and remote code execution. "
            "The primary defense is parameterized queries (prepared statements), which separate the SQL code from untrusted data parameters, ensuring that the "
            "database engine compiles the query template before binding parameter values."
        ),
        "page_number": 82,
        "section_heading": "SQL Injection and Prepared Statement Defenses"
    },

    # 13. Web Development
    {
        "id_key": "web_dom_event_loop",
        "doc_key": "doc_web_architecture",
        "subject": "Web Development",
        "topic": "Client-Side Architecture",
        "title": "Browser DOM and JavaScript Event Loop",
        "content": (
            "The Document Object Model (DOM) is a cross-platform, language-independent object tree representing HTML documents in browsers. "
            "JavaScript executes in a single-threaded runtime driven by an event loop. Synchronous code executes on the call stack. "
            "Asynchronous operations (e.g. fetch, timers) queue callbacks in the task queue (macro-tasks) or micro-task queue (Promises). "
            "The event loop processes all pending micro-tasks immediately after each call stack execution finishes, before rendering and before processing the next macro-task."
        ),
        "page_number": 26,
        "section_heading": "DOM Representation and the Browser Event Loop"
    },
    {
        "id_key": "web_sop_cors",
        "doc_key": "doc_web_security",
        "subject": "Web Development",
        "topic": "Web Security Models",
        "title": "Same-Origin Policy and CORS",
        "content": (
            "The Same-Origin Policy (SOP) is a critical browser security boundary that restricts scripts running in one origin (defined as protocol, "
            "domain, and port tuple) from accessing DOM or network resources in another origin. Cross-Origin Resource Sharing (CORS) is a W3C mechanism "
            "that enables servers to selectively relax SOP restrictions using HTTP response headers (e.g. Access-Control-Allow-Origin, Access-Control-Allow-Methods). "
            "Non-simple cross-origin requests trigger an automatic preflight HTTP OPTIONS request before the main request is dispatched."
        ),
        "page_number": 35,
        "section_heading": "Same-Origin Policy and Cross-Origin Resource Sharing"
    }
]

def generate_curated_benchmark() -> Tuple[List[CorpusChunk], List[EvaluationCase]]:
    """
    Generates the authoritative 31-chunk curriculum corpus and 80 curated evaluation cases
    partitioned across DEV (26), VAL (20), and held-out TEST (34).
    """
    corpus_chunks: List[CorpusChunk] = []
    chunk_map: Dict[str, CorpusChunk] = {}

    for item in RAW_CORPUS:
        c_id = create_deterministic_uuid(item["id_key"])
        d_id = create_deterministic_uuid(item["doc_key"])
        chunk = CorpusChunk(
            chunk_id=c_id,
            document_id=d_id,
            subject=item["subject"],
            topic=item["topic"],
            title=item["title"],
            content=item["content"],
            token_count=len(item["content"].split()),
            page_number=item.get("page_number"),
            section_heading=item.get("section_heading")
        )
        corpus_chunks.append(chunk)
        chunk_map[item["id_key"]] = chunk

    cases: List[EvaluationCase] = []

    # =========================================================================
    # 1. DEVELOPMENT SPLIT (DEV) - 26 Cases (All Answerable, 2 per Subject)
    # Used for development, feature calibration, and prompt engineering.
    # =========================================================================
    dev_cases = [
        # Programming Fundamentals (2)
        EvaluationCase(
            case_id="eval-dev-pf-01",
            subject="Programming Fundamentals",
            topic="Recursion and Memory",
            question="What is the role of the call stack in recursion and what causes a stack overflow?",
            expected_answer="Recursion uses the call stack to store execution frames for each recursive invocation. A missing base case causes unbounded frame allocations leading to a stack overflow.",
            key_facts=["Each recursive call allocates a new stack frame", "Stack frames store local variables, parameters, and return addresses", "A base case is required to stop recursion", "Missing base case causes stack overflow"],
            relevant_chunk_ids=[chunk_map["pf_recursion_stack"].chunk_id],
            graded_relevance={str(chunk_map["pf_recursion_stack"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-pf-02",
            subject="Programming Fundamentals",
            topic="Memory Management",
            question="Compare stack and heap memory allocation in terms of lifetime and management.",
            expected_answer="Stack memory is automatically managed with LIFO scope lifetime, whereas heap memory is dynamically managed with lifetimes spanning beyond function calls.",
            key_facts=["Stack allocation is automatic and deterministic", "Heap allocation is dynamic at runtime", "Heap objects survive function returns", "Heap requires garbage collection or manual deallocation"],
            relevant_chunk_ids=[chunk_map["pf_heap_vs_stack"].chunk_id],
            graded_relevance={str(chunk_map["pf_heap_vs_stack"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.DEV
        ),
        # Data Structures and Algorithms (2)
        EvaluationCase(
            case_id="eval-dev-dsa-01",
            subject="Data Structures and Algorithms",
            topic="Binary Search Trees",
            question="What is the worst-case search complexity in an unbalanced binary search tree and why does it degrade?",
            expected_answer="In an unbalanced BST, inserting elements in sorted order causes the tree to degenerate into a linked list with worst-case O(N) search complexity.",
            key_facts=["Worst-case search complexity in an unbalanced BST is O(N)", "Sorted insertions cause degeneration into a linked list", "Balanced BST search is O(log N)"],
            relevant_chunk_ids=[chunk_map["dsa_bst_properties"].chunk_id],
            graded_relevance={str(chunk_map["dsa_bst_properties"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-dsa-02",
            subject="Data Structures and Algorithms",
            topic="AVL Trees",
            question="How do AVL tree rotations restore height balance when balance factor invariants are violated?",
            expected_answer="AVL trees maintain a balance factor between -1 and +1; when violated, single or double rotations restore height balance bounded by 1.44*log2(N).",
            key_facts=["AVL balance factor is height(left) - height(right)", "Allowed balance factors are -1, 0, +1", "Single (LL, RR) or double (LR, RL) rotations restore balance", "Guarantees O(log N) operations"],
            relevant_chunk_ids=[chunk_map["dsa_avl_rotations"].chunk_id],
            graded_relevance={str(chunk_map["dsa_avl_rotations"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        # Database Management Systems (2)
        EvaluationCase(
            case_id="eval-dev-db-01",
            subject="Database Management Systems",
            topic="Transaction Processing",
            question="Explain the difference between dirty reads and non-repeatable reads in transaction isolation.",
            expected_answer="A dirty read occurs when a transaction reads uncommitted modifications, whereas a non-repeatable read occurs when re-reading committed changes from another concurrent transaction yields different values.",
            key_facts=["Dirty reads access uncommitted changes", "Read Committed isolation prevents dirty reads", "Non-repeatable reads occur when committed updates change row values", "Repeatable Read prevents non-repeatable reads"],
            relevant_chunk_ids=[chunk_map["db_acid_properties"].chunk_id],
            graded_relevance={str(chunk_map["db_acid_properties"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-db-02",
            subject="Database Management Systems",
            topic="Relational Normalization",
            question="What condition distinguishes Boyce-Codd Normal Form (BCNF) from Third Normal Form (3NF)?",
            expected_answer="In 3NF, the right-hand side of a functional dependency may be a prime attribute, whereas in BCNF, every determinant must strictly be a superkey.",
            key_facts=["3NF allows non-superkey determinants if the dependent attribute is prime", "BCNF requires every determinant X in X -> A to be a superkey", "BCNF is strictly stronger than 3NF"],
            relevant_chunk_ids=[chunk_map["db_normalization_bcnf"].chunk_id],
            graded_relevance={str(chunk_map["db_normalization_bcnf"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.DEV
        ),
        # Operating Systems (2)
        EvaluationCase(
            case_id="eval-dev-os-01",
            subject="Operating Systems",
            topic="Virtual Memory",
            question="How does the page table and TLB translate a virtual page number to a physical frame number?",
            expected_answer="The MMU first consults the hardware TLB cache for the VPN-to-PFN mapping; on a TLB miss, it executes a hierarchical page table walk before caching the translation.",
            key_facts=["Virtual memory maps virtual page numbers to physical frame numbers", "TLB is a fast hardware cache for page translations", "TLB misses trigger multi-level page table walks", "Page faults occur when pages are not present in physical memory"],
            relevant_chunk_ids=[chunk_map["os_virtual_memory"].chunk_id],
            graded_relevance={str(chunk_map["os_virtual_memory"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-os-02",
            subject="Operating Systems",
            topic="Concurrency and Deadlock",
            question="What are the four Coffman conditions required for a deadlock to occur?",
            expected_answer="The four Coffman conditions are mutual exclusion, hold and wait, no preemption, and circular wait. All four must hold simultaneously for a deadlock.",
            key_facts=["Mutual exclusion prevents sharing", "Hold and wait allows holding while requesting", "No preemption prohibits forcible resource reclamation", "Circular wait forms a closed dependency chain"],
            relevant_chunk_ids=[chunk_map["os_deadlock_coffman"].chunk_id],
            graded_relevance={str(chunk_map["os_deadlock_coffman"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        ),
        # Computer Networks (2)
        EvaluationCase(
            case_id="eval-dev-net-01",
            subject="Computer Networks",
            topic="Transport Layer",
            question="What packets are exchanged in a standard TCP three-way handshake to establish a connection?",
            expected_answer="TCP connection establishment begins with the client sending a SYN packet, the server responding with SYN-ACK, and the client confirming with ACK.",
            key_facts=["Client sends SYN with initial sequence number", "Server replies with SYN-ACK acknowledging client ISN", "Client confirms with ACK packet", "Completes reliable full-duplex synchronization"],
            relevant_chunk_ids=[chunk_map["net_tcp_handshake"].chunk_id],
            graded_relevance={str(chunk_map["net_tcp_handshake"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-net-02",
            subject="Computer Networks",
            topic="Congestion Control",
            question="Compare TCP Slow Start and Congestion Avoidance algorithms in cwnd window scaling.",
            expected_answer="Slow Start increases the congestion window exponentially per RTT until ssthresh, after which Congestion Avoidance increases cwnd linearly by 1 MSS per RTT.",
            key_facts=["Slow Start doubles cwnd exponentially each RTT", "Transition occurs when cwnd reaches ssthresh", "Congestion Avoidance uses AIMD linear increase", "Duplicate ACKs trigger fast retransmit"],
            relevant_chunk_ids=[chunk_map["net_tcp_congestion"].chunk_id],
            graded_relevance={str(chunk_map["net_tcp_congestion"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.DEV
        ),
        # Software Engineering (2)
        EvaluationCase(
            case_id="eval-dev-se-01",
            subject="Software Engineering",
            topic="Object-Oriented Design",
            question="State the Liskov Substitution Principle and explain its impact on inheritance hierarchy.",
            expected_answer="The Liskov Substitution Principle states that subtypes must be substitutable for their base types without altering correctness or breaking caller expectations.",
            key_facts=["Subtypes must be substitutable for base types", "Preconditions cannot be strengthened in subtypes", "Postconditions cannot be weakened in subtypes", "Prevents runtime failures in polymorphic calls"],
            relevant_chunk_ids=[chunk_map["se_solid_principles"].chunk_id],
            graded_relevance={str(chunk_map["se_solid_principles"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-se-02",
            subject="Software Engineering",
            topic="DevOps and CI/CD",
            question="What is the difference between Continuous Integration and Continuous Delivery?",
            expected_answer="Continuous Integration automates code merging, builds, and unit tests, whereas Continuous Delivery automates deployment pipelines so verified builds can release at any time.",
            key_facts=["Continuous Integration focuses on automated merge and testing", "Continuous Delivery ensures deployability to production", "CD includes automated deployment and canary rollbacks"],
            relevant_chunk_ids=[chunk_map["se_ci_cd_pipeline"].chunk_id],
            graded_relevance={str(chunk_map["se_ci_cd_pipeline"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.DEV
        ),
        # Artificial Intelligence and Machine Learning (2)
        EvaluationCase(
            case_id="eval-dev-ai-01",
            subject="Artificial Intelligence and Machine Learning",
            topic="Deep Learning",
            question="How does the backpropagation algorithm compute parameter gradients in deep neural networks?",
            expected_answer="Backpropagation applies the calculus chain rule backwards from the loss function, propagating error deltas to compute weight gradients layer by layer.",
            key_facts=["Utilizes calculus chain rule for partial derivatives", "Computes loss gradient with respect to all layer weights", "Propagates errors backwards from output layer", "Gradient descent updates weights proportionally to negative gradients"],
            relevant_chunk_ids=[chunk_map["ai_backpropagation"].chunk_id],
            graded_relevance={str(chunk_map["ai_backpropagation"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-ai-02",
            subject="Artificial Intelligence and Machine Learning",
            topic="Transformer Architecture",
            question="Explain the purpose of query, key, and value matrices in multi-head self-attention.",
            expected_answer="Multi-head self-attention projects input tokens into Query, Key, and Value matrices, computing softmax attention weights from Q and K to weight the Value vectors.",
            key_facts=["Scaled dot-product attention computes Q * K^T divided by sqrt(d_k)", "Softmax normalizes attention weights across token positions", "Value vectors are aggregated by attention weights", "Multiple heads project representations into distinct subspaces"],
            relevant_chunk_ids=[chunk_map["ai_transformer_attention"].chunk_id],
            graded_relevance={str(chunk_map["ai_transformer_attention"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        # Cloud and Distributed Systems (2)
        EvaluationCase(
            case_id="eval-dev-cloud-01",
            subject="Cloud and Distributed Systems",
            topic="Distributed Consensus",
            question="Explain why a distributed system can only guarantee two out of Consistency, Availability, and Partition Tolerance.",
            expected_answer="Under Brewer's CAP theorem, when an inevitable network partition occurs, a system must choose between returning errors to guarantee consistency (CP) or serving stale data for availability (AP).",
            key_facts=["CAP theorem proves simultaneous C, A, and P is impossible", "Network partitions are unavoidable in distributed systems", "CP systems refuse updates when consistency cannot be verified", "AP systems prioritize availability over strict linearizability"],
            relevant_chunk_ids=[chunk_map["cloud_cap_theorem"].chunk_id],
            graded_relevance={str(chunk_map["cloud_cap_theorem"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-cloud-02",
            subject="Cloud and Distributed Systems",
            topic="Consensus Protocols",
            question="How does the Raft consensus algorithm handle leader election when heartbeats time out?",
            expected_answer="When a follower's election timeout expires, it increments its term, transitions to Candidate, votes for itself, and broadcasts RequestVote RPCs seeking a quorum majority.",
            key_facts=["Followers transition to Candidate upon election timeout", "Candidate increments current term number", "Candidate requests votes from all cluster peers", "Achieving a majority quorum makes the candidate Leader"],
            relevant_chunk_ids=[chunk_map["cloud_raft_consensus"].chunk_id],
            graded_relevance={str(chunk_map["cloud_raft_consensus"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        ),
        # Computer Architecture (2)
        EvaluationCase(
            case_id="eval-dev-arch-01",
            subject="Computer Architecture",
            topic="Instruction Pipelining",
            question="What are data, structural, and control hazards in CPU instruction pipelining?",
            expected_answer="Structural hazards arise from hardware resource conflicts, data hazards occur from instruction operand dependencies (RAW, WAR, WAW), and control hazards stem from branch uncertainties.",
            key_facts=["Structural hazards occur when hardware cannot support concurrent instructions", "Data hazards involve Read-After-Write or Write-After-Write dependencies", "Control hazards result from branch decisions", "Forwarding and stalls mitigate pipeline stalls"],
            relevant_chunk_ids=[chunk_map["arch_pipeline_hazards"].chunk_id],
            graded_relevance={str(chunk_map["arch_pipeline_hazards"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-arch-02",
            subject="Computer Architecture",
            topic="Memory Hierarchy",
            question="Explain the four line states in the MESI cache coherence protocol.",
            expected_answer="MESI protocol lines are in Modified (dirty and exclusive), Exclusive (clean and exclusive), Shared (clean in multiple caches), or Invalid (stale data).",
            key_facts=["Modified state indicates dirty data not yet in main memory", "Exclusive state means clean data present only in one cache", "Shared state allows multiple concurrent readers", "Invalid state requires reload via bus snooping"],
            relevant_chunk_ids=[chunk_map["arch_cache_coherence"].chunk_id],
            graded_relevance={str(chunk_map["arch_cache_coherence"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        # Compiler Design (2)
        EvaluationCase(
            case_id="eval-dev-comp-01",
            subject="Compiler Design",
            topic="Syntax and Parsing",
            question="What is the fundamental difference between LL(1) and LR(1) parsing strategies?",
            expected_answer="LL(1) is a top-down parser that constructs leftmost derivations and cannot handle left-recursive grammars, whereas LR(1) is bottom-up shift-reduce constructing rightmost derivations.",
            key_facts=["LL(1) builds parse trees top-down with leftmost derivations", "LL(1) cannot handle left-recursive productions", "LR(1) builds parse trees bottom-up with shift-reduce steps", "LR(1) strictly subsumes LL(1) in grammar class power"],
            relevant_chunk_ids=[chunk_map["comp_lexical_cfg"].chunk_id],
            graded_relevance={str(chunk_map["comp_lexical_cfg"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-comp-02",
            subject="Compiler Design",
            topic="Optimization and Code Generation",
            question="Why do optimizing compilers insert phi-functions at control flow merge points in SSA form?",
            expected_answer="Phi-functions select which variable version to assign based on the predecessor basic block traversed, maintaining single assignment invariants across branches.",
            key_facts=["SSA requires every variable to be assigned exactly once", "Phi-functions select values dynamically based on incoming control flow edge", "SSA simplifies dead code elimination and constant propagation"],
            relevant_chunk_ids=[chunk_map["comp_ssa_form"].chunk_id],
            graded_relevance={str(chunk_map["comp_ssa_form"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        # Theory of Computation (2)
        EvaluationCase(
            case_id="eval-dev-toc-01",
            subject="Theory of Computation",
            topic="Computability and Automata",
            question="Why is the Halting Problem undecidable for deterministic Turing machines?",
            expected_answer="Alan Turing proved the Halting Problem is undecidable via a proof by contradiction using Cantor's diagonalization, showing no general algorithm can decide halting for all program-input pairs.",
            key_facts=["Turing machines model general algorithmic computation", "Halting Problem asks if an arbitrary program M halts on input w", "Proved undecidable via diagonal contradiction", "Rice's theorem extends undecidability to all semantic properties"],
            relevant_chunk_ids=[chunk_map["toc_turing_halting"].chunk_id],
            graded_relevance={str(chunk_map["toc_turing_halting"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-toc-02",
            subject="Theory of Computation",
            topic="Computational Complexity",
            question="Define complexity class NP and what makes a problem NP-complete.",
            expected_answer="Class NP contains decision problems whose solutions are verifiable in polynomial time, and a problem is NP-complete if it is in NP and every problem in NP reduces to it in polynomial time.",
            key_facts=["NP problems have candidate solutions verifiable in polynomial time", "NP-complete problems are the hardest problems in NP", "Cook-Levin theorem proved Boolean SAT is NP-complete", "Whether P = NP remains unresolved"],
            relevant_chunk_ids=[chunk_map["toc_p_vs_np"].chunk_id],
            graded_relevance={str(chunk_map["toc_p_vs_np"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        ),
        # Cybersecurity (2)
        EvaluationCase(
            case_id="eval-dev-sec-01",
            subject="Cybersecurity",
            topic="Cryptography",
            question="How does RSA public-key encryption differ from Elliptic Curve Cryptography (ECC)?",
            expected_answer="RSA bases security on the hardness of integer factorization of large semiprimes, whereas ECC relies on the elliptic curve discrete logarithm problem, providing equivalent security with smaller keys.",
            key_facts=["RSA security derives from prime integer factorization hardness", "ECC security relies on the discrete logarithm problem over elliptic curves", "ECC achieves comparable security with much smaller key lengths (256-bit vs 3072-bit)"],
            relevant_chunk_ids=[chunk_map["sec_asymmetric_crypto"].chunk_id],
            graded_relevance={str(chunk_map["sec_asymmetric_crypto"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-sec-02",
            subject="Cybersecurity",
            topic="Application Security",
            question="Why do parameterized queries prevent SQL injection attacks?",
            expected_answer="Parameterized queries separate query logic from data by compiling the SQL template before binding parameters, preventing untrusted input from modifying the query structure.",
            key_facts=["SQL injection occurs when untrusted input is concatenated into query strings", "Parameterized queries compile the query template before parameter binding", "Data parameters are treated strictly as literal values, not executable syntax"],
            relevant_chunk_ids=[chunk_map["sec_sqli_prevention"].chunk_id],
            graded_relevance={str(chunk_map["sec_sqli_prevention"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        ),
        # Web Development (2)
        EvaluationCase(
            case_id="eval-dev-web-01",
            subject="Web Development",
            topic="Client-Side Architecture",
            question="In the JavaScript event loop, when are microtasks executed relative to macrotasks?",
            expected_answer="Microtasks (e.g. Promise callbacks) execute immediately when the current call stack empties, draining the entire microtask queue before browser rendering and before the next macrotask.",
            key_facts=["JavaScript runs on a single-threaded event loop", "Synchronous code runs on the call stack", "Microtask queue is fully drained before the next macrotask is dequeued", "Browser rendering occurs after microtask queue draining"],
            relevant_chunk_ids=[chunk_map["web_dom_event_loop"].chunk_id],
            graded_relevance={str(chunk_map["web_dom_event_loop"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.DEV
        ),
        EvaluationCase(
            case_id="eval-dev-web-02",
            subject="Web Development",
            topic="Web Security Models",
            question="What is the Same-Origin Policy and under what conditions is a CORS preflight OPTIONS request required?",
            expected_answer="The Same-Origin Policy restricts scripts in one origin from accessing resources in another origin, and CORS preflight OPTIONS requests are required for non-simple cross-origin HTTP requests.",
            key_facts=["Same-Origin Policy enforces origin boundary (protocol, domain, port)", "CORS allows servers to relax SOP via response headers", "Non-simple cross-origin requests trigger automated preflight HTTP OPTIONS"],
            relevant_chunk_ids=[chunk_map["web_sop_cors"].chunk_id],
            graded_relevance={str(chunk_map["web_sop_cors"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.DEV
        )
    ]
    cases.extend(dev_cases)

    # =========================================================================
    # 2. VALIDATION SPLIT (VAL) - 20 Cases (14 Answerable + 6 Unanswerable)
    # Used for hyperparameter tuning (e.g. RRF k, reranking top-k, threshold calibration).
    # =========================================================================
    val_cases = [
        # Answerable Validation Cases (14)
        EvaluationCase(
            case_id="eval-val-pf-01",
            subject="Programming Fundamentals",
            topic="Scope and Lifetime",
            question="How does lexical scoping resolve identifiers in nested closures?",
            expected_answer="Lexical scoping resolves variable references based on the static source code nesting, allowing inner closures to retain references to variables in enclosing scopes after outer functions return.",
            key_facts=["Lexical scoping resolves variable references at compile time based on static code structure", "Inner closures retain access to outer function scope variables even after outer return"],
            relevant_chunk_ids=[chunk_map["pf_variable_scope"].chunk_id],
            graded_relevance={str(chunk_map["pf_variable_scope"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-dsa-01",
            subject="Data Structures and Algorithms",
            topic="Graph Algorithms",
            question="Under what edge weight condition does Dijkstra's shortest path algorithm guarantee correctness?",
            expected_answer="Dijkstra's algorithm guarantees correctness strictly on graphs with non-negative edge weights; negative edge weights require Bellman-Ford.",
            key_facts=["Dijkstra requires strictly non-negative edge weights", "Greedy relaxation assumes visited node distances never decrease", "Negative weights violate greedy subproblem optimality"],
            relevant_chunk_ids=[chunk_map["dsa_dijkstra_algorithm"].chunk_id],
            graded_relevance={str(chunk_map["dsa_dijkstra_algorithm"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-db-01",
            subject="Database Management Systems",
            topic="Indexing and Storage",
            question="Why are B+ trees preferred over standard binary search trees for disk-based database indexes?",
            expected_answer="B+ trees provide very high node fan-out which minimizes tree depth and disk I/O, and linked leaf nodes allow sequential range scans without re-traversing upper nodes.",
            key_facts=["B+ tree internal nodes store only routing keys maximizing fan-out", "Shallow tree depth minimizes disk I/O block reads", "Doubly-linked leaf nodes enable sequential range queries"],
            relevant_chunk_ids=[chunk_map["db_btree_indexing"].chunk_id],
            graded_relevance={str(chunk_map["db_btree_indexing"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-os-01",
            subject="Operating Systems",
            topic="CPU Scheduling",
            question="How does the Linux Completely Fair Scheduler (CFS) use virtual runtime (vruntime) for thread scheduling?",
            expected_answer="CFS tracks elapsed execution time normalized by priority in vruntime stored in a red-black tree, always picking the task with the lowest vruntime to run next.",
            key_facts=["CFS tracks task execution in virtual runtime (vruntime)", "vruntime is normalized by process nice value", "CFS uses red-black trees to select the task with minimum vruntime"],
            relevant_chunk_ids=[chunk_map["os_process_scheduling"].chunk_id],
            graded_relevance={str(chunk_map["os_process_scheduling"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-net-01",
            subject="Computer Networks",
            topic="Application Layer",
            question="Describe the recursive resolution process through Root, TLD, and Authoritative DNS servers.",
            expected_answer="A recursive DNS resolver queries Root servers for TLD referrals, queries the TLD server for authoritative name server locations, and finally queries the authoritative server for the IP address.",
            key_facts=["Recursive resolver queries Root DNS servers first", "Root servers return Top-Level Domain (TLD) referrals", "TLD servers return authoritative name server addresses", "Responses are cached locally according to TTL"],
            relevant_chunk_ids=[chunk_map["net_dns_resolution"].chunk_id],
            graded_relevance={str(chunk_map["net_dns_resolution"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-se-01",
            subject="Software Engineering",
            topic="Object-Oriented Design",
            question="Explain the Dependency Inversion Principle and how interfaces decouple modules.",
            expected_answer="The Dependency Inversion Principle dictates that high-level modules should not depend on low-level modules, but rather both should depend on abstractions such as interfaces.",
            key_facts=["High-level modules should depend on abstractions not concrete classes", "Low-level modules implement abstractions", "Prevents tight coupling across architectural boundaries"],
            relevant_chunk_ids=[chunk_map["se_solid_principles"].chunk_id],
            graded_relevance={str(chunk_map["se_solid_principles"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-ai-01",
            subject="Artificial Intelligence and Machine Learning",
            topic="Transformer Architecture",
            question="Why does scaled dot-product attention divide by the square root of the key dimension d_k?",
            expected_answer="Dividing by sqrt(d_k) prevents large dot products from pushing the softmax function into regions with extremely small gradients, stabilizing training.",
            key_facts=["Dot products grow large in high dimensional spaces", "Large magnitude inputs saturate softmax functions", "Division by sqrt(d_k) preserves stable gradient flow during backpropagation"],
            relevant_chunk_ids=[chunk_map["ai_transformer_attention"].chunk_id],
            graded_relevance={str(chunk_map["ai_transformer_attention"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-cloud-01",
            subject="Cloud and Distributed Systems",
            topic="Consensus Protocols",
            question="How does Raft ensure committed log entries are never overwritten by new leaders?",
            expected_answer="Raft ensures Leader Completeness by requiring candidate nodes to have logs at least as up-to-date as any voter's log in order to receive votes.",
            key_facts=["Raft candidates must possess all committed entries to win election", "Voters deny votes to candidates with less up-to-date logs", "Committed entries are replicated on a quorum majority"],
            relevant_chunk_ids=[chunk_map["cloud_raft_consensus"].chunk_id],
            graded_relevance={str(chunk_map["cloud_raft_consensus"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-arch-01",
            subject="Computer Architecture",
            topic="Memory Hierarchy",
            question="What happens to a Modified cache line in MESI when another core reads that memory block?",
            expected_answer="When a remote core reads a Modified line, the holding core intercepts the request via snooping, writes the line back to main memory, and transitions its line to Shared.",
            key_facts=["Holding core snoops the bus read broadcast", "Dirty cache line is written back to main memory", "Holding core transitions state from Modified to Shared", "Requesting core receives data and enters Shared state"],
            relevant_chunk_ids=[chunk_map["arch_cache_coherence"].chunk_id],
            graded_relevance={str(chunk_map["arch_cache_coherence"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-comp-01",
            subject="Compiler Design",
            topic="Optimization and Code Generation",
            question="How does Static Single Assignment (SSA) form facilitate dead code elimination?",
            expected_answer="Because every variable in SSA has exactly one definition site, use-def chains are trivial, allowing the compiler to identify and delete assignments whose variables have no uses.",
            key_facts=["SSA establishes explicit 1:1 definition-use relationships", "Trivial use-def chains eliminate complex dataflow tracking", "Definitions with zero uses can be safely eliminated as dead code"],
            relevant_chunk_ids=[chunk_map["comp_ssa_form"].chunk_id],
            graded_relevance={str(chunk_map["comp_ssa_form"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-toc-01",
            subject="Theory of Computation",
            topic="Computational Complexity",
            question="What is the significance of the Cook-Levin theorem for NP-completeness proofs?",
            expected_answer="The Cook-Levin theorem proved that Boolean Satisfiability (SAT) is NP-complete, providing the foundational base problem from which all other NP-completeness reductions are built.",
            key_facts=["Cook-Levin theorem proved Boolean SAT is NP-complete", "Established that all NP problems polynomial-time reduce to SAT", "Serves as the root for subsequent polynomial-time reductions"],
            relevant_chunk_ids=[chunk_map["toc_p_vs_np"].chunk_id],
            graded_relevance={str(chunk_map["toc_p_vs_np"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-sec-01",
            subject="Cybersecurity",
            topic="Cryptography",
            question="Why does 256-bit ECC provide comparable security to 3072-bit RSA with lower computational overhead?",
            expected_answer="ECC relies on the discrete logarithm problem over elliptic curves where no sub-exponential algorithm exists, unlike RSA where sub-exponential General Number Field Sieve factoring reduces effective security.",
            key_facts=["RSA suffers from sub-exponential General Number Field Sieve factoring", "Elliptic curve discrete logs require fully exponential Pollard-rho steps", "Allows 256-bit ECC keys to match 3072-bit RSA security strength"],
            relevant_chunk_ids=[chunk_map["sec_asymmetric_crypto"].chunk_id],
            graded_relevance={str(chunk_map["sec_asymmetric_crypto"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-web-01",
            subject="Web Development",
            topic="Web Security Models",
            question="What three components define an origin in browser security under the Same-Origin Policy?",
            expected_answer="An origin is defined strictly as the tuple of URI protocol/scheme, hostname/domain, and port number.",
            key_facts=["Origin is defined as protocol, domain, and port tuple", "All three components must match exactly for same-origin requests", "Cross-origin requests are blocked unless allowed by CORS headers"],
            relevant_chunk_ids=[chunk_map["web_sop_cors"].chunk_id],
            graded_relevance={str(chunk_map["web_sop_cors"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-multi-01",
            subject="Programming Fundamentals",
            topic="Cross-Domain Memory",
            question="How does the stack allocation mechanism in programming languages interact with virtual memory pages in the operating system?",
            expected_answer="Function call frames are allocated in the process call stack, which is mapped into contiguous virtual memory pages that the MMU translates into physical frames via the page table.",
            key_facts=["Call stack frames reside inside process virtual memory address space", "MMU translates virtual stack pages into physical frames using page tables", "Stack growth exceeding allocated virtual pages triggers stack overflow or page faults"],
            relevant_chunk_ids=[chunk_map["pf_recursion_stack"].chunk_id, chunk_map["os_virtual_memory"].chunk_id],
            graded_relevance={
                str(chunk_map["pf_recursion_stack"].chunk_id): 3,
                str(chunk_map["os_virtual_memory"].chunk_id): 2
            },
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.MULTI_HOP,
            is_multi_source=True,
            split=DatasetSplit.VAL
        ),

        # Unanswerable Validation Stress Cases (6)
        EvaluationCase(
            case_id="eval-val-unans-01",
            subject="Programming Fundamentals",
            topic="Advanced Recursion",
            question="What is the tail call optimization mechanism in mutual recursion under LLVM compiler flags?",
            expected_answer="The provided documents do not contain information regarding tail call optimization mechanisms or LLVM compiler optimization flags.",
            key_facts=["Tail call optimization details are absent from the corpus", "LLVM optimization flags are not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Tail call optimization and LLVM compiler flags are not covered in the curriculum corpus.",
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-unans-02",
            subject="Data Structures and Algorithms",
            topic="Advanced Heaps",
            question="How does a Fibonacci Heap achieve O(1) amortized decrease-key complexity using potential functions?",
            expected_answer="The provided documents do not contain information regarding Fibonacci heaps or amortized potential function proofs.",
            key_facts=["Fibonacci heap decrease-key operations are absent", "Potential function amortized analysis is not in the corpus"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Fibonacci heaps and amortized potential functions are absent from the curriculum corpus.",
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-unans-03",
            subject="Database Management Systems",
            topic="Storage Engines",
            question="How does PostgreSQL vacuuming clean up dead row versions in transaction ID wraparound?",
            expected_answer="The provided documents do not contain information regarding PostgreSQL vacuuming mechanics or transaction ID wraparound.",
            key_facts=["Dead tuple cleanup mechanics are absent", "Transaction ID wraparound is not in the corpus"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Postgres dead tuple cleanup and transaction ID wraparound are absent.",
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-unans-04",
            subject="Computer Networks",
            topic="Transport Layer",
            question="Explain the QUIC protocol 0-RTT connection resumption mechanism over UDP.",
            expected_answer="The provided documents do not contain information regarding the QUIC transport protocol or 0-RTT handshakes.",
            key_facts=["QUIC protocol details are absent", "0-RTT connection resumption is not in the corpus"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="QUIC protocol and 0-RTT handshakes are not in the corpus.",
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-unans-05",
            subject="Artificial Intelligence and Machine Learning",
            topic="GPU Acceleration",
            question="What is FlashAttention-2 tiling in GPU SRAM kernel fusion for long context transformers?",
            expected_answer="The provided documents do not contain information regarding FlashAttention kernel tiling or GPU SRAM caching.",
            key_facts=["FlashAttention GPU kernel tiling is absent", "SRAM hardware memory hierarchy is not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="FlashAttention GPU memory hierarchy kernel tiling is not in the corpus.",
            split=DatasetSplit.VAL
        ),
        EvaluationCase(
            case_id="eval-val-unans-06",
            subject="Cloud and Distributed Systems",
            topic="Distributed Clocks",
            question="How does Google Spanner use TrueTime atomic clocks to serialize distributed read-write transactions?",
            expected_answer="The provided documents do not contain information regarding Google Spanner or TrueTime atomic clock hardware synchronization.",
            key_facts=["Google Spanner is not in the corpus", "TrueTime GPS/atomic clock uncertainty bounds are absent"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Google Spanner and TrueTime GPS/atomic clock uncertainty bounds are not in the corpus.",
            split=DatasetSplit.VAL
        )
    ]
    cases.extend(val_cases)

    # =========================================================================
    # 3. HELD-OUT TEST SPLIT (TEST) - 34 Cases (20 Answerable + 14 Unanswerable/Distractor)
    # Balanced held-out evaluation measuring both successful answering and correct abstention.
    # Strictly held out: never used for parameter tuning.
    # =========================================================================
    test_cases = [
        # --- Answerable Test Cases (20) ---
        # Direct Factual (6)
        EvaluationCase(
            case_id="eval-test-ans-01",
            subject="Programming Fundamentals",
            topic="Recursion and Memory",
            question="What specific information is stored within an activation record on the call stack during a function call?",
            expected_answer="An activation record (stack frame) on the call stack stores local variables, function parameters, and the return address to resume after completion.",
            key_facts=["Stack frames store local variables", "Stack frames store function arguments/parameters", "Stack frames store the return execution address"],
            relevant_chunk_ids=[chunk_map["pf_recursion_stack"].chunk_id],
            graded_relevance={str(chunk_map["pf_recursion_stack"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-02",
            subject="Data Structures and Algorithms",
            topic="Graph Algorithms",
            question="Why does Dijkstra's algorithm require non-negative edge weights to guarantee finding the shortest path?",
            expected_answer="Dijkstra's greedy choice assumes that once a node is marked visited with minimum tentative distance, no subsequent path can reduce that distance, an invariant broken by negative edge weights.",
            key_facts=["Dijkstra greedily marks nodes visited assuming shortest path is finalized", "Negative edge weights can decrease path cost after a node is visited", "Violates greedy choice property"],
            relevant_chunk_ids=[chunk_map["dsa_dijkstra_algorithm"].chunk_id],
            graded_relevance={str(chunk_map["dsa_dijkstra_algorithm"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-03",
            subject="Database Management Systems",
            topic="Indexing and Storage",
            question="Why do B+ trees link all leaf nodes in a doubly-linked list?",
            expected_answer="Linking leaf nodes in a doubly-linked list enables efficient sequential range scans across records without traversing upper tree levels.",
            key_facts=["All leaf nodes are connected in a contiguous doubly-linked list", "Enables sequential range scans across table rows", "Avoids re-traversing upper tree nodes"],
            relevant_chunk_ids=[chunk_map["db_btree_indexing"].chunk_id],
            graded_relevance={str(chunk_map["db_btree_indexing"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-04",
            subject="Operating Systems",
            topic="Concurrency and Deadlock",
            question="Which Coffman condition is eliminated by enforcing a strict global resource ordering?",
            expected_answer="Enforcing a strict global resource ordering eliminates the Circular Wait condition, as processes must request resources in strictly ascending numerical order.",
            key_facts=["Global resource ordering eliminates Circular Wait", "Processes must request resources in strictly ascending numerical order", "Prevents closed dependency cycles"],
            relevant_chunk_ids=[chunk_map["os_deadlock_coffman"].chunk_id],
            graded_relevance={str(chunk_map["os_deadlock_coffman"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-05",
            subject="Computer Networks",
            topic="Application Layer",
            question="What default transport port and protocol does DNS use for queries under 512 bytes?",
            expected_answer="DNS predominantly uses UDP port 53 for standard queries under 512 bytes to minimize latency overhead.",
            key_facts=["DNS default transport protocol is UDP", "DNS uses port 53", "Falls back to TCP for responses exceeding 512 bytes"],
            relevant_chunk_ids=[chunk_map["net_dns_resolution"].chunk_id],
            graded_relevance={str(chunk_map["net_dns_resolution"].chunk_id): 3},
            difficulty=DifficultyLevel.BEGINNER,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-06",
            subject="Cybersecurity",
            topic="Application Security",
            question="What role does the database query compiler play in making prepared statements immune to SQL injection?",
            expected_answer="Prepared statements cause the database engine to parse and compile the SQL query template before binding untrusted parameter values as literal data.",
            key_facts=["Database engine compiles query template before parameter binding", "Untrusted inputs are treated strictly as data literals", "Prevents attacker inputs from altering SQL query grammar"],
            relevant_chunk_ids=[chunk_map["sec_sqli_prevention"].chunk_id],
            graded_relevance={str(chunk_map["sec_sqli_prevention"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.DIRECT_FACTUAL,
            split=DatasetSplit.TEST
        ),

        # Conceptual (6)
        EvaluationCase(
            case_id="eval-test-ans-07",
            subject="Software Engineering",
            topic="Object-Oriented Design",
            question="Explain the Open/Closed Principle and describe how polymorphism enables extension without modification.",
            expected_answer="The Open/Closed Principle states software entities should be open for extension but closed for modification, achieved through polymorphic interfaces where new behaviors are added via new subclasses.",
            key_facts=["Entities should be open for extension but closed for modification", "Abstractions and interfaces allow adding new behaviors without altering existing code", "Polymorphism decouples callers from concrete implementations"],
            relevant_chunk_ids=[chunk_map["se_solid_principles"].chunk_id],
            graded_relevance={str(chunk_map["se_solid_principles"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-08",
            subject="Artificial Intelligence and Machine Learning",
            topic="Deep Learning",
            question="How does the chain rule enable backward propagation of errors from loss to input layer?",
            expected_answer="The calculus chain rule allows computing composite derivatives by multiplying intermediate partial derivatives backwards through successive network layers.",
            key_facts=["Chain rule multiplies partial derivatives across composite functions", "Error deltas propagate backwards from output loss layer", "Enables computing weight gradients at hidden and input layers"],
            relevant_chunk_ids=[chunk_map["ai_backpropagation"].chunk_id],
            graded_relevance={str(chunk_map["ai_backpropagation"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-09",
            subject="Cloud and Distributed Systems",
            topic="Consensus Protocols",
            question="How do randomized election timeouts in Raft prevent split votes among candidate nodes?",
            expected_answer="Raft randomizes follower election timeouts across an interval (e.g. 150-300ms) so one follower times out first, becoming a Candidate and winning a majority before competitors.",
            key_facts=["Followers use randomized election timeouts", "Prevents simultaneous candidate transitions", "Avoids split votes by enabling one candidate to win majority quorum quickly"],
            relevant_chunk_ids=[chunk_map["cloud_raft_consensus"].chunk_id],
            graded_relevance={str(chunk_map["cloud_raft_consensus"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-10",
            subject="Computer Architecture",
            topic="Instruction Pipelining",
            question="How does data forwarding (bypassing) resolve Read-After-Write (RAW) data hazards in pipelined processors?",
            expected_answer="Data forwarding routes calculated results directly from the Execution (EX) or Memory (MEM) pipeline stages to the Decode/Execute stage of subsequent instructions without waiting for register write-back.",
            key_facts=["Data forwarding routes results directly from EX/MEM stages", "Eliminates stalls by supplying operands before register write-back", "Resolves Read-After-Write dependencies"],
            relevant_chunk_ids=[chunk_map["arch_pipeline_hazards"].chunk_id],
            graded_relevance={str(chunk_map["arch_pipeline_hazards"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-11",
            subject="Compiler Design",
            topic="Syntax and Parsing",
            question="How does an LR(1) parser use a lookahead token to resolve shift-reduce conflicts?",
            expected_answer="An LR(1) parser inspects 1 lookahead token to determine whether to shift the token onto the parse stack or reduce a completed production rule based on canonical LR items.",
            key_facts=["LR(1) parsers use bottom-up shift-reduce derivations", "1 lookahead token disambiguates between shifting and reducing", "Constructs rightmost derivations in reverse"],
            relevant_chunk_ids=[chunk_map["comp_lexical_cfg"].chunk_id],
            graded_relevance={str(chunk_map["comp_lexical_cfg"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-12",
            subject="Theory of Computation",
            topic="Computability and Automata",
            question="What does Rice's Theorem imply about undecidability of program behavior from Turing machines?",
            expected_answer="Rice's Theorem proves that any non-trivial semantic property of partial recursive functions computed by Turing machines is undecidable.",
            key_facts=["Rice's theorem applies to all non-trivial semantic properties", "Proves behavioral analysis of arbitrary Turing machines is undecidable", "Derived from the undecidability of the Halting Problem"],
            relevant_chunk_ids=[chunk_map["toc_turing_halting"].chunk_id],
            graded_relevance={str(chunk_map["toc_turing_halting"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.CONCEPTUAL,
            split=DatasetSplit.TEST
        ),

        # Multi-Source / Multi-Hop (3)
        EvaluationCase(
            case_id="eval-test-ans-13",
            subject="Computer Networks",
            topic="Network and Web Integration",
            question="How does the TCP 3-way handshake relate to latency overhead in CORS preflight OPTIONS requests?",
            expected_answer="Non-simple cross-origin web requests require an automated preflight HTTP OPTIONS round trip over TCP, incurring connection handshake round trips (SYN, SYN-ACK, ACK) and CORS authorization overhead before data transfer.",
            key_facts=["TCP connection establishment requires SYN, SYN-ACK, ACK round trips", "CORS requires automated HTTP OPTIONS preflight for non-simple requests", "Compound latency overhead impacts cross-origin web applications"],
            relevant_chunk_ids=[chunk_map["net_tcp_handshake"].chunk_id, chunk_map["web_sop_cors"].chunk_id],
            graded_relevance={
                str(chunk_map["net_tcp_handshake"].chunk_id): 3,
                str(chunk_map["web_sop_cors"].chunk_id): 3
            },
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.MULTI_HOP,
            is_multi_source=True,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-14",
            subject="Database Management Systems",
            topic="DB Indexing and OS Virtual Memory",
            question="Compare how B+ tree page sizes are aligned with operating system virtual memory page frames to minimize I/O overhead.",
            expected_answer="Database B+ tree nodes are sized to match or multiply OS virtual memory page sizes (e.g. 4KB or 8KB), ensuring that fetching an index node corresponds exactly to virtual page translations without wasted I/O.",
            key_facts=["B+ tree nodes are sized to match disk block and virtual memory page boundaries", "Virtual memory MMU maps pages to frames using page tables and TLB", "Alignment minimizes disk block transfers and page fault latency"],
            relevant_chunk_ids=[chunk_map["db_btree_indexing"].chunk_id, chunk_map["os_virtual_memory"].chunk_id],
            graded_relevance={
                str(chunk_map["db_btree_indexing"].chunk_id): 3,
                str(chunk_map["os_virtual_memory"].chunk_id): 2
            },
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.MULTI_HOP,
            is_multi_source=True,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-15",
            subject="Computer Architecture",
            topic="Hardware Coherence and Concurrency",
            question="How does hardware cache line invalidation in the MESI protocol impact thread synchronization and deadlock detection in multicore systems?",
            expected_answer="When multiple cores synchronize shared resources (e.g. mutexes), writes cause MESI cache lines to invalidate in peer caches, while improper resource lock ordering across threads can satisfy the Coffman circular wait condition leading to deadlock.",
            key_facts=["MESI bus snooping invalidates shared cache lines upon write", "Thread synchronization variables trigger cache line invalidations", "Lack of global resource lock ordering satisfies Coffman circular wait leading to deadlock"],
            relevant_chunk_ids=[chunk_map["arch_cache_coherence"].chunk_id, chunk_map["os_deadlock_coffman"].chunk_id],
            graded_relevance={
                str(chunk_map["arch_cache_coherence"].chunk_id): 3,
                str(chunk_map["os_deadlock_coffman"].chunk_id): 2
            },
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.MULTI_HOP,
            is_multi_source=True,
            split=DatasetSplit.TEST
        ),

        # Comparison (3)
        EvaluationCase(
            case_id="eval-test-ans-16",
            subject="Compiler Design",
            topic="Syntax and Parsing",
            question="Compare LL(1) top-down recursive descent with LR(1) bottom-up shift-reduce parsing in language grammar coverage.",
            expected_answer="LL(1) top-down parsers require non-left-recursive grammars and predict productions from 1 token lookahead, whereas LR(1) bottom-up parsers handle left-recursive grammars and represent a strictly larger grammar class.",
            key_facts=["LL(1) is top-down and cannot handle left-recursive grammars", "LR(1) is bottom-up shift-reduce and handles left recursion", "LR(1) grammar class strictly contains LL(1)"],
            relevant_chunk_ids=[chunk_map["comp_lexical_cfg"].chunk_id],
            graded_relevance={str(chunk_map["comp_lexical_cfg"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-17",
            subject="Cybersecurity",
            topic="Cryptography",
            question="Compare the mathematical trapdoor functions underlying RSA factorization versus Elliptic Curve discrete logarithms.",
            expected_answer="RSA relies on the difficulty of integer factorization of semiprimes (n = p * q), while ECC relies on the discrete logarithm problem over points on an elliptic curve, yielding equivalent security with smaller keys.",
            key_facts=["RSA trapdoor is based on semiprime integer factorization", "ECC trapdoor is based on elliptic curve discrete logarithm problem", "ECC provides equivalent security with smaller keys"],
            relevant_chunk_ids=[chunk_map["sec_asymmetric_crypto"].chunk_id],
            graded_relevance={str(chunk_map["sec_asymmetric_crypto"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-18",
            subject="Web Development",
            topic="Client-Side Architecture",
            question="Compare microtask and macrotask scheduling priority in the browser event loop.",
            expected_answer="Microtasks (Promise reactions) have higher execution priority and are completely drained whenever the call stack empties, before the browser renders and before the next macrotask (timers, I/O) executes.",
            key_facts=["Microtasks include Promise callbacks and MutationObserver", "Macrotasks include setTimeout, setInterval, and I/O callbacks", "Microtask queue is fully drained before the next macrotask is dequeued"],
            relevant_chunk_ids=[chunk_map["web_dom_event_loop"].chunk_id],
            graded_relevance={str(chunk_map["web_dom_event_loop"].chunk_id): 3},
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.COMPARISON,
            split=DatasetSplit.TEST
        ),

        # Partial-Evidence Questions (2)
        EvaluationCase(
            case_id="eval-test-ans-19",
            subject="Cloud and Distributed Systems",
            topic="Consensus Trade-offs",
            question="Explain the CAP theorem partition tolerance trade-off and describe the Paxos multi-decree protocol phases.",
            expected_answer="The documents explain the CAP theorem trade-off between consistency and availability under partition [S1], but do not contain information regarding multi-decree Paxos protocol phases.",
            key_facts=["CAP theorem forces choice between CP and AP under partition", "Multi-decree Paxos details are absent from the provided documents"],
            relevant_chunk_ids=[chunk_map["cloud_cap_theorem"].chunk_id],
            graded_relevance={str(chunk_map["cloud_cap_theorem"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.PARTIALLY_ANSWERABLE,
            abstention_rationale="CAP theorem is supported, but Paxos multi-decree protocol phases are absent from the corpus.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-ans-20",
            subject="Data Structures and Algorithms",
            topic="Tree Structures",
            question="Explain BST worst-case search complexity and describe how splay tree zig-zig rotations maintain amortized bounds.",
            expected_answer="The documents explain that unbalanced BST search degrades to O(N) worst-case time [S1], but do not contain information on splay tree zig-zig rotations.",
            key_facts=["Unbalanced BST search degrades to O(N) when inserting sorted elements", "Splay tree zig-zig rotation mechanics are absent from the corpus"],
            relevant_chunk_ids=[chunk_map["dsa_bst_properties"].chunk_id],
            graded_relevance={str(chunk_map["dsa_bst_properties"].chunk_id): 3},
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.PARTIALLY_ANSWERABLE,
            abstention_rationale="BST properties are supported, but splay tree zig-zig rotations are absent from the corpus.",
            split=DatasetSplit.TEST
        ),

        # --- Unanswerable / Abstention / Distractor Stress Cases (14) ---
        # Completely Absent Evidence (8)
        EvaluationCase(
            case_id="eval-test-unans-01",
            subject="Artificial Intelligence and Machine Learning",
            topic="Quantum Computing",
            question="What is the decoherence rate of superconducting transmon qubits under cryogenic dilution?",
            expected_answer="The provided documents do not contain information regarding superconducting transmon qubits or quantum decoherence rates.",
            key_facts=["Superconducting qubits are absent from the corpus", "Cryogenic dilution decoherence rates are not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Quantum computing and qubit decoherence are absent from the curriculum corpus.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-unans-02",
            subject="Database Management Systems",
            topic="Distributed Databases",
            question="How does Apache Cassandra use hinted handoffs and read repair during partition healing?",
            expected_answer="The provided documents do not contain information regarding Apache Cassandra hinted handoffs or read repair mechanisms.",
            key_facts=["Apache Cassandra is absent from the corpus", "Hinted handoffs and read repair are not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Cassandra-specific hinted handoffs and read repair protocols are absent.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-unans-03",
            subject="Operating Systems",
            topic="Kernel Security",
            question="Explain the eBPF verifier bytecode static safety checks for kernel ring buffers.",
            expected_answer="The provided documents do not contain information regarding Linux eBPF bytecode verification or kernel ring buffers.",
            key_facts=["Linux eBPF architecture is absent from the corpus", "Bytecode static safety verification is not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Linux eBPF bytecode verification is absent from the curriculum corpus.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-unans-04",
            subject="Computer Networks",
            topic="Virtualization",
            question="How does BGP EVPN use VXLAN encapsulation for data center network virtualization?",
            expected_answer="The provided documents do not contain information regarding BGP EVPN or VXLAN network encapsulation.",
            key_facts=["BGP EVPN protocol is absent from the corpus", "VXLAN network virtualization is not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="BGP EVPN and VXLAN network overlay protocols are absent from the curriculum corpus.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-unans-05",
            subject="Artificial Intelligence and Machine Learning",
            topic="Optimization",
            question="Explain the mathematical derivation of AdamW decoupled weight decay versus L2 regularization.",
            expected_answer="The provided documents do not contain information regarding AdamW decoupled weight decay or its mathematical derivation.",
            key_facts=["AdamW optimizer equations are absent from the corpus", "Decoupled weight decay derivation is not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="AdamW decoupled weight decay optimizer equations are absent from the corpus.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-unans-06",
            subject="Compiler Design",
            topic="Virtual Machines",
            question="How does the GraalVM polyglot engine compile JavaScript to native machine code via Truffle AST specialization?",
            expected_answer="The provided documents do not contain information regarding GraalVM, Truffle AST specialization, or polyglot runtime compilation.",
            key_facts=["GraalVM engine architecture is absent from the corpus", "Truffle AST specialization is not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="GraalVM Truffle AST specialization is absent from the curriculum corpus.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-unans-07",
            subject="Cybersecurity",
            topic="Zero-Knowledge Proofs",
            question="Explain zero-knowledge succinct non-interactive arguments of knowledge (zk-SNARKs) quadratic arithmetic programs.",
            expected_answer="The provided documents do not contain information regarding zk-SNARKs or quadratic arithmetic program transformations.",
            key_facts=["zk-SNARKs cryptographic protocols are absent from the corpus", "Quadratic arithmetic programs are not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Zero-knowledge proofs and QAP transformations are absent from the curriculum corpus.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-unans-08",
            subject="Web Development",
            topic="WebAssembly",
            question="How does WebAssembly linear memory interact with JavaScript SharedArrayBuffer under the Atomics memory model?",
            expected_answer="The provided documents do not contain information regarding WebAssembly linear memory, SharedArrayBuffer, or JavaScript Atomics.",
            key_facts=["WebAssembly linear memory is absent from the corpus", "SharedArrayBuffer Atomics concurrency is not covered"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.INSUFFICIENT_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="WebAssembly linear memory and SharedArrayBuffer Atomics concurrency are absent from the corpus.",
            split=DatasetSplit.TEST
        ),

        # Topical Distractor Questions (4) - domain keywords present, but target relationship is absent
        EvaluationCase(
            case_id="eval-test-distract-01",
            subject="Data Structures and Algorithms",
            topic="Graph Algorithms",
            question="How does Dijkstra's algorithm handle negative weight cycles by updating the predecessor array?",
            expected_answer="The provided documents state that Dijkstra's algorithm strictly requires non-negative edge weights and fails on negative weights; handling negative weight cycles requires Bellman-Ford, which is not in the corpus.",
            key_facts=["Dijkstra strictly requires non-negative edge weights", "Dijkstra cannot handle negative weight cycles"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.DISTRACTOR,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Dijkstra is covered, but explicitly assumes non-negative weights; negative weight cycle handling requires Bellman-Ford, which is absent.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-distract-02",
            subject="Computer Architecture",
            topic="Instruction Pipelining",
            question="Explain how branch prediction in instruction pipelining uses the Spectre speculative execution gadget to leak kernel memory.",
            expected_answer="The provided documents explain pipelining hazards and branch prediction, but do not contain information on Spectre speculative execution exploits.",
            key_facts=["Instruction pipelining hazards are covered", "Spectre speculative execution gadget exploit is absent"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.DISTRACTOR,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Pipeline hazards and control hazards are covered, but Spectre vulnerability speculative gadgets are absent.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-distract-03",
            subject="Operating Systems",
            topic="CPU Scheduling",
            question="How does the Linux CFS scheduler calculate real-time priority task preemptions under the SCHED_FIFO scheduling class?",
            expected_answer="The provided documents explain CFS virtual runtime for standard tasks, but do not contain information on the SCHED_FIFO real-time scheduling class.",
            key_facts=["Linux CFS virtual runtime is covered", "Real-time SCHED_FIFO scheduling is absent"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.DISTRACTOR,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="CFS virtual runtime is covered, but real-time SCHED_FIFO priority scheduling is absent.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-distract-04",
            subject="Computer Networks",
            topic="Congestion Control",
            question="Explain how TCP Slow Start calculates the initial congestion window using Cubic polynomial curve coefficients.",
            expected_answer="The provided documents explain standard TCP Slow Start and AIMD, but do not contain information regarding TCP Cubic polynomial coefficients.",
            key_facts=["TCP Slow Start and AIMD are covered", "TCP Cubic polynomial coefficients are absent"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.ADVANCED,
            category=QuestionCategory.DISTRACTOR,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="Standard TCP Slow Start and AIMD are covered, but TCP Cubic polynomial formulas are absent.",
            split=DatasetSplit.TEST
        ),

        # Conflicting / Boundary Questions (2)
        EvaluationCase(
            case_id="eval-test-conflict-01",
            subject="Cloud and Distributed Systems",
            topic="Distributed Guarantees",
            question="Can a network partition in a distributed system guarantee both strict linearizable consistency and 100% availability?",
            expected_answer="Based on Brewer's CAP theorem in the provided documents, a distributed system under network partition mathematically cannot guarantee both consistency and availability simultaneously.",
            key_facts=["CAP theorem proves simultaneous C and A under partition is impossible", "Systems must trade off consistency for availability during partitions"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONFLICTING_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="The CAP theorem in the corpus explicitly proves this combination is mathematically impossible; a grounded system must state this invariant.",
            split=DatasetSplit.TEST
        ),
        EvaluationCase(
            case_id="eval-test-conflict-02",
            subject="Theory of Computation",
            topic="Computational Complexity",
            question="Has an efficient polynomial-time algorithm O(n^2) been proven for solving all NP-complete problems?",
            expected_answer="The provided documents state that whether P = NP remains the foremost unsolved problem in theoretical computer science, and no polynomial-time algorithm is known for NP-complete problems.",
            key_facts=["P vs NP remains an unsolved problem", "No polynomial-time algorithm is known for NP-complete problems"],
            relevant_chunk_ids=[],
            difficulty=DifficultyLevel.INTERMEDIATE,
            category=QuestionCategory.CONFLICTING_EVIDENCE,
            answerability=ExpectedAnswerability.UNANSWERABLE,
            abstention_rationale="The corpus states P vs NP remains open and Cook-Levin proves SAT is NP-complete; claiming an existing polynomial solution contradicts known theoretical computer science.",
            split=DatasetSplit.TEST
        )
    ]
    cases.extend(test_cases)

    return corpus_chunks, cases

def save_benchmark_data(base_dir: str):
    os.makedirs(base_dir, exist_ok=True)
    corpus, cases = generate_curated_benchmark()

    # Save corpus
    corpus_file = os.path.join(base_dir, "corpus.json")
    with open(corpus_file, "w", encoding="utf-8") as f:
        json.dump([c.model_dump(mode="json") for c in corpus], f, indent=2)

    # Save splits
    dev_cases = [c for c in cases if c.split == DatasetSplit.DEV]
    val_cases = [c for c in cases if c.split == DatasetSplit.VAL]
    test_cases = [c for c in cases if c.split == DatasetSplit.TEST]

    with open(os.path.join(base_dir, "dev_set.json"), "w", encoding="utf-8") as f:
        json.dump([c.model_dump(mode="json") for c in dev_cases], f, indent=2)

    with open(os.path.join(base_dir, "val_set.json"), "w", encoding="utf-8") as f:
        json.dump([c.model_dump(mode="json") for c in val_cases], f, indent=2)

    with open(os.path.join(base_dir, "test_set.json"), "w", encoding="utf-8") as f:
        json.dump([c.model_dump(mode="json") for c in test_cases], f, indent=2)

    print(f"Saved {len(corpus)} corpus chunks, {len(dev_cases)} DEV cases, {len(val_cases)} VAL cases, {len(test_cases)} TEST cases (Total: {len(cases)}) to {base_dir}")

if __name__ == "__main__":
    target_dir = os.path.join(os.path.dirname(__file__), "data")
    save_benchmark_data(target_dir)
