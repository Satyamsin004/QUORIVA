-- ==============================================================================
-- Migration: V11__seed_knowledge_graph.sql
-- Description: Curated baseline dataset for CS Topic Discovery & Knowledge Graph
-- ==============================================================================

-- 1. Insert Academic Subjects
INSERT INTO academic_subjects (id, name, slug, code, description, category, icon_name, display_order)
VALUES
('a0000001-0000-0000-0000-000000000001', 'Programming Fundamentals', 'programming-fundamentals', 'CS-PROG',
 'Core principles of programming, structured code, logic, variables, control flow, functions, and algorithmic thinking.',
 'SOFTWARE_ENGINEERING', 'Code2', 1),

('a0000001-0000-0000-0000-000000000002', 'Data Structures and Algorithms', 'data-structures-and-algorithms', 'CS-DSA',
 'Fundamental structures for organizing data and efficient algorithmic problem-solving techniques for computational complexity.',
 'DATA_STRUCTURES', 'BinaryTree', 2),

('a0000001-0000-0000-0000-000000000003', 'Database Management Systems', 'database-management-systems', 'CS-DBMS',
 'Relational theory, SQL, transaction processing, indexing, query optimization, ACID semantics, and distributed storage.',
 'DATABASE_SYSTEMS', 'Database', 3),

('a0000001-0000-0000-0000-000000000004', 'Operating Systems', 'operating-systems', 'CS-OS',
 'Hardware virtualization, processes, multi-threading, CPU scheduling, synchronization, virtual memory, and file systems.',
 'OPERATING_SYSTEMS', 'Cpu', 4),

('a0000001-0000-0000-0000-000000000005', 'Computer Networks', 'computer-networks', 'CS-CN',
 'Network layers, OSI and TCP/IP protocols, IP routing, reliable transport, sockets, application protocols, and security.',
 'COMPUTER_NETWORKS', 'Network', 5),

('a0000001-0000-0000-0000-000000000006', 'Object-Oriented Software Engineering', 'software-engineering-oop', 'CS-SE',
 'Object-oriented design, encapsulation, polymorphism, inheritance, SOLID design principles, and enterprise design patterns.',
 'SOFTWARE_ENGINEERING', 'Boxes', 6),

('a0000001-0000-0000-0000-000000000007', 'Artificial Intelligence & Machine Learning', 'ai-machine-learning', 'CS-AI',
 'Statistical learning, supervised models, deep neural networks, vector embeddings, and retrieval-augmented generation (RAG).',
 'ARTIFICIAL_INTELLIGENCE', 'Sparkles', 7),

('a0000001-0000-0000-0000-000000000008', 'Cloud & Distributed Systems', 'cloud-distributed-systems', 'CS-SYS',
 'Consensus algorithms, replication, CAP theorem, distributed storage, RPC, and cloud computing architectures.',
 'CLOUD_COMPUTING', 'Cloud', 8)
ON CONFLICT (slug) DO NOTHING;

-- 2. Insert Core Knowledge Topics
INSERT INTO knowledge_topics (id, subject_id, name, slug, summary, description, difficulty_level, topic_type, estimated_minutes, status, importance_score)
VALUES
-- Programming Fundamentals (1-7)
('b0000001-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001',
 'Programming Basics', 'programming-basics',
 'Introduction to computational execution, program structure, compilers, and interpreters.',
 'Foundational understanding of how software executes on hardware. Covers compilation, bytecode interpretation, program entry points, comments, and standard input/output streams.',
 'BEGINNER', 'CONCEPT', 45, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000002', 'a0000001-0000-0000-0000-000000000001',
 'Variables and Data Types', 'variables-and-data-types',
 'Primitive data types, memory representation, variable declaration, scoping, and type casting.',
 'How primitive types (integers, floating point numbers, characters, booleans) occupy memory and are manipulated. Explores static vs dynamic typing, immutability, overflow, and type safety.',
 'BEGINNER', 'CONCEPT', 45, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000003', 'a0000001-0000-0000-0000-000000000001',
 'Control Flow and Conditionals', 'control-flow',
 'Conditional branching, iterative loops, switch statements, and loop control directives.',
 'Execution branching with if-else logic, switch cases, and loops (while, for, do-while). Includes loop invariants, break/continue semantics, and nested conditional structures.',
 'BEGINNER', 'CONCEPT', 60, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000004', 'a0000001-0000-0000-0000-000000000001',
 'Functions and Modular Design', 'functions',
 'Modular code decomposition, parameter passing mechanisms, call stack frames, and return values.',
 'Decomposing complex logic into reusable functions. Analyzes pass-by-value vs pass-by-reference, function signatures, scope boundaries, and the call stack during invocation.',
 'BEGINNER', 'CONCEPT', 60, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000005', 'a0000001-0000-0000-0000-000000000001',
 'Recursion', 'recursion',
 'Self-referential function execution, base conditions, call stack memory, and recurrence relations.',
 'The algorithmic paradigm where a function calls itself to solve smaller subproblems. Emphasizes base condition definition to prevent stack overflow, call-stack tracing, and tree recurrence visualization.',
 'INTERMEDIATE', 'ALGORITHM', 90, 'PUBLISHED', 0.95),

-- Data Structures and Algorithms (6-16)
('b0000001-0000-0000-0000-000000000006', 'a0000001-0000-0000-0000-000000000002',
 'Arrays and Memory Layout', 'arrays',
 'Contiguous memory allocation, random index access, multi-dimensional buffers, and spatial locality.',
 'Linear collection of elements allocated in contiguous memory blocks. Covers O(1) random indexing, element insertion/deletion overheads, cache friendliness, and multi-dimensional matrix operations.',
 'BEGINNER', 'DATA_STRUCTURE', 60, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000007', 'a0000001-0000-0000-0000-000000000002',
 'Strings and Character Arrays', 'strings',
 'Text encoding, string immutability, pattern matching, and string buffer optimizations.',
 'Sequence of characters represented in ASCII or UTF-8 encodings. Examines string manipulation algorithms, immutable string pools, palindromes, substrings, and builder buffers.',
 'BEGINNER', 'DATA_STRUCTURE', 45, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000008', 'a0000001-0000-0000-0000-000000000002',
 'Asymptotic Complexity Analysis', 'complexity-analysis',
 'Big-O, Big-Omega, Big-Theta notation, time complexity, and auxiliary space bounds.',
 'Formal mathematical evaluation of algorithmic efficiency. Explores worst-case, average-case, and best-case analysis, tight asymptotic bounds, and recurrence relations.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000009', 'a0000001-0000-0000-0000-000000000002',
 'Two Pointers Technique', 'two-pointers',
 'Iterating collections with dual index pointers from opposite ends or moving in sync.',
 'Efficient algorithmic pattern using two directional pointers to achieve linear time complexity O(N) instead of quadratic O(N^2) for pair searching, reversals, and partition problems.',
 'INTERMEDIATE', 'ALGORITHM', 60, 'PUBLISHED', 0.85),

('b0000001-0000-0000-0000-000000000010', 'a0000001-0000-0000-0000-000000000002',
 'Sliding Window Technique', 'sliding-window',
 'Maintaining a dynamic or fixed subarray boundary over sequential datasets.',
 'Optimization technique for subarray/substring problems that adjusts window boundaries dynamically or fixed-size, eliminating redundant recalculations and optimizing from O(N*K) to O(N).',
 'INTERMEDIATE', 'ALGORITHM', 60, 'PUBLISHED', 0.85),

('b0000001-0000-0000-0000-000000000011', 'a0000001-0000-0000-0000-000000000002',
 'Linked Lists', 'linked-lists',
 'Node pointer references, singly and doubly linked chains, and dynamic allocation.',
 'Dynamic linear structure where elements contain node references to consecutive elements. Covers fast insertions/deletions at head, slow O(N) access, cycle detection, and list reversal.',
 'INTERMEDIATE', 'DATA_STRUCTURE', 75, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000012', 'a0000001-0000-0000-0000-000000000002',
 'Stacks and Queues', 'stacks-and-queues',
 'LIFO and FIFO linear structures, monotonic stacks, priority queues, and double-ended queues.',
 'Restricted access linear data structures. Explores Stack (Last-In-First-Out) for backtracking/parsing and Queue (First-In-First-Out) for buffer management and breadth-first search.',
 'BEGINNER', 'DATA_STRUCTURE', 60, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000013', 'a0000001-0000-0000-0000-000000000002',
 'Trees and Binary Trees', 'trees',
 'Hierarchical structures, root, parent-child edges, tree height, depth, and traversal orders.',
 'Non-linear hierarchical data structure. Explores binary trees, pre-order, in-order, post-order, and level-order traversals, height calculation, and tree balance.',
 'INTERMEDIATE', 'DATA_STRUCTURE', 90, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000014', 'a0000001-0000-0000-0000-000000000002',
 'Binary Search Trees (BST)', 'binary-search-trees',
 'Ordered binary trees supporting logarithmic search, insertion, and deletion.',
 'Binary tree structure maintaining the BST invariant: left child key < node key < right child key. Covers search algorithms, self-balancing rotations (AVL / Red-Black), and floor/ceil queries.',
 'INTERMEDIATE', 'DATA_STRUCTURE', 75, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000015', 'a0000001-0000-0000-0000-000000000002',
 'Graphs and Graph Traversals', 'graphs',
 'Vertices, directed/undirected edges, adjacency matrices, BFS, and DFS traversals.',
 'Generalized network data structure representing relationships between vertices. Explores Breadth-First Search (BFS) for shortest path in unweighted graphs and Depth-First Search (DFS) for connected components.',
 'ADVANCED', 'DATA_STRUCTURE', 105, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000016', 'a0000001-0000-0000-0000-000000000002',
 'Dynamic Programming', 'dynamic-programming',
 'Optimal substructure, overlapping subproblems, memoization, and bottom-up tabulation.',
 'Algorithmic optimization paradigm for multistage decision problems. Analyzes state transitions, top-down memoization, bottom-up tabulation, space reduction, and knapsack/longest subsequence formulations.',
 'ADVANCED', 'ALGORITHM', 120, 'PUBLISHED', 1.00),

-- Database Management Systems (17-23)
('b0000001-0000-0000-0000-000000000017', 'a0000001-0000-0000-0000-000000000003',
 'Database Fundamentals', 'db-fundamentals',
 'Data persistence, DBMS vs file systems, data independence, schemas, and instances.',
 'Core concepts of structured persistence. Covers physical vs logical data independence, 3-tier architecture, data models, and database management system engines.',
 'BEGINNER', 'CONCEPT', 45, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000018', 'a0000001-0000-0000-0000-000000000003',
 'Relational Model and Algebra', 'relational-model',
 'Relations, tuples, primary/foreign keys, integrity constraints, and relational operators.',
 'Formal mathematical foundation of relational databases. Explains selection, projection, Cartesian product, joins, set operations, and referential integrity constraints.',
 'INTERMEDIATE', 'PARADIGM', 60, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000019', 'a0000001-0000-0000-0000-000000000003',
 'SQL & Complex Joins', 'sql-joins',
 'DDL, DML, DQL, inner/outer joins, aggregation, grouping, subqueries, and window functions.',
 'Declarative database querying language. Covers relational joins (INNER, LEFT, RIGHT, FULL OUTER), GROUP BY aggregation, subqueries, CTE expressions, and analytical window functions.',
 'INTERMEDIATE', 'CONCEPT', 90, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000020', 'a0000001-0000-0000-0000-000000000003',
 'Normalization & Functional Dependencies', 'normalization',
 'Anomalies, Armstrong axioms, 1NF, 2NF, 3NF, BCNF decomposition, and dependency preservation.',
 'Systematic schema refinement to eliminate insertion, update, and deletion anomalies. Analyzes functional dependency closure, lossless joins, and canonical normal forms.',
 'ADVANCED', 'CONCEPT', 90, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000021', 'a0000001-0000-0000-0000-000000000003',
 'Transactions and ACID Properties', 'transactions-acid',
 'Atomicity, Consistency, Isolation, Durability, write-ahead logging, and crash recovery.',
 'Foundations of reliable database transactions. Explores atomic commits, consistency constraints, isolation anomalies (dirty reads, non-repeatable reads, phantom reads), and WAL durability.',
 'ADVANCED', 'CONCEPT', 75, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000022', 'a0000001-0000-0000-0000-000000000003',
 'Concurrency Control & 2PL', 'concurrency-control',
 'Two-Phase Locking (2PL), serializability, timestamp ordering, MVCC, and deadlock resolution.',
 'Techniques for coordinating concurrent transaction executions safely. Covers conflict serializability, shared/exclusive lock protocols, multi-version concurrency control (MVCC), and deadlock graphs.',
 'ADVANCED', 'CONCEPT', 90, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000023', 'a0000001-0000-0000-0000-000000000003',
 'Database Indexing & B-Trees', 'indexing-query-optimization',
 'B-Tree, B+Tree indexes, Hash indexes, clustered vs non-clustered, and cost-based query plans.',
 'Underlying storage engine access paths for rapid record lookup. Detailed analysis of B+Tree node branching, index scans, selectivity, composite indexes, and EXPLAIN query plan analysis.',
 'ADVANCED', 'DATA_STRUCTURE', 90, 'PUBLISHED', 0.95),

-- Operating Systems (24-30)
('b0000001-0000-0000-0000-000000000024', 'a0000001-0000-0000-0000-000000000004',
 'Process Management & Context Switching', 'process-management',
 'Process Control Block (PCB), process states, fork/exec semantics, context switches, and signals.',
 'Core unit of OS abstraction. Explores process lifecycle states (New, Ready, Running, Waiting, Terminated), PCB register state saving, context switch latency, and inter-process communication.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000025', 'a0000001-0000-0000-0000-000000000004',
 'Threads & Concurrency Models', 'threads-concurrency',
 'User vs kernel threads, thread pools, race conditions, shared memory, and asynchronous tasks.',
 'Lightweight units of execution within a shared address space. Compares multi-threading architectures (1:1, M:N), thread safety, race conditions, and synchronization overhead.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000026', 'a0000001-0000-0000-0000-000000000004',
 'CPU Scheduling Algorithms', 'cpu-scheduling',
 'Preemptive and non-preemptive scheduling: FCFS, SJF, Round Robin, Multi-level Feedback Queues.',
 'Strategies for allocating CPU cores to ready processes. Evaluates turnaround time, waiting time, response time, starvation, and priority inversion.',
 'INTERMEDIATE', 'ALGORITHM', 60, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000027', 'a0000001-0000-0000-0000-000000000004',
 'Process Synchronization & Semaphores', 'process-synchronization',
 'Critical section problem, Peterson algorithm, mutexes, semaphores, monitors, and barriers.',
 'Coordinating concurrent process execution to avoid data corruption. Details mutual exclusion, progress, bounded waiting, counting/binary semaphores, and classic producer-consumer problems.',
 'ADVANCED', 'CONCEPT', 90, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000028', 'a0000001-0000-0000-0000-000000000004',
 'Deadlocks & Banker Algorithm', 'deadlocks',
 'Coffman conditions, resource allocation graphs, deadlock prevention, detection, and avoidance.',
 'Conditions causing indefinite process stalls. Analyzes mutual exclusion, hold-and-wait, no-preemption, circular wait, and safe state evaluation using Dijkstra Banker algorithm.',
 'ADVANCED', 'CONCEPT', 75, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000029', 'a0000001-0000-0000-0000-000000000004',
 'Memory Management & Fragmentation', 'memory-management',
 'Contiguous memory allocation, logical vs physical address spaces, MMU, and internal/external fragmentation.',
 'Hardware memory virtualization. Analyzes base and limit registers, dynamic address translation, memory compaction, and allocation strategies (First-fit, Best-fit, Worst-fit).',
 'INTERMEDIATE', 'CONCEPT', 60, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000030', 'a0000001-0000-0000-0000-000000000004',
 'Virtual Memory & Paging', 'virtual-memory-paging',
 'Page tables, Translation Lookaside Buffer (TLB), page faults, replacement policies (LRU, FIFO), and thrashing.',
 'Virtual address space mapped to physical frames. Covers multi-level page tables, TLB hits/misses, inverted tables, page fault service routines, and working set models against thrashing.',
 'ADVANCED', 'ARCHITECTURE', 90, 'PUBLISHED', 0.95),

-- Computer Networks (31-36)
('b0000001-0000-0000-0000-000000000031', 'a0000001-0000-0000-0000-000000000005',
 'Computer Networks Fundamentals', 'network-fundamentals',
 'Network topologies, packet switching vs circuit switching, bandwidth, latency, and throughput.',
 'Introductory concepts of distributed node communication. Analyzes point-to-point connections, broadcast mediums, store-and-forward delay, transmission, propagation, and queueing overheads.',
 'BEGINNER', 'CONCEPT', 45, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000032', 'a0000001-0000-0000-0000-000000000005',
 'OSI Model and TCP/IP Architecture', 'osi-tcpip-model',
 'Layered protocols, encapsulation, de-encapsulation, PDU, and comparing OSI 7-layer with TCP/IP 4-layer.',
 'Formal architectural abstraction layers. Details physical, data-link, network, transport, and application protocol responsibilities, packet headers, and encapsulation lifecycles.',
 'BEGINNER', 'ARCHITECTURE', 60, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000033', 'a0000001-0000-0000-0000-000000000005',
 'IP Addressing & Subnetting', 'ip-addressing',
 'IPv4, IPv6, CIDR notation, subnet masks, ARP, NAT, and default gateways.',
 'Network-layer logical addressing. Explores classless inter-domain routing (CIDR), network/host bit partitioning, broadcast domains, Address Resolution Protocol (ARP), and private network NAT.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000034', 'a0000001-0000-0000-0000-000000000005',
 'TCP & UDP Transport Protocols', 'tcp-udp-transport',
 'Three-way handshake, reliable delivery, sliding window flow control, congestion control (AIMD), and UDP datagrams.',
 'Transport layer reliability guarantees. Detailed examination of sequence/acknowledgement numbers, connection teardown (FIN/ACK), flow control window, and congestion windows (Slow Start, Fast Retransmit).',
 'INTERMEDIATE', 'CONCEPT', 90, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000035', 'a0000001-0000-0000-0000-000000000005',
 'DNS and HTTP/HTTPS Protocols', 'dns-http-https',
 'Domain name resolution, HTTP methods, headers, status codes, cookies, and TLS/SSL certificate encryption.',
 'Core application layer web protocols. Details recursive/iterative DNS queries, HTTP/1.1 keep-alive vs HTTP/2 multiplexing, REST semantics, and TLS asymmetric/symmetric session handshakes.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000036', 'a0000001-0000-0000-0000-000000000005',
 'Network Security & Firewalls', 'network-security',
 'Symmetric/asymmetric cryptography, firewalls, packet filtering, DDoS attacks, and VPN tunnels.',
 'Defensive networking techniques. Covers public-key infrastructure (PKI), stateful packet inspection firewalls, SSL/TLS, port scanning, and threat mitigations.',
 'ADVANCED', 'CONCEPT', 90, 'PUBLISHED', 0.90),

-- Object-Oriented Software Engineering (37-40)
('b0000001-0000-0000-0000-000000000037', 'a0000001-0000-0000-0000-000000000006',
 'Object-Oriented Programming Core', 'oop-core',
 'Classes, objects, constructors, encapsulation, access modifiers, and state management.',
 'Object-oriented programming paradigm fundamentals. Covers class blueprint definition, heap object instantiation, constructors, getters/setters, and access encapsulation boundaries.',
 'BEGINNER', 'PARADIGM', 60, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000038', 'a0000001-0000-0000-0000-000000000006',
 'Inheritance and Polymorphism', 'inheritance-polymorphism',
 'Subclassing, method overriding vs overloading, dynamic dispatch, and abstract interfaces.',
 'Code reuse and polymorphic runtime flexibility. Analyzes "is-a" vs "has-a" relationships, virtual method tables (vtable), dynamic binding, interface contracts, and the diamond inheritance problem.',
 'INTERMEDIATE', 'PARADIGM', 75, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000039', 'a0000001-0000-0000-0000-000000000006',
 'SOLID Design Principles', 'solid-principles',
 'Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion.',
 'Five essential principles for modular, maintainable object-oriented software design. Explains how adhering to SOLID avoids brittle, tightly coupled codebases and enables seamless extension.',
 'ADVANCED', 'PARADIGM', 90, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000040', 'a0000001-0000-0000-0000-000000000006',
 'Software Design Patterns', 'design-patterns',
 'Creational, Structural, and Behavioral patterns: Factory, Singleton, Strategy, Observer, Adapter.',
 'Proven reusable solutions to recurring software engineering problems. In-depth analysis of Singleton synchronization, Factory abstractions, Strategy polymorphism, and Observer event distribution.',
 'ADVANCED', 'PARADIGM', 105, 'PUBLISHED', 0.95),

-- AI & Machine Learning (41-45)
('b0000001-0000-0000-0000-000000000041', 'a0000001-0000-0000-0000-000000000007',
 'Machine Learning Fundamentals', 'ml-fundamentals',
 'Features, labels, training vs inference, loss functions, gradient descent, and bias-variance tradeoff.',
 'Statistical learning fundamentals. Covers mathematical formulation of optimization, MSE and Cross-Entropy loss, learning rates, underfitting/overfitting, and cross-validation techniques.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000042', 'a0000001-0000-0000-0000-000000000007',
 'Supervised vs Unsupervised Learning', 'supervised-learning',
 'Linear regression, logistic classification, decision trees, k-means clustering, and PCA.',
 'Core machine learning paradigms. Details continuous regression vs discrete classification, tree splitting criteria (Gini, entropy), centroid-based clustering, and dimensionality reduction.',
 'INTERMEDIATE', 'ALGORITHM', 90, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000043', 'a0000001-0000-0000-0000-000000000007',
 'Neural Networks & Deep Learning', 'neural-networks',
 'Perceptrons, activation functions (ReLU, Sigmoid), backpropagation, matrix math, and deep networks.',
 'Multi-layer artificial neural networks. Details forward pass matrix transformations, non-linear activation functions, chain rule backpropagation, vanishing gradients, and GPU tensor acceleration.',
 'ADVANCED', 'ARCHITECTURE', 120, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000044', 'a0000001-0000-0000-0000-000000000007',
 'Vector Embeddings & Semantic Search', 'vector-embeddings',
 'Dense vector projections, cosine similarity, pgvector, HNSW indexing, and semantic similarity.',
 'Mapping textual and conceptual data into high-dimensional geometric spaces where semantic proximity correlates with angular or Euclidean distance. Explores embedding models and vector indexing.',
 'ADVANCED', 'CONCEPT', 90, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000045', 'a0000001-0000-0000-0000-000000000007',
 'Retrieval-Augmented Generation (RAG)', 'rag-systems',
 'Grounded LLM context injection, chunking strategies, hybrid retrieval, cross-encoders, and citations.',
 'Advanced AI paradigm combining real-time document retrieval with large language models to produce hallucination-resistant, verified, source-grounded answers with verifiable citations.',
 'ADVANCED', 'ARCHITECTURE', 105, 'PUBLISHED', 1.00)
ON CONFLICT (slug) DO NOTHING;

-- 3. Insert Topic Learning Objectives
INSERT INTO knowledge_topic_learning_objectives (topic_id, objective, display_order)
VALUES
-- Programming Basics
('b0000001-0000-0000-0000-000000000001', 'Explain the difference between compiled and interpreted execution models.', 1),
('b0000001-0000-0000-0000-000000000001', 'Trace the entry point and execution lifecycle of a console application.', 2),

-- Variables & Data Types
('b0000001-0000-0000-0000-000000000002', 'Differentiate between primitive value types and reference types in memory.', 1),
('b0000001-0000-0000-0000-000000000002', 'Analyze precision limits and numeric overflow hazards.', 2),

-- Control Flow
('b0000001-0000-0000-0000-000000000003', 'Construct complex multi-branch decision trees using if-else and switch statements.', 1),
('b0000001-0000-0000-0000-000000000003', 'Demonstrate loop termination invariants to prevent infinite execution cycles.', 2),

-- Functions
('b0000001-0000-0000-0000-000000000004', 'Trace stack frame allocation, parameter passing, and return address management.', 1),
('b0000001-0000-0000-0000-000000000004', 'Design modular functions following single-responsibility and clean interface guidelines.', 2),

-- Recursion
('b0000001-0000-0000-0000-000000000005', 'Identify and formulate valid base conditions for recursive decomposition.', 1),
('b0000001-0000-0000-0000-000000000005', 'Calculate call stack memory complexity and identify potential stack overflow scenarios.', 2),

-- Arrays
('b0000001-0000-0000-0000-000000000006', 'Explain why index-based lookup in arrays provides deterministic O(1) time complexity.', 1),
('b0000001-0000-0000-0000-000000000006', 'Evaluate memory overhead and data shifting required for insertion and deletion.', 2),

-- Complexity Analysis
('b0000001-0000-0000-0000-000000000008', 'Formulate asymptotic Big-O bounds for iterative and nested algorithmic patterns.', 1),
('b0000001-0000-0000-0000-000000000008', 'Distinguish time complexity from auxiliary space complexity.', 2),

-- Linked Lists
('b0000001-0000-0000-0000-000000000011', 'Implement node pointer manipulation without memory leaks or dangling references.', 1),
('b0000001-0000-0000-0000-000000000011', 'Apply Floyd Cycle Detection algorithm to find cycles in linked structures.', 2),

-- Trees
('b0000001-0000-0000-0000-000000000013', 'Implement pre-order, in-order, post-order, and breadth-first level traversals.', 1),
('b0000001-0000-0000-0000-000000000013', 'Compute tree depth, height, and maximum path sums recursively.', 2),

-- Dynamic Programming
('b0000001-0000-0000-0000-000000000016', 'Identify overlapping subproblems and optimal substructure properties in problems.', 1),
('b0000001-0000-0000-0000-000000000016', 'Convert exponential recursive formulations into polynomial bottom-up tabular DP solutions.', 2),

-- Transactions & ACID
('b0000001-0000-0000-0000-000000000021', 'Define each of the ACID guarantees and explain how write-ahead logging delivers durability.', 1),
('b0000001-0000-0000-0000-000000000021', 'Analyze dirty read, non-repeatable read, and phantom read anomalies across isolation levels.', 2),

-- RAG Systems
('b0000001-0000-0000-0000-000000000045', 'Design a hybrid retrieval pipeline combining dense vector embeddings and BM25/FTS.', 1),
('b0000001-0000-0000-0000-000000000045', 'Implement citation validation to ensure LLM generated statements strictly match source chunks.', 2);

-- 4. Insert Topic Tags
INSERT INTO knowledge_topic_tags (topic_id, tag)
VALUES
('b0000001-0000-0000-0000-000000000001', 'syntax'),
('b0000001-0000-0000-0000-000000000001', 'compilation'),
('b0000001-0000-0000-0000-000000000002', 'primitives'),
('b0000001-0000-0000-0000-000000000002', 'memory'),
('b0000001-0000-0000-0000-000000000003', 'loops'),
('b0000001-0000-0000-0000-000000000003', 'branching'),
('b0000001-0000-0000-0000-000000000004', 'callstack'),
('b0000001-0000-0000-0000-000000000004', 'modularity'),
('b0000001-0000-0000-0000-000000000005', 'recursion'),
('b0000001-0000-0000-0000-000000000005', 'callstack'),
('b0000001-0000-0000-0000-000000000006', 'arrays'),
('b0000001-0000-0000-0000-000000000006', 'memory-layout'),
('b0000001-0000-0000-0000-000000000008', 'big-o'),
('b0000001-0000-0000-0000-000000000008', 'complexity'),
('b0000001-0000-0000-0000-000000000009', 'two-pointers'),
('b0000001-0000-0000-0000-000000000010', 'sliding-window'),
('b0000001-0000-0000-0000-000000000011', 'linked-list'),
('b0000001-0000-0000-0000-000000000012', 'stack'),
('b0000001-0000-0000-0000-000000000012', 'queue'),
('b0000001-0000-0000-0000-000000000013', 'trees'),
('b0000001-0000-0000-0000-000000000014', 'bst'),
('b0000001-0000-0000-0000-000000000015', 'graphs'),
('b0000001-0000-0000-0000-000000000016', 'dp'),
('b0000001-0000-0000-0000-000000000016', 'memoization'),
('b0000001-0000-0000-0000-000000000019', 'sql'),
('b0000001-0000-0000-0000-000000000021', 'acid'),
('b0000001-0000-0000-0000-000000000021', 'transactions'),
('b0000001-0000-0000-0000-000000000023', 'b-tree'),
('b0000001-0000-0000-0000-000000000025', 'concurrency'),
('b0000001-0000-0000-0000-000000000030', 'paging'),
('b0000001-0000-0000-0000-000000000034', 'tcp'),
('b0000001-0000-0000-0000-000000000039', 'solid'),
('b0000001-0000-0000-0000-000000000040', 'patterns'),
('b0000001-0000-0000-0000-000000000044', 'embeddings'),
('b0000001-0000-0000-0000-000000000045', 'rag')
ON CONFLICT (topic_id, tag) DO NOTHING;

-- 5. Insert Curated Knowledge Topic Relationships (PREREQUISITE_OF and RELATED_TO)
-- Format: (source_topic_id, target_topic_id, relationship_type, description)
-- Semantics: "source_topic is PREREQUISITE_OF target_topic" (Learn source BEFORE target)
INSERT INTO knowledge_topic_relationships (source_topic_id, target_topic_id, relationship_type, description, weight)
VALUES
-- Programming Basics -> Variables -> Control Flow -> Functions -> Recursion
('b0000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000002', 'PREREQUISITE_OF', 'Must understand execution lifecycle before declaring variables.', 1.0),
('b0000001-0000-0000-0000-000000000002', 'b0000001-0000-0000-0000-000000000003', 'PREREQUISITE_OF', 'Control flow logic operates on primitive variables and boolean expressions.', 1.0),
('b0000001-0000-0000-0000-000000000003', 'b0000001-0000-0000-0000-000000000004', 'PREREQUISITE_OF', 'Function body implementation requires loops and conditional statements.', 1.0),
('b0000001-0000-0000-0000-000000000004', 'b0000001-0000-0000-0000-000000000005', 'PREREQUISITE_OF', 'Recursion relies on stack-frame function invocation semantics.', 1.0),

-- Control Flow -> Arrays -> Strings
('b0000001-0000-0000-0000-000000000003', 'b0000001-0000-0000-0000-000000000006', 'PREREQUISITE_OF', 'Array traversal requires loops and bounds checking.', 1.0),
('b0000001-0000-0000-0000-000000000006', 'b0000001-0000-0000-0000-000000000007', 'PREREQUISITE_OF', 'Strings are represented and indexed as sequential character arrays.', 1.0),

-- Arrays -> Complexity Analysis
('b0000001-0000-0000-0000-000000000006', 'b0000001-0000-0000-0000-000000000008', 'PREREQUISITE_OF', 'Concrete operations on arrays illustrate constant vs linear time analysis.', 0.9),

-- Arrays -> Two Pointers -> Sliding Window
('b0000001-0000-0000-0000-000000000006', 'b0000001-0000-0000-0000-000000000009', 'PREREQUISITE_OF', 'Two pointers technique requires indexed array access.', 1.0),
('b0000001-0000-0000-0000-000000000009', 'b0000001-0000-0000-0000-000000000010', 'PREREQUISITE_OF', 'Sliding window expands on two-pointer boundary manipulation.', 1.0),

-- Arrays & Variables -> Linked Lists -> Stacks and Queues
('b0000001-0000-0000-0000-000000000006', 'b0000001-0000-0000-0000-000000000011', 'PREREQUISITE_OF', 'Understanding contiguous array limitations motivates dynamic linked lists.', 0.9),
('b0000001-0000-0000-0000-000000000011', 'b0000001-0000-0000-0000-000000000012', 'PREREQUISITE_OF', 'Stacks and queues are implemented cleanly via linked list nodes or dynamic arrays.', 1.0),

-- Recursion & Linked Lists -> Trees -> BST
('b0000001-0000-0000-0000-000000000005', 'b0000001-0000-0000-0000-000000000013', 'PREREQUISITE_OF', 'Tree traversals and property evaluations are inherently recursive.', 1.0),
('b0000001-0000-0000-0000-000000000011', 'b0000001-0000-0000-0000-000000000013', 'PREREQUISITE_OF', 'Node reference semantics form the structural basis of tree pointers.', 0.9),
('b0000001-0000-0000-0000-000000000013', 'b0000001-0000-0000-0000-000000000014', 'PREREQUISITE_OF', 'Binary Search Trees enforce specific key order invariants on binary trees.', 1.0),

-- Trees & Stacks/Queues -> Graphs
('b0000001-0000-0000-0000-000000000013', 'b0000001-0000-0000-0000-000000000015', 'PREREQUISITE_OF', 'Trees are restricted acyclic connected graphs; understanding them is prerequisite to general graphs.', 1.0),
('b0000001-0000-0000-0000-000000000012', 'b0000001-0000-0000-0000-000000000015', 'PREREQUISITE_OF', 'BFS and DFS graph traversals require Queues and Stacks.', 1.0),

-- Recursion & Arrays & Complexity Analysis -> Dynamic Programming
('b0000001-0000-0000-0000-000000000005', 'b0000001-0000-0000-0000-000000000016', 'PREREQUISITE_OF', 'DP memoization is built directly on top of recursive subproblem decomposition.', 1.0),
('b0000001-0000-0000-0000-000000000006', 'b0000001-0000-0000-0000-000000000016', 'PREREQUISITE_OF', 'DP tabulation stores subproblem solutions in 1D/2D arrays.', 1.0),
('b0000001-0000-0000-0000-000000000008', 'b0000001-0000-0000-0000-000000000016', 'PREREQUISITE_OF', 'Complexity analysis is necessary to evaluate exponential to polynomial speedup.', 0.9),

-- Database Track: DB Fundamentals -> Relational Model -> SQL -> Normalization -> ACID -> Concurrency -> Indexing
('b0000001-0000-0000-0000-000000000017', 'b0000001-0000-0000-0000-000000000018', 'PREREQUISITE_OF', 'Relational model formalizes the general database persistence concepts.', 1.0),
('b0000001-0000-0000-0000-000000000018', 'b0000001-0000-0000-0000-000000000019', 'PREREQUISITE_OF', 'SQL is the declarative implementation of relational algebra.', 1.0),
('b0000001-0000-0000-0000-000000000018', 'b0000001-0000-0000-0000-000000000020', 'PREREQUISITE_OF', 'Functional dependencies and normalization evaluate relations.', 1.0),
('b0000001-0000-0000-0000-000000000019', 'b0000001-0000-0000-0000-000000000021', 'PREREQUISITE_OF', 'Executing queries leads to multi-statement transactional semantics.', 0.9),
('b0000001-0000-0000-0000-000000000021', 'b0000001-0000-0000-0000-000000000022', 'PREREQUISITE_OF', 'Isolation in ACID requires concurrency control protocols.', 1.0),
('b0000001-0000-0000-0000-000000000014', 'b0000001-0000-0000-0000-000000000023', 'PREREQUISITE_OF', 'B-Tree database indexes are multi-way self-balancing search trees.', 1.0),
('b0000001-0000-0000-0000-000000000019', 'b0000001-0000-0000-0000-000000000023', 'PREREQUISITE_OF', 'Understanding SQL query patterns is required for effective index design.', 0.9),

-- Operating Systems Track: Process -> Threads -> Scheduling & Sync -> Deadlocks -> Memory -> Virtual Memory
('b0000001-0000-0000-0000-000000000024', 'b0000001-0000-0000-0000-000000000025', 'PREREQUISITE_OF', 'Threads are lightweight execution entities inside a parent process.', 1.0),
('b0000001-0000-0000-0000-000000000024', 'b0000001-0000-0000-0000-000000000026', 'PREREQUISITE_OF', 'CPU scheduler selects among ready processes in the process table.', 1.0),
('b0000001-0000-0000-0000-000000000025', 'b0000001-0000-0000-0000-000000000027', 'PREREQUISITE_OF', 'Shared memory across threads necessitates synchronization and critical sections.', 1.0),
('b0000001-0000-0000-0000-000000000027', 'b0000001-0000-0000-0000-000000000028', 'PREREQUISITE_OF', 'Lock acquisition ordering can trigger deadlocks.', 1.0),
('b0000001-0000-0000-0000-000000000024', 'b0000001-0000-0000-0000-000000000029', 'PREREQUISITE_OF', 'Process address spaces require physical memory allocation.', 1.0),
('b0000001-0000-0000-0000-000000000029', 'b0000001-0000-0000-0000-000000000030', 'PREREQUISITE_OF', 'Contiguous memory limitations motivate non-contiguous virtual paging.', 1.0),

-- Computer Networks Track: Fundamentals -> OSI/TCP/IP -> IP Addressing -> Transport -> DNS/HTTP -> Security
('b0000001-0000-0000-0000-000000000031', 'b0000001-0000-0000-0000-000000000032', 'PREREQUISITE_OF', 'General networking concepts ground the layered architecture.', 1.0),
('b0000001-0000-0000-0000-000000000032', 'b0000001-0000-0000-0000-000000000033', 'PREREQUISITE_OF', 'Network layer packet routing requires IP addressing.', 1.0),
('b0000001-0000-0000-0000-000000000033', 'b0000001-0000-0000-0000-000000000034', 'PREREQUISITE_OF', 'Transport layer protocols encapsulate IP host packets into port conversations.', 1.0),
('b0000001-0000-0000-0000-000000000034', 'b0000001-0000-0000-0000-000000000035', 'PREREQUISITE_OF', 'HTTP/HTTPS operates over reliable TCP transport connections.', 1.0),
('b0000001-0000-0000-0000-000000000035', 'b0000001-0000-0000-0000-000000000036', 'PREREQUISITE_OF', 'Web application communications require cryptographic transport protection.', 0.9),

-- Software Engineering Track: Functions -> OOP Core -> Inheritance -> SOLID -> Design Patterns
('b0000001-0000-0000-0000-000000000004', 'b0000001-0000-0000-0000-000000000037', 'PREREQUISITE_OF', 'Classes encapsulate functions (methods) and variables (state).', 1.0),
('b0000001-0000-0000-0000-000000000037', 'b0000001-0000-0000-0000-000000000038', 'PREREQUISITE_OF', 'Inheritance and polymorphism build on class definition fundamentals.', 1.0),
('b0000001-0000-0000-0000-000000000038', 'b0000001-0000-0000-0000-000000000039', 'PREREQUISITE_OF', 'SOLID principles establish best practices for polymorphic class hierarchies.', 1.0),
('b0000001-0000-0000-0000-000000000039', 'b0000001-0000-0000-0000-000000000040', 'PREREQUISITE_OF', 'Design patterns leverage SOLID principles for robust architecture.', 1.0),

-- AI & ML Track: Arrays & Complexity -> ML Fundamentals -> Supervised -> Neural Networks -> Embeddings -> RAG
('b0000001-0000-0000-0000-000000000006', 'b0000001-0000-0000-0000-000000000041', 'PREREQUISITE_OF', 'Features and weights are manipulated as multidimensional matrix arrays.', 1.0),
('b0000001-0000-0000-0000-000000000041', 'b0000001-0000-0000-0000-000000000042', 'PREREQUISITE_OF', 'Supervised learning formalizes objective optimization with labeled datasets.', 1.0),
('b0000001-0000-0000-0000-000000000042', 'b0000001-0000-0000-0000-000000000043', 'PREREQUISITE_OF', 'Neural networks extend linear models into deep layered architectures.', 1.0),
('b0000001-0000-0000-0000-000000000043', 'b0000001-0000-0000-0000-000000000044', 'PREREQUISITE_OF', 'Dense vector representations are generated via deep neural encoders.', 1.0),
('b0000001-0000-0000-0000-000000000044', 'b0000001-0000-0000-0000-000000000045', 'PREREQUISITE_OF', 'RAG retrieval depends on vector embeddings and similarity search.', 1.0),

-- RELATED_TO Relationships (Bidirectional conceptual relevance)
('b0000001-0000-0000-0000-000000000005', 'b0000001-0000-0000-0000-000000000012', 'RELATED_TO', 'Function call stacks operate strictly as LIFO stacks.', 0.8),
('b0000001-0000-0000-0000-000000000014', 'b0000001-0000-0000-0000-000000000023', 'RELATED_TO', 'B-Tree database indexes adapt binary search tree concepts for disk blocks.', 0.9),
('b0000001-0000-0000-0000-000000000022', 'b0000001-0000-0000-0000-000000000027', 'RELATED_TO', 'Database lock managers share concurrency primitives with OS semaphores.', 0.85),
('b0000001-0000-0000-0000-000000000028', 'b0000001-0000-0000-0000-000000000022', 'RELATED_TO', 'Transaction deadlocks in databases mirror OS deadlock conditions.', 0.85),
('b0000001-0000-0000-0000-000000000023', 'b0000001-0000-0000-0000-000000000044', 'RELATED_TO', 'Modern vector databases adapt traditional indexing structures for embeddings.', 0.8)
ON CONFLICT (source_topic_id, target_topic_id, relationship_type) DO NOTHING;

