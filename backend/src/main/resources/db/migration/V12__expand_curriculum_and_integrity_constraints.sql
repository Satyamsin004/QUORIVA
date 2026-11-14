-- ==============================================================================
-- Migration: V12__expand_curriculum_and_integrity_constraints.sql
-- Description: Prerequisite cycle prevention trigger, expanded curriculum subjects,
--              and topics (Architecture, Compilers, Theory of Computation, Security, Web).
-- ==============================================================================

-- 1. Database-Level Prerequisite Cycle Prevention Function & Trigger
CREATE OR REPLACE FUNCTION check_prerequisite_cycle()
RETURNS TRIGGER AS $$
BEGIN
    -- Only check cycles for PREREQUISITE_OF relationships
    IF NEW.relationship_type = 'PREREQUISITE_OF' THEN
        -- Prevent self-reference
        IF NEW.source_topic_id = NEW.target_topic_id THEN
            RAISE EXCEPTION 'Prerequisite cycle detected: Topic cannot be a prerequisite of itself (topic_id: %)', NEW.source_topic_id;
        END IF;

        -- Acquire transaction-level advisory lock to serialize concurrent edge additions
        PERFORM pg_advisory_xact_lock(hashtext('knowledge_prerequisite_cycle_check'));

        -- Verify if target_topic_id can already reach source_topic_id via existing PREREQUISITE_OF edges
        -- Adding edge A -> B (A is prerequisite of B) is invalid if B can already reach A (B -> ... -> A)
        IF EXISTS (
            WITH RECURSIVE reachable AS (
                SELECT target_topic_id AS node
                FROM knowledge_topic_relationships
                WHERE source_topic_id = NEW.target_topic_id
                  AND relationship_type = 'PREREQUISITE_OF'
                UNION
                SELECT r.target_topic_id AS node
                FROM knowledge_topic_relationships r
                JOIN reachable ON reachable.node = r.source_topic_id
                WHERE r.relationship_type = 'PREREQUISITE_OF'
            )
            SELECT 1 FROM reachable WHERE node = NEW.source_topic_id LIMIT 1
        ) THEN
            RAISE EXCEPTION 'Prerequisite cycle detected: Adding relationship from % to % creates a circular prerequisite dependency',
                NEW.source_topic_id, NEW.target_topic_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_prerequisite_cycle ON knowledge_topic_relationships;

CREATE TRIGGER trg_prevent_prerequisite_cycle
BEFORE INSERT OR UPDATE ON knowledge_topic_relationships
FOR EACH ROW
EXECUTE FUNCTION check_prerequisite_cycle();

-- 2. Insert Missing Academic Subjects
INSERT INTO academic_subjects (id, name, slug, code, description, category, icon_name, display_order)
VALUES
('a0000001-0000-0000-0000-000000000009', 'Computer Organization and Architecture', 'computer-organization-and-architecture', 'CS-ARCH',
 'Digital logic, CPU microarchitecture, instruction set architectures (ISA), memory hierarchy, cache coherency, pipelining, and bus interfaces.',
 'SYSTEMS', 'Cpu', 9),

('a0000001-0000-0000-0000-000000000010', 'Compiler Design', 'compiler-design', 'CS-COMP',
 'Phases of compilation, lexical tokenization, context-free grammars, LL and LR parsing, intermediate representations (AST, TAC, SSA), optimization, and machine code generation.',
 'THEORY_AND_SYSTEMS', 'FileCode', 10),

('a0000001-0000-0000-0000-000000000011', 'Theory of Computation', 'theory-of-computation', 'CS-TOC',
 'Formal languages, finite state automata, context-free languages, pushdown automata, Turing machines, decidability, the Halting Problem, and computational complexity (P vs NP).',
 'THEORETICAL_CS', 'BinaryTree', 11),

('a0000001-0000-0000-0000-000000000012', 'Cybersecurity & Cryptography', 'cybersecurity-cryptography', 'CS-SEC',
 'Confidentiality, integrity, availability, symmetric and asymmetric ciphers, public-key infrastructure, secure authentication protocols, OWASP application security, and network defenses.',
 'CYBERSECURITY', 'Shield', 12),

('a0000001-0000-0000-0000-000000000013', 'Web Development & Systems', 'web-development-systems', 'CS-WEB',
 'Web architecture, client-server models, HTTP protocols, DOM rendering engines, browser event loops, reactive UI frameworks, and server-side rendering.',
 'SOFTWARE_ENGINEERING', 'Globe', 13)
ON CONFLICT (slug) DO NOTHING;

-- 3. Insert Curated Knowledge Topics for New Subjects
INSERT INTO knowledge_topics (id, subject_id, name, slug, summary, description, difficulty_level, topic_type, estimated_minutes, status, importance_score)
VALUES
-- Computer Organization and Architecture (46-49)
('b0000001-0000-0000-0000-000000000046', 'a0000001-0000-0000-0000-000000000009',
 'Digital Logic and Boolean Algebra', 'digital-logic-boolean-algebra',
 'Fundamental logic gates, Boolean algebraic simplification, combinational circuits, and sequential memory elements.',
 'Covers truth tables, Karnaugh maps, De Morgan laws, multiplexers, decoders, full adders, latches, and master-slave flip-flops.',
 'BEGINNER', 'CONCEPT', 60, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000047', 'a0000001-0000-0000-0000-000000000009',
 'CPU Datapath and Instruction Sets', 'cpu-datapath-instruction-sets',
 'Von Neumann datapath, ALU, program counter, register file, instruction cycle (fetch, decode, execute), and RISC vs CISC ISAs.',
 'Analyzes control unit micro-operations, register transfers, addressing modes, and contrasts load-store RISC architectures with microcode-driven CISC designs.',
 'INTERMEDIATE', 'ARCHITECTURE', 90, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000048', 'a0000001-0000-0000-0000-000000000009',
 'Instruction Pipelining and Hazards', 'instruction-pipelining-hazards',
 'Multi-stage execution pipelining, throughput speedup, structural, data (RAW, WAR, WAW), and control hazards, and branch prediction.',
 'Details classic 5-stage RISC pipelines (IF, ID, EX, MEM, WB), pipeline bubble stalls, data forwarding bypasses, dynamic 2-bit branch predictors, and speculative execution.',
 'ADVANCED', 'ARCHITECTURE', 90, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000049', 'a0000001-0000-0000-0000-000000000009',
 'Cache Memory and Memory Hierarchy', 'cache-memory-hierarchy',
 'Temporal and spatial locality, L1/L2/L3 cache architectures, direct-mapped vs set-associative caching, and cache replacement policies.',
 'Examines cache block structures (tag, index, offset bits), cold/capacity/conflict misses (3Cs), write-through vs write-back with dirty bits, and MESI cache coherency protocols.',
 'INTERMEDIATE', 'ARCHITECTURE', 75, 'PUBLISHED', 0.95),

-- Compiler Design (50-53)
('b0000001-0000-0000-0000-000000000050', 'a0000001-0000-0000-0000-000000000010',
 'Lexical Analysis and Tokenization', 'lexical-analysis-tokenization',
 'Scanning source streams into token streams using regular expressions, transition diagrams, and deterministic finite automata.',
 'Formal language token definition, lexer generator mechanics (Lex/Flex), longest match heuristics, lookahead buffers, and identifier symbol table registration.',
 'BEGINNER', 'ALGORITHM', 60, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000051', 'a0000001-0000-0000-0000-000000000010',
 'Syntax Analysis and Parsing Algorithms', 'syntax-analysis-parsing',
 'Context-free grammars, ambiguity resolution, LL(1) recursive descent predictive parsing, and LR/LALR shift-reduce bottom-up parsing.',
 'Covers FIRST and FOLLOW set computations, parsing tables, shift-reduce conflict resolution, syntax error recovery, and parser generator tools (Yacc/Bison).',
 'INTERMEDIATE', 'ALGORITHM', 90, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000052', 'a0000001-0000-0000-0000-000000000010',
 'Intermediate Representation and AST', 'intermediate-representation-ast',
 'Syntax-directed translation, abstract syntax tree (AST) construction, symbol table management, and Three-Address Code (TAC).',
 'Focuses on type checking, semantic analysis, linearization into quadruples/triples, Static Single Assignment (SSA) form, and scope resolution.',
 'INTERMEDIATE', 'DATA_STRUCTURE', 75, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000053', 'a0000001-0000-0000-0000-000000000010',
 'Compiler Optimizations and Code Generation', 'compiler-optimizations-codegen',
 'Control flow graphs (CFG), basic blocks, dead code elimination, constant folding, register allocation via graph coloring, and target assembly emission.',
 'Dataflow equations (reaching definitions, available expressions, liveness analysis), loop-invariant code motion, peephole optimizations, and instruction selection.',
 'ADVANCED', 'ALGORITHM', 90, 'PUBLISHED', 0.90),

-- Theory of Computation (54-57)
('b0000001-0000-0000-0000-000000000054', 'a0000001-0000-0000-0000-000000000011',
 'Regular Languages and Finite Automata', 'regular-languages-finite-automata',
 'Deterministic Finite Automata (DFA), Non-Deterministic Finite Automata (NFA), epsilon transitions, regular expressions, and closure properties.',
 'Subset construction algorithm (NFA to DFA conversion), DFA state minimization via partition refinement, and the Pumping Lemma for regular languages.',
 'BEGINNER', 'CONCEPT', 60, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000055', 'a0000001-0000-0000-0000-000000000011',
 'Context-Free Grammars and Pushdown Automata', 'context-free-languages-pda',
 'Context-Free Grammars (CFG), parse trees, pushdown automata with auxiliary stack storage, Chomsky normal form, and the Pumping Lemma.',
 'Equivalence between CFGs and Pushdown Automata (PDA), deterministic vs non-deterministic PDAs, inherently ambiguous languages, and non-context-free proofs.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000056', 'a0000001-0000-0000-0000-000000000011',
 'Turing Machines and Decidability', 'turing-machines-decidability',
 'Turing machine formal model, infinite tape, Church-Turing thesis, decidable vs undecidable languages, and the Halting Problem proof via diagonalization.',
 'Universal Turing machines, recognizable (recursively enumerable) vs decidable languages, mapping reductions (A <=m B), Rice theorem, and Post Correspondence Problem.',
 'INTERMEDIATE', 'CONCEPT', 90, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000057', 'a0000001-0000-0000-0000-000000000011',
 'Computational Complexity and NP-Completeness', 'computational-complexity-np-completeness',
 'Time and space complexity classes, P, NP, NP-Hard, polynomial-time reductions, Cook-Levin theorem, and canonical NP-Complete problems.',
 'Verification vs search, non-deterministic polynomial time, polynomial reductions (SAT to 3SAT, 3SAT to Clique/Vertex Cover), and the P vs NP millennium prize problem.',
 'ADVANCED', 'CONCEPT', 90, 'PUBLISHED', 1.00),

-- Cybersecurity & Cryptography (58-61)
('b0000001-0000-0000-0000-000000000058', 'a0000001-0000-0000-0000-000000000012',
 'Cryptographic Primitives and PKI', 'cryptographic-primitives-pki',
 'Symmetric encryption (AES), asymmetric key exchange (RSA, ECC), cryptographic hash functions (SHA-256), digital signatures, and X.509 PKI.',
 'Block cipher modes (GCM, CBC), Diffie-Hellman key exchange, elliptic curve cryptography, certificate authorities (CAs), certificate pinning, and replay defenses.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 1.00),

('b0000001-0000-0000-0000-000000000059', 'a0000001-0000-0000-0000-000000000012',
 'Web Application Security and OWASP', 'web-app-security-owasp',
 'OWASP Top 10 vulnerabilities (SQL Injection, XSS, CSRF, SSRF, Broken Object Level Authorization), input validation, secure cookie attributes, and CSP.',
 'Mechanisms of reflected/stored/DOM XSS, parameterized queries to prevent SQLi, SameSite cookie policies, anti-CSRF token verification, and Content Security Policy headers.',
 'INTERMEDIATE', 'CONCEPT', 75, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000060', 'a0000001-0000-0000-0000-000000000012',
 'Authentication and Authorization Protocols', 'auth-protocols-oauth-jwt',
 'Cryptographic password hashing (Argon2, bcrypt), OAuth 2.0 authorization framework, OpenID Connect identity layer, JWT structure, and RBAC.',
 'Authorization code flow with PKCE, access vs refresh token lifecycles, cryptographic token signatures, claims validation, and Role-Based vs Attribute-Based Access Control.',
 'INTERMEDIATE', 'CONCEPT', 60, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000061', 'a0000001-0000-0000-0000-000000000012',
 'Network Security and Firewalls', 'network-security-firewalls',
 'Stateful inspection firewalls, packet filters, TLS handshake mechanics, VPN tunneling (IPsec, WireGuard), and intrusion detection systems.',
 'TCP syn-flood defenses, deep packet inspection, TLS 1.3 key derivation, zero-trust network architectures, mutual TLS (mTLS), and perimeter defense policies.',
 'ADVANCED', 'ARCHITECTURE', 75, 'PUBLISHED', 0.90),

-- Web Development & Systems (62-65)
('b0000001-0000-0000-0000-000000000062', 'a0000001-0000-0000-0000-000000000013',
 'HTTP Protocols and RESTful Architecture', 'http-protocols-rest',
 'HTTP/1.1 pipelining, HTTP/2 multiplexing, HTTP/3 QUIC, request/response headers, status codes, REST architectural constraints, and idempotency.',
 'Client-server separation, stateless interactions, caching directives (Cache-Control, ETag), RESTful resource URIs, content negotiation, and connection keep-alive.',
 'BEGINNER', 'CONCEPT', 60, 'PUBLISHED', 0.95),

('b0000001-0000-0000-0000-000000000063', 'a0000001-0000-0000-0000-000000000013',
 'Browser Rendering Engine and DOM', 'browser-rendering-dom',
 'Critical rendering path (HTML/CSS parsing, DOM/CSSOM tree, Layout, Paint, Composite), reflow/repaint optimizations, and the JavaScript event loop.',
 'Microtask vs macrotask execution order, requestAnimationFrame, compositor layers, memory leak detection in DOM references, and browser execution lifecycle.',
 'INTERMEDIATE', 'ARCHITECTURE', 75, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000064', 'a0000001-0000-0000-0000-000000000013',
 'Reactive UI Models and State Management', 'reactive-ui-state-management',
 'Declarative component lifecycles, Virtual DOM diffing, unidirectional data flow, immutable state trees, and reactive event bindings.',
 'Reconciliation heuristics (key attributes, tree level diffing), state lifting, global state stores, hooks abstraction, and rendering optimization techniques.',
 'INTERMEDIATE', 'PARADIGM', 75, 'PUBLISHED', 0.90),

('b0000001-0000-0000-0000-000000000065', 'a0000001-0000-0000-0000-000000000013',
 'Server-Side Rendering and Web Performance', 'ssr-web-performance',
 'Client-side rendering (CSR) vs Server-Side Rendering (SSR), hydration, static site generation (SSG), Core Web Vitals (LCP, FID, CLS), and CDN edge caching.',
 'Incremental static regeneration, streaming SSR, edge functions, code splitting, bundle chunk analysis, and critical resource preloading.',
 'ADVANCED', 'ARCHITECTURE', 75, 'PUBLISHED', 0.85)
ON CONFLICT (slug) DO NOTHING;

-- 4. Learning Objectives for New Topics
INSERT INTO knowledge_topic_learning_objectives (topic_id, objective, display_order)
VALUES
('b0000001-0000-0000-0000-000000000046', 'Construct truth tables and simplify Boolean expressions using algebraic theorems.', 1),
('b0000001-0000-0000-0000-000000000046', 'Design combinational logic circuits using basic gates, multiplexers, and decoders.', 2),
('b0000001-0000-0000-0000-000000000047', 'Trace instruction execution through the CPU fetch-decode-execute cycle.', 1),
('b0000001-0000-0000-0000-000000000047', 'Compare RISC and CISC architectures in register utilization and instruction complexity.', 2),
('b0000001-0000-0000-0000-000000000048', 'Analyze instruction pipelining and compute theoretical CPI speedup.', 1),
('b0000001-0000-0000-0000-000000000048', 'Identify structural, data, and control hazards and apply forwarding or branch prediction.', 2),
('b0000001-0000-0000-0000-000000000049', 'Explain temporal and spatial locality and its role in cache hierarchy design.', 1),
('b0000001-0000-0000-0000-000000000049', 'Calculate cache hit/miss ratios for direct-mapped and set-associative caches.', 2),
('b0000001-0000-0000-0000-000000000050', 'Construct regular expressions and finite automata for lexical token specifications.', 1),
('b0000001-0000-0000-0000-000000000050', 'Implement a lexical scanner handling identifiers, keywords, literals, and operators.', 2),
('b0000001-0000-0000-0000-000000000051', 'Compute FIRST and FOLLOW sets for context-free grammars.', 1),
('b0000001-0000-0000-0000-000000000051', 'Construct predictive LL(1) parsing tables and resolve shift-reduce parser conflicts.', 2),
('b0000001-0000-0000-0000-000000000052', 'Generate Abstract Syntax Trees and perform semantic type-checking passes.', 1),
('b0000001-0000-0000-0000-000000000052', 'Translate high-level expressions into linearized Three-Address Code and SSA representations.', 2),
('b0000001-0000-0000-0000-000000000053', 'Construct control flow graphs and identify loop invariants and dead code.', 1),
('b0000001-0000-0000-0000-000000000053', 'Perform register allocation using interference graph coloring.', 2),
('b0000001-0000-0000-0000-000000000054', 'Design DFAs and NFAs accepting formal languages and convert NFA to DFA via subset construction.', 1),
('b0000001-0000-0000-0000-000000000054', 'Apply the Pumping Lemma to prove non-regularity of candidate languages.', 2),
('b0000001-0000-0000-0000-000000000055', 'Formulate Context-Free Grammars and convert to Chomsky Normal Form.', 1),
('b0000001-0000-0000-0000-000000000055', 'Design pushdown automata recognizing context-free languages.', 2),
('b0000001-0000-0000-0000-000000000056', 'Define formal Turing machines and simulate computation on input tapes.', 1),
('b0000001-0000-0000-0000-000000000056', 'Prove the undecidability of the Halting Problem using diagonalization arguments.', 2),
('b0000001-0000-0000-0000-000000000057', 'Differentiate complexity classes P, NP, NP-Complete, and NP-Hard.', 1),
('b0000001-0000-0000-0000-000000000057', 'Construct polynomial-time reductions between canonical NP-Complete problems.', 2),
('b0000001-0000-0000-0000-000000000058', 'Explain symmetric and asymmetric encryption principles and their cryptographic guarantees.', 1),
('b0000001-0000-0000-0000-000000000058', 'Trace the PKI certificate verification chain and digital signature validation.', 2),
('b0000001-0000-0000-0000-000000000059', 'Audit web applications for OWASP Top 10 vulnerabilities including XSS and SQLi.', 1),
('b0000001-0000-0000-0000-000000000059', 'Configure security headers (CSP, HSTS, SameSite cookies) to mitigate client-side attacks.', 2),
('b0000001-0000-0000-0000-000000000060', 'Implement secure password storage using modern adaptive hashing algorithms (Argon2, bcrypt).', 1),
('b0000001-0000-0000-0000-000000000060', 'Trace the OAuth 2.0 Authorization Code flow with PKCE and validate signed JWT tokens.', 2),
('b0000001-0000-0000-0000-000000000061', 'Configure stateful firewall filtering rules and interpret packet headers.', 1),
('b0000001-0000-0000-0000-000000000061', 'Describe the TLS handshake cryptographic key exchange and verify cipher suites.', 2),
('b0000001-0000-0000-0000-000000000062', 'Analyze HTTP request/response methods, headers, and status code semantics.', 1),
('b0000001-0000-0000-0000-000000000062', 'Design idempotent, stateless RESTful APIs adhering to Richardson Maturity Model.', 2),
('b0000001-0000-0000-0000-000000000063', 'Trace the browser critical rendering path from HTML to screen pixels.', 1),
('b0000001-0000-0000-0000-000000000063', 'Optimize JavaScript event execution using event delegation and microtask scheduling.', 2),
('b0000001-0000-0000-0000-000000000064', 'Explain declarative rendering and Virtual DOM tree diffing algorithms.', 1),
('b0000001-0000-0000-0000-000000000064', 'Manage application state immutably across component hierarchies.', 2),
('b0000001-0000-0000-0000-000000000065', 'Compare CSR, SSR, and SSG performance trade-offs for web applications.', 1),
('b0000001-0000-0000-0000-000000000065', 'Measure and optimize Core Web Vitals using code-splitting and asset preloading.', 2)
ON CONFLICT DO NOTHING;

-- 5. Tags for New Topics
INSERT INTO knowledge_topic_tags (topic_id, tag)
VALUES
('b0000001-0000-0000-0000-000000000046', 'logic-gates'), ('b0000001-0000-0000-0000-000000000046', 'boolean-algebra'), ('b0000001-0000-0000-0000-000000000046', 'circuits'),
('b0000001-0000-0000-0000-000000000047', 'cpu-datapath'), ('b0000001-0000-0000-0000-000000000047', 'instruction-set'), ('b0000001-0000-0000-0000-000000000047', 'risc-cisc'),
('b0000001-0000-0000-0000-000000000048', 'pipelining'), ('b0000001-0000-0000-0000-000000000048', 'hazards'), ('b0000001-0000-0000-0000-000000000048', 'branch-prediction'),
('b0000001-0000-0000-0000-000000000049', 'cache'), ('b0000001-0000-0000-0000-000000000049', 'locality'), ('b0000001-0000-0000-0000-000000000049', 'memory-hierarchy'),
('b0000001-0000-0000-0000-000000000050', 'lexer'), ('b0000001-0000-0000-0000-000000000050', 'tokenization'), ('b0000001-0000-0000-0000-000000000050', 'regex'),
('b0000001-0000-0000-0000-000000000051', 'parser'), ('b0000001-0000-0000-0000-000000000051', 'cfg'), ('b0000001-0000-0000-0000-000000000051', 'll-parsing'), ('b0000001-0000-0000-0000-000000000051', 'lr-parsing'),
('b0000001-0000-0000-0000-000000000052', 'ast'), ('b0000001-0000-0000-0000-000000000052', 'ir'), ('b0000001-0000-0000-0000-000000000052', 'three-address-code'),
('b0000001-0000-0000-0000-000000000053', 'optimization'), ('b0000001-0000-0000-0000-000000000053', 'codegen'), ('b0000001-0000-0000-0000-000000000053', 'cfg-analysis'),
('b0000001-0000-0000-0000-000000000054', 'automata'), ('b0000001-0000-0000-0000-000000000054', 'dfa'), ('b0000001-0000-0000-0000-000000000054', 'nfa'),
('b0000001-0000-0000-0000-000000000055', 'pushdown-automata'), ('b0000001-0000-0000-0000-000000000055', 'pda'), ('b0000001-0000-0000-0000-000000000055', 'context-free'),
('b0000001-0000-0000-0000-000000000056', 'turing-machine'), ('b0000001-0000-0000-0000-000000000056', 'decidability'), ('b0000001-0000-0000-0000-000000000056', 'halting-problem'),
('b0000001-0000-0000-0000-000000000057', 'np-completeness'), ('b0000001-0000-0000-0000-000000000057', 'p-vs-np'), ('b0000001-0000-0000-0000-000000000057', 'reductions'),
('b0000001-0000-0000-0000-000000000058', 'cryptography'), ('b0000001-0000-0000-0000-000000000058', 'rsa'), ('b0000001-0000-0000-0000-000000000058', 'pki'), ('b0000001-0000-0000-0000-000000000058', 'aes'),
('b0000001-0000-0000-0000-000000000059', 'web-security'), ('b0000001-0000-0000-0000-000000000059', 'owasp'), ('b0000001-0000-0000-0000-000000000059', 'xss'), ('b0000001-0000-0000-0000-000000000059', 'sqli'),
('b0000001-0000-0000-0000-000000000060', 'oauth2'), ('b0000001-0000-0000-0000-000000000060', 'jwt'), ('b0000001-0000-0000-0000-000000000060', 'authentication'),
('b0000001-0000-0000-0000-000000000061', 'firewalls'), ('b0000001-0000-0000-0000-000000000061', 'tls'), ('b0000001-0000-0000-0000-000000000061', 'vpn'),
('b0000001-0000-0000-0000-000000000062', 'http'), ('b0000001-0000-0000-0000-000000000062', 'rest-api'), ('b0000001-0000-0000-0000-000000000062', 'http2'),
('b0000001-0000-0000-0000-000000000063', 'dom'), ('b0000001-0000-0000-0000-000000000063', 'rendering-engine'), ('b0000001-0000-0000-0000-000000000063', 'event-loop'),
('b0000001-0000-0000-0000-000000000064', 'reactivity'), ('b0000001-0000-0000-0000-000000000064', 'virtual-dom'), ('b0000001-0000-0000-0000-000000000064', 'state-management'),
('b0000001-0000-0000-0000-000000000065', 'ssr'), ('b0000001-0000-0000-0000-000000000065', 'ssg'), ('b0000001-0000-0000-0000-000000000065', 'web-vitals')
ON CONFLICT DO NOTHING;

-- 6. Insert Prerequisite and Related Relationships (Validated by trg_prevent_prerequisite_cycle)
INSERT INTO knowledge_topic_relationships (source_topic_id, target_topic_id, relationship_type, description, weight)
VALUES
-- Architecture Track: Digital Logic -> CPU Datapath -> Pipelining & Cache
('b0000001-0000-0000-0000-000000000046', 'b0000001-0000-0000-0000-000000000047', 'PREREQUISITE_OF', 'Logic gates and circuits are the physical building blocks of the CPU datapath.', 1.0),
('b0000001-0000-0000-0000-000000000047', 'b0000001-0000-0000-0000-000000000048', 'PREREQUISITE_OF', 'Pipelining overlaps stages of the single-cycle instruction execution datapath.', 1.0),
('b0000001-0000-0000-0000-000000000047', 'b0000001-0000-0000-0000-000000000049', 'PREREQUISITE_OF', 'CPU load-store instruction timing directly drives cache hierarchy design.', 1.0),

-- Theory of Computation Track: Programming Basics -> Regular Languages -> CFL/PDA -> Turing Machines -> Complexity
('b0000001-0000-0000-0000-000000000001', 'b0000001-0000-0000-0000-000000000054', 'PREREQUISITE_OF', 'Basic computational models motivate formal regular language specifications.', 0.9),
('b0000001-0000-0000-0000-000000000054', 'b0000001-0000-0000-0000-000000000055', 'PREREQUISITE_OF', 'Context-free languages strictly extend regular language expressive capacity.', 1.0),
('b0000001-0000-0000-0000-000000000055', 'b0000001-0000-0000-0000-000000000056', 'PREREQUISITE_OF', 'Pushdown automata with stacks generalize into infinite-tape Turing machines.', 1.0),
('b0000001-0000-0000-0000-000000000056', 'b0000001-0000-0000-0000-000000000057', 'PREREQUISITE_OF', 'Decidable languages are classified by polynomial runtime bounds in complexity theory.', 1.0),
('b0000001-0000-0000-0000-000000000008', 'b0000001-0000-0000-0000-000000000057', 'PREREQUISITE_OF', 'Big-O asymptotic bounds provide the mathematical foundation for P and NP.', 1.0),

-- Compiler Track: Regular Languages -> Lexical Analysis -> Syntax Analysis -> IR/AST -> Code Generation
('b0000001-0000-0000-0000-000000000054', 'b0000001-0000-0000-0000-000000000050', 'PREREQUISITE_OF', 'Finite automata and regexes directly power lexical scanner implementation.', 1.0),
('b0000001-0000-0000-0000-000000000050', 'b0000001-0000-0000-0000-000000000051', 'PREREQUISITE_OF', 'Lexical token streams are consumed as input by the syntax parser.', 1.0),
('b0000001-0000-0000-0000-000000000055', 'b0000001-0000-0000-0000-000000000051', 'PREREQUISITE_OF', 'Context-free grammars formally define programming language syntax rules.', 1.0),
('b0000001-0000-0000-0000-000000000051', 'b0000001-0000-0000-0000-000000000052', 'PREREQUISITE_OF', 'Parsers build Abstract Syntax Trees via syntax-directed translation.', 1.0),
('b0000001-0000-0000-0000-000000000013', 'b0000001-0000-0000-0000-000000000052', 'PREREQUISITE_OF', 'Tree data structures are the concrete representation of Abstract Syntax Trees.', 1.0),
('b0000001-0000-0000-0000-000000000052', 'b0000001-0000-0000-0000-000000000053', 'PREREQUISITE_OF', 'Intermediate representations are optimized and lowered into machine code.', 1.0),
('b0000001-0000-0000-0000-000000000047', 'b0000001-0000-0000-0000-000000000053', 'PREREQUISITE_OF', 'Target instruction sets dictate code generator assembly output.', 0.9),

-- Cybersecurity Track: Primitive Variables -> Cryptographic Primitives -> Auth Protocols -> Web & Network Security
('b0000001-0000-0000-0000-000000000002', 'b0000001-0000-0000-0000-000000000058', 'PREREQUISITE_OF', 'Bitwise operations and binary representations underpin cryptographic ciphers.', 0.8),
('b0000001-0000-0000-0000-000000000058', 'b0000001-0000-0000-0000-000000000060', 'PREREQUISITE_OF', 'Hashing and asymmetric cryptography secure authentication tokens and signatures.', 1.0),
('b0000001-0000-0000-0000-000000000060', 'b0000001-0000-0000-0000-000000000059', 'PREREQUISITE_OF', 'Authentication and authorization flaws constitute major OWASP security vulnerabilities.', 1.0),
('b0000001-0000-0000-0000-000000000058', 'b0000001-0000-0000-0000-000000000061', 'PREREQUISITE_OF', 'PKI and cipher suites enable secure TLS handshakes in network security.', 1.0),
('b0000001-0000-0000-0000-000000000034', 'b0000001-0000-0000-0000-000000000061', 'PREREQUISITE_OF', 'Transport layer socket mechanics are inspected by network packet firewalls.', 1.0),

-- Web Systems Track: Transport Protocols -> HTTP -> Browser DOM -> Reactive UI -> SSR
('b0000001-0000-0000-0000-000000000034', 'b0000001-0000-0000-0000-000000000062', 'PREREQUISITE_OF', 'HTTP/1.1 and HTTP/2 operate directly over reliable TCP transport streams.', 1.0),
('b0000001-0000-0000-0000-000000000062', 'b0000001-0000-0000-0000-000000000063', 'PREREQUISITE_OF', 'Browser rendering begins with HTTP response payload reception.', 0.9),
('b0000001-0000-0000-0000-000000000063', 'b0000001-0000-0000-0000-000000000064', 'PREREQUISITE_OF', 'Reactive UI libraries reconcile state changes against the native DOM.', 1.0),
('b0000001-0000-0000-0000-000000000064', 'b0000001-0000-0000-0000-000000000065', 'PREREQUISITE_OF', 'SSR renders reactive component trees on the server before client hydration.', 1.0),
('b0000001-0000-0000-0000-000000000062', 'b0000001-0000-0000-0000-000000000065', 'PREREQUISITE_OF', 'Streaming SSR and caching headers rely on advanced HTTP protocol directives.', 0.9),
('b0000001-0000-0000-0000-000000000062', 'b0000001-0000-0000-0000-000000000059', 'PREREQUISITE_OF', 'Understanding HTTP headers and cookies is vital for web application security.', 0.9),

-- RELATED_TO Edges
('b0000001-0000-0000-0000-000000000049', 'b0000001-0000-0000-0000-000000000030', 'RELATED_TO', 'Hardware cache memory and OS virtual memory paging both exploit memory locality.', 0.85),
('b0000001-0000-0000-0000-000000000051', 'b0000001-0000-0000-0000-000000000012', 'RELATED_TO', 'Shift-reduce parsing algorithms operate on internal stack data structures.', 0.9),
('b0000001-0000-0000-0000-000000000059', 'b0000001-0000-0000-0000-000000000019', 'RELATED_TO', 'SQL injection vulnerabilities exploit improper SQL query parameterization.', 0.95),
('b0000001-0000-0000-0000-000000000063', 'b0000001-0000-0000-0000-000000000025', 'RELATED_TO', 'Browser event loop task queues interact closely with operating system thread scheduling.', 0.8)
ON CONFLICT (source_topic_id, target_topic_id, relationship_type) DO NOTHING;
