export type TopicDifficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export type TopicType =
  | 'CORE_CONCEPT'
  | 'DATA_STRUCTURE'
  | 'ALGORITHM'
  | 'SYSTEM_DESIGN'
  | 'PARADIGM'
  | 'PROTOCOL'
  | 'THEORY'
  | 'TOOL_PRACTICE';

export type RelationshipType =
  | 'PREREQUISITE_OF'
  | 'RELATED_TO'
  | 'PART_OF'
  | 'EXTENDS'
  | 'USES'
  | 'IMPLEMENTED_BY'
  | 'ALTERNATIVE_TO';

export interface AcademicSubject {
  id: string;
  name: string;
  slug: string;
  code: string;
  description: string;
  category: string;
  displayOrder: number;
  topicCount: number;
}

export interface KnowledgeTopicSummary {
  id: string;
  subjectId: string;
  subjectName: string;
  subjectCode: string;
  name: string;
  slug: string;
  topicType: TopicType;
  difficultyLevel: TopicDifficulty;
  estimatedMinutes: number;
  summary: string;
  importanceScore: number;
  prerequisiteCount: number;
  subtopicCount: number;
}

export interface KnowledgeRelationship {
  relationshipId: string;
  topicId: string;
  name: string;
  slug: string;
  subjectName: string;
  difficultyLevel: TopicDifficulty;
  relationshipType: RelationshipType;
  weight: number;
  description: string;
}

export interface TopicResourceLink {
  resourceId: string;
  title: string;
  category: string;
  resourceType: string;
  relevanceScore: number;
}

export interface KnowledgeTopicDetail {
  id: string;
  subjectId: string;
  subjectName: string;
  subjectCode: string;
  name: string;
  slug: string;
  topicType: TopicType;
  difficultyLevel: TopicDifficulty;
  estimatedMinutes: number;
  summary: string;
  detailedDescription: string;
  importanceScore: number;
  learningObjectives: string[];
  tags: string[];
  prerequisites: KnowledgeRelationship[];
  unlockedTopics: KnowledgeRelationship[];
  relatedTopics: KnowledgeRelationship[];
  subtopics: KnowledgeTopicSummary[];
  resources: TopicResourceLink[];
}

export interface GraphNode {
  id: string;
  name: string;
  slug: string;
  subjectId: string;
  subjectName: string;
  topicType: TopicType;
  difficultyLevel: TopicDifficulty;
  importanceScore: number;
  prerequisiteCount: number;
  unlockedCount: number;
  isCenter?: boolean;
  center?: boolean;
  // Canvas layout coords (computed client-side)
  x?: number;
  y?: number;
  vx?: number;
  vy?: number;
}

export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  relationshipType: RelationshipType;
  description: string;
  isBidirectional?: boolean;
}

export interface KnowledgeGraph {
  centerTopicId?: string;
  centerTopicName?: string;
  depth?: number;
  nodes: GraphNode[];
  edges: GraphEdge[];
  totalNodes: number;
  totalEdges: number;
  truncated?: boolean;
}

export interface LearningPathStep {
  stepNumber: number;
  topicId: string;
  name: string;
  slug: string;
  subjectName: string;
  difficultyLevel: TopicDifficulty;
  estimatedMinutes: number;
  summary: string;
  isTargetTopic?: boolean;
  targetTopic?: boolean;
  directPrerequisitesCount: number;
}

export interface LearningPath {
  targetTopicId: string;
  targetTopicName: string;
  targetTopicSlug: string;
  totalSteps: number;
  estimatedTotalMinutes: number;
  hasPrerequisites: boolean;
  steps: LearningPathStep[];
}

export interface SearchTopicsParams {
  q?: string;
  subjectId?: string;
  difficulty?: TopicDifficulty;
  topicType?: TopicType;
  page?: number;
  size?: number;
}
