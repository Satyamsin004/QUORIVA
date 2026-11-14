import { test, describe } from 'node:test';
import assert from 'node:assert';
import {
  KnowledgeTopicSummary,
  GraphNode,
  GraphEdge,
  LearningPathStep,
  TopicDifficulty,
} from '../src/types/knowledge';

// Helper functions mirroring frontend logic
function buildConceptualTutorPrompt(topicName: string, subjectName: string): string {
  return `Explain the foundational computer science concepts of "${topicName}" (${subjectName}). Cover the theoretical intuition, core mechanisms or algorithms, space/time complexities and engineering trade-offs, common practical use cases, and how it connects to its prerequisites.`;
}

function filterGraphBySubject(nodes: GraphNode[], edges: GraphEdge[], subjectId?: string) {
  if (!subjectId || subjectId === 'all') {
    return { nodes, edges };
  }
  const filteredNodes = nodes.filter((n) => n.subjectId === subjectId);
  const nodeIds = new Set(filteredNodes.map((n) => n.id));
  const filteredEdges = edges.filter((e) => nodeIds.has(e.source) && nodeIds.has(e.target));
  return { nodes: filteredNodes, edges: filteredEdges };
}

function calculateTotalPathMinutes(steps: LearningPathStep[]): number {
  return steps.reduce((acc, step) => acc + (step.estimatedMinutes || 0), 0);
}

function verifyTopologicalPrerequisiteOrder(steps: LearningPathStep[], prereqEdges: { from: string; to: string }[]): boolean {
  const stepIndices = new Map<string, number>();
  steps.forEach((step, idx) => stepIndices.set(step.slug, idx));

  for (const edge of prereqEdges) {
    const fromIdx = stepIndices.get(edge.from);
    const toIdx = stepIndices.get(edge.to);
    if (fromIdx !== undefined && toIdx !== undefined) {
      if (fromIdx >= toIdx) {
        return false; // Prerequisite must come before target
      }
    }
  }
  return true;
}

describe('CS Topic Discovery & Knowledge Graph Integration', () => {
  test('generates conceptual tutor prefill prompt without client-injected evidence or unauthorized passages', () => {
    const prompt = buildConceptualTutorPrompt(
      'Binary Search Trees (BST)',
      'Data Structures and Algorithms'
    );

    assert.ok(prompt.includes('Binary Search Trees (BST)'));
    assert.ok(prompt.includes('Data Structures and Algorithms'));
    assert.ok(prompt.includes('theoretical intuition'));
    assert.ok(prompt.includes('space/time complexities'));
    // Ensures no client-injected passage text
    assert.strictEqual(prompt.includes('passageContent'), false);
    assert.strictEqual(prompt.includes('unauthorized'), false);
  });

  test('validates learning path total study minutes aggregation', () => {
    const mockSteps: LearningPathStep[] = [
      {
        stepNumber: 1,
        topicId: 't1',
        name: 'Variables & Types',
        slug: 'variables-and-data-types',
        subjectName: 'Programming Fundamentals',
        difficultyLevel: 'BEGINNER',
        estimatedMinutes: 45,
        summary: 'Variables',
        directPrerequisitesCount: 0,
      },
      {
        stepNumber: 2,
        topicId: 't2',
        name: 'Arrays & Strings',
        slug: 'arrays-and-strings',
        subjectName: 'Data Structures and Algorithms',
        difficultyLevel: 'BEGINNER',
        estimatedMinutes: 60,
        summary: 'Arrays',
        directPrerequisitesCount: 1,
      },
      {
        stepNumber: 3,
        topicId: 't3',
        name: 'Binary Search',
        slug: 'binary-search',
        subjectName: 'Data Structures and Algorithms',
        difficultyLevel: 'INTERMEDIATE',
        estimatedMinutes: 60,
        summary: 'Search',
        isTargetTopic: true,
        directPrerequisitesCount: 1,
      },
    ];

    const totalMinutes = calculateTotalPathMinutes(mockSteps);
    assert.strictEqual(totalMinutes, 165);
    assert.strictEqual(mockSteps[mockSteps.length - 1].isTargetTopic, true);
  });

  test('enforces strict topological order where prerequisites always precede dependent topics', () => {
    const steps: LearningPathStep[] = [
      {
        stepNumber: 1,
        topicId: '1',
        name: 'Variables and Data Types',
        slug: 'variables-and-data-types',
        subjectName: 'Programming Fundamentals',
        difficultyLevel: 'BEGINNER',
        estimatedMinutes: 45,
        summary: 'Basics',
        directPrerequisitesCount: 0,
      },
      {
        stepNumber: 2,
        topicId: '2',
        name: 'Recursion Fundamentals',
        slug: 'recursion-fundamentals',
        subjectName: 'Programming Fundamentals',
        difficultyLevel: 'INTERMEDIATE',
        estimatedMinutes: 60,
        summary: 'Recursive calls',
        directPrerequisitesCount: 1,
      },
      {
        stepNumber: 3,
        topicId: '3',
        name: 'Trees and Binary Trees',
        slug: 'trees',
        subjectName: 'Data Structures and Algorithms',
        difficultyLevel: 'INTERMEDIATE',
        estimatedMinutes: 90,
        summary: 'Hierarchical structures',
        directPrerequisitesCount: 1,
      },
      {
        stepNumber: 4,
        topicId: '4',
        name: 'Binary Search Trees (BST)',
        slug: 'binary-search-trees',
        subjectName: 'Data Structures and Algorithms',
        difficultyLevel: 'INTERMEDIATE',
        estimatedMinutes: 75,
        summary: 'BST ordering',
        isTargetTopic: true,
        directPrerequisitesCount: 1,
      },
    ];

    const prereqEdges = [
      { from: 'variables-and-data-types', to: 'recursion-fundamentals' },
      { from: 'recursion-fundamentals', to: 'trees' },
      { from: 'trees', to: 'binary-search-trees' },
    ];

    const isValidOrder = verifyTopologicalPrerequisiteOrder(steps, prereqEdges);
    assert.strictEqual(isValidOrder, true);
  });

  test('filters graph nodes and edges safely when restricted by subject', () => {
    const mockNodes: GraphNode[] = [
      {
        id: 'n1',
        name: 'Arrays',
        slug: 'arrays',
        subjectId: 'sub-dsa',
        subjectName: 'DSA',
        topicType: 'DATA_STRUCTURE',
        difficultyLevel: 'BEGINNER',
        importanceScore: 0.9,
        prerequisiteCount: 0,
        unlockedCount: 2,
      },
      {
        id: 'n2',
        name: 'Processes',
        slug: 'processes',
        subjectId: 'sub-os',
        subjectName: 'OS',
        topicType: 'CORE_CONCEPT',
        difficultyLevel: 'INTERMEDIATE',
        importanceScore: 0.85,
        prerequisiteCount: 0,
        unlockedCount: 1,
      },
    ];

    const mockEdges: GraphEdge[] = [
      {
        id: 'e1',
        source: 'n1',
        target: 'n2',
        relationshipType: 'RELATED_TO',
        description: 'Related concepts',
      },
    ];

    // Filter to sub-dsa
    const filtered = filterGraphBySubject(mockNodes, mockEdges, 'sub-dsa');
    assert.strictEqual(filtered.nodes.length, 1);
    assert.strictEqual(filtered.nodes[0].slug, 'arrays');
    // The edge between n1 and n2 should be removed because n2 is excluded
    assert.strictEqual(filtered.edges.length, 0);

    // Filter with 'all'
    const all = filterGraphBySubject(mockNodes, mockEdges, 'all');
    assert.strictEqual(all.nodes.length, 2);
    assert.strictEqual(all.edges.length, 1);
  });

  test('validates all 13 academic disciplines and their canonical codes', () => {
    const expectedSubjectCodes = [
      'CS-FND',
      'CS-DSA',
      'CS-OS',
      'CS-NET',
      'CS-DB',
      'CS-SE',
      'CS-AI',
      'CS-SYS',
      'CS-ARCH',
      'CS-COMP',
      'CS-TOC',
      'CS-SEC',
      'CS-WEB',
    ];

    const mockSubjects = [
      { id: '1', code: 'CS-FND', name: 'Programming Fundamentals', slug: 'programming-fundamentals' },
      { id: '2', code: 'CS-DSA', name: 'Data Structures and Algorithms', slug: 'data-structures-and-algorithms' },
      { id: '3', code: 'CS-OS', name: 'Operating Systems', slug: 'operating-systems' },
      { id: '4', code: 'CS-NET', name: 'Computer Networks', slug: 'computer-networks' },
      { id: '5', code: 'CS-DB', name: 'Database Systems', slug: 'database-systems' },
      { id: '6', code: 'CS-SE', name: 'Software Engineering', slug: 'software-engineering' },
      { id: '7', code: 'CS-AI', name: 'Artificial Intelligence & Machine Learning', slug: 'artificial-intelligence' },
      { id: '8', code: 'CS-SYS', name: 'Distributed Systems & Cloud Computing', slug: 'distributed-systems' },
      { id: '9', code: 'CS-ARCH', name: 'Computer Organization & Architecture', slug: 'computer-organization-architecture' },
      { id: '10', code: 'CS-COMP', name: 'Compiler Design', slug: 'compiler-design' },
      { id: '11', code: 'CS-TOC', name: 'Theory of Computation', slug: 'theory-of-computation' },
      { id: '12', code: 'CS-SEC', name: 'Cybersecurity & Information Assurance', slug: 'cybersecurity-information-assurance' },
      { id: '13', code: 'CS-WEB', name: 'Web Development & Distributed Applications', slug: 'web-development-distributed-applications' },
    ];

    assert.strictEqual(mockSubjects.length, 13);
    const codes = mockSubjects.map((s) => s.code);
    expectedSubjectCodes.forEach((expectedCode) => {
      assert.ok(codes.includes(expectedCode), `Expected subject code ${expectedCode} to be present`);
    });
  });

  test('enforces strict topological sequence on newly added disciplines (Architecture, Compilers, Theory, Security, Web)', () => {
    // Pipeline sequence across CS-ARCH, CS-COMP, CS-TOC, CS-SEC, CS-WEB
    const archSteps: LearningPathStep[] = [
      {
        stepNumber: 1,
        topicId: 'arch-1',
        name: 'Digital Logic and Boolean Algebra',
        slug: 'digital-logic-boolean-algebra',
        subjectName: 'Computer Organization & Architecture',
        difficultyLevel: 'BEGINNER',
        estimatedMinutes: 60,
        summary: 'Gates and circuits',
        directPrerequisitesCount: 0,
      },
      {
        stepNumber: 2,
        topicId: 'arch-2',
        name: 'Computer Organization & Register Transfer',
        slug: 'computer-organization-register-transfer',
        subjectName: 'Computer Organization & Architecture',
        difficultyLevel: 'INTERMEDIATE',
        estimatedMinutes: 75,
        summary: 'ALU and register transfers',
        directPrerequisitesCount: 1,
      },
      {
        stepNumber: 3,
        topicId: 'arch-3',
        name: 'CPU Microarchitecture & Datapaths',
        slug: 'cpu-microarchitecture-datapaths',
        subjectName: 'Computer Organization & Architecture',
        difficultyLevel: 'INTERMEDIATE',
        estimatedMinutes: 90,
        summary: 'Control unit and datapaths',
        directPrerequisitesCount: 1,
      },
      {
        stepNumber: 4,
        topicId: 'arch-4',
        name: 'Instruction Pipelining & Hazards',
        slug: 'instruction-pipelining-hazards',
        subjectName: 'Computer Organization & Architecture',
        difficultyLevel: 'ADVANCED',
        estimatedMinutes: 105,
        summary: 'Hazards and forwarding',
        isTargetTopic: true,
        directPrerequisitesCount: 1,
      },
    ];

    const archEdges = [
      { from: 'digital-logic-boolean-algebra', to: 'computer-organization-register-transfer' },
      { from: 'computer-organization-register-transfer', to: 'cpu-microarchitecture-datapaths' },
      { from: 'cpu-microarchitecture-datapaths', to: 'instruction-pipelining-hazards' },
    ];

    assert.strictEqual(verifyTopologicalPrerequisiteOrder(archSteps, archEdges), true);
    assert.strictEqual(archSteps[archSteps.length - 1].isTargetTopic, true);

    // Theory of Computation sequence
    const tocSteps: LearningPathStep[] = [
      {
        stepNumber: 1,
        topicId: 'toc-1',
        name: 'Deterministic & Non-Deterministic Finite Automata',
        slug: 'finite-automata-regular-languages',
        subjectName: 'Theory of Computation',
        difficultyLevel: 'BEGINNER',
        estimatedMinutes: 60,
        summary: 'DFA and NFA',
        directPrerequisitesCount: 0,
      },
      {
        stepNumber: 2,
        topicId: 'toc-2',
        name: 'Context-Free Languages & Pushdown Automata',
        slug: 'context-free-languages-pushdown-automata',
        subjectName: 'Theory of Computation',
        difficultyLevel: 'INTERMEDIATE',
        estimatedMinutes: 75,
        summary: 'CFG and PDA',
        directPrerequisitesCount: 1,
      },
      {
        stepNumber: 3,
        topicId: 'toc-3',
        name: 'Turing Machines & Decidability',
        slug: 'turing-machines-decidability',
        subjectName: 'Theory of Computation',
        difficultyLevel: 'ADVANCED',
        estimatedMinutes: 90,
        summary: 'Halting problem',
        directPrerequisitesCount: 1,
      },
      {
        stepNumber: 4,
        topicId: 'toc-4',
        name: 'Computational Complexity & NP-Completeness',
        slug: 'computational-complexity-np-completeness',
        subjectName: 'Theory of Computation',
        difficultyLevel: 'ADVANCED',
        estimatedMinutes: 100,
        summary: 'P vs NP and reductions',
        isTargetTopic: true,
        directPrerequisitesCount: 1,
      },
    ];

    const tocEdges = [
      { from: 'finite-automata-regular-languages', to: 'context-free-languages-pushdown-automata' },
      { from: 'context-free-languages-pushdown-automata', to: 'turing-machines-decidability' },
      { from: 'turing-machines-decidability', to: 'computational-complexity-np-completeness' },
    ];

    assert.strictEqual(verifyTopologicalPrerequisiteOrder(tocSteps, tocEdges), true);
    assert.strictEqual(tocSteps[tocSteps.length - 1].isTargetTopic, true);
  });

  test('handles prerequisite cycle error response gracefully without unhandled exception', () => {
    // Simulating Spring Boot 400 Bad Request response from KnowledgeGraphIntegrityException
    const mockCycleErrorResponse = {
      status: 400,
      data: {
        timestamp: '2026-11-12T14:00:00Z',
        status: 400,
        error: 'Bad Request',
        message: 'Graph integrity violation: Prerequisite cycle detected: Adding prerequisite edge from Topic A to Topic B would introduce a circular dependency.',
        path: '/api/v1/knowledge/topics/test/path',
      },
    };

    function extractErrorMessage(err: any): string {
      return err.response?.data?.message || err.message || 'Failed to process knowledge graph request.';
    }

    const axiosError = { response: mockCycleErrorResponse };
    const userMessage = extractErrorMessage(axiosError);

    assert.ok(userMessage.includes('Graph integrity violation'));
    assert.ok(userMessage.includes('Prerequisite cycle detected'));
  });

  test('verifies resource mapping filter restricts display to PUBLISHED resources only', () => {
    interface MockRawResourceLink {
      resourceId: string;
      title: string;
      category: string;
      resourceType: string;
      publicationStatus: 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'SUSPENDED';
    }

    const candidateResources: MockRawResourceLink[] = [
      {
        resourceId: 'res-pub-1',
        title: 'Modern Compiler Implementation in ML',
        category: 'SYSTEMS',
        resourceType: 'BOOK',
        publicationStatus: 'PUBLISHED',
      },
      {
        resourceId: 'res-pend-2',
        title: 'Draft Notes on Pipelining',
        category: 'SYSTEMS',
        resourceType: 'NOTES',
        publicationStatus: 'PENDING_REVIEW',
      },
      {
        resourceId: 'res-rej-3',
        title: 'Unverified Cheat Sheet',
        category: 'FOUNDATIONS',
        resourceType: 'NOTES',
        publicationStatus: 'REJECTED',
      },
      {
        resourceId: 'res-pub-4',
        title: 'Introduction to the Theory of Computation',
        category: 'THEORY',
        resourceType: 'BOOK',
        publicationStatus: 'PUBLISHED',
      },
    ];

    // Filter replicating server-side / client-side check
    const displayedResources = candidateResources.filter((r) => r.publicationStatus === 'PUBLISHED');

    assert.strictEqual(displayedResources.length, 2);
    assert.strictEqual(displayedResources[0].resourceId, 'res-pub-1');
    assert.strictEqual(displayedResources[1].resourceId, 'res-pub-4');
    assert.ok(displayedResources.every((r) => r.publicationStatus === 'PUBLISHED'));
  });
});

