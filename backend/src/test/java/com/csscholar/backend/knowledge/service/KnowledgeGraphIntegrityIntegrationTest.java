package com.csscholar.backend.knowledge.service;

import com.csscholar.backend.knowledge.dto.KnowledgeTopicDetailResponse;
import com.csscholar.backend.knowledge.dto.LearningPathResponse;
import com.csscholar.backend.knowledge.dto.LearningPathStepResponse;
import com.csscholar.backend.knowledge.entity.AcademicSubject;
import com.csscholar.backend.knowledge.entity.KnowledgeTopic;
import com.csscholar.backend.knowledge.entity.KnowledgeTopicRelationship;
import com.csscholar.backend.knowledge.entity.KnowledgeTopicResource;
import com.csscholar.backend.knowledge.enums.DifficultyLevel;
import com.csscholar.backend.knowledge.enums.RelationshipType;
import com.csscholar.backend.knowledge.enums.TopicType;
import com.csscholar.backend.knowledge.exception.KnowledgeGraphIntegrityException;
import com.csscholar.backend.knowledge.repository.*;
import com.csscholar.backend.library.entity.LibraryResource;
import com.csscholar.backend.library.enums.*;
import com.csscholar.backend.library.repository.LibraryResourceRepository;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KnowledgeGraphIntegrityIntegrationTest {

    @Autowired
    private KnowledgeGraphIntegrityService integrityService;

    @Autowired
    private KnowledgePathService pathService;

    @Autowired
    private KnowledgeTopicService topicService;

    @Autowired
    private KnowledgeTopicRepository topicRepository;

    @Autowired
    private AcademicSubjectRepository subjectRepository;

    @Autowired
    private KnowledgeTopicRelationshipRepository relationshipRepository;

    @Autowired
    private KnowledgeTopicResourceRepository topicResourceRepository;

    @Autowired
    private LibraryResourceRepository libraryResourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private KnowledgeTopic topicA;
    private KnowledgeTopic topicB;
    private KnowledgeTopic topicC;

    @BeforeEach
    void setUp() {
        AcademicSubject subject = subjectRepository.findBySlug("programming-fundamentals")
                .orElseThrow();

        topicA = topicRepository.save(new KnowledgeTopic(
                subject, "Test Concept A " + UUID.randomUUID(), "test-concept-a-" + UUID.randomUUID(),
                "Summary A", "Desc A", DifficultyLevel.BEGINNER, TopicType.CONCEPT, 30, BigDecimal.valueOf(0.9)
        ));
        topicB = topicRepository.save(new KnowledgeTopic(
                subject, "Test Concept B " + UUID.randomUUID(), "test-concept-b-" + UUID.randomUUID(),
                "Summary B", "Desc B", DifficultyLevel.INTERMEDIATE, TopicType.CONCEPT, 45, BigDecimal.valueOf(0.8)
        ));
        topicC = topicRepository.save(new KnowledgeTopic(
                subject, "Test Concept C " + UUID.randomUUID(), "test-concept-c-" + UUID.randomUUID(),
                "Summary C", "Desc C", DifficultyLevel.ADVANCED, TopicType.CONCEPT, 60, BigDecimal.valueOf(0.7)
        ));
        topicRepository.flush();
    }

    @Test
    @DisplayName("Allows valid prerequisite chain A -> B -> C and verifies topological ordering")
    void testValidPrerequisiteChainAndTopologicalOrder() {
        // A -> B
        integrityService.validatePrerequisiteEdge(topicA.getId(), topicB.getId());
        relationshipRepository.save(new KnowledgeTopicRelationship(topicA, topicB, RelationshipType.PREREQUISITE_OF, "A prereq of B", BigDecimal.valueOf(1.0)));

        // B -> C
        integrityService.validatePrerequisiteEdge(topicB.getId(), topicC.getId());
        relationshipRepository.save(new KnowledgeTopicRelationship(topicB, topicC, RelationshipType.PREREQUISITE_OF, "B prereq of C", BigDecimal.valueOf(1.0)));

        // Generate learning path for C
        LearningPathResponse path = pathService.generateLearningPath(topicC.getSlug());
        assertThat(path.totalSteps()).isGreaterThanOrEqualTo(3);

        List<LearningPathStepResponse> steps = path.steps();
        int idxA = -1, idxB = -1, idxC = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).topicId().equals(topicA.getId())) idxA = i;
            if (steps.get(i).topicId().equals(topicB.getId())) idxB = i;
            if (steps.get(i).topicId().equals(topicC.getId())) idxC = i;
        }

        assertThat(idxA).isGreaterThanOrEqualTo(0);
        assertThat(idxB).isGreaterThan(idxA);
        assertThat(idxC).isGreaterThan(idxB);
        assertThat(steps.get(steps.size() - 1).topicId()).isEqualTo(topicC.getId());
    }

    @Test
    @DisplayName("Service validation rejects self-reference prerequisite")
    void testServiceRejectsSelfReference() {
        assertThatThrownBy(() -> integrityService.validatePrerequisiteEdge(topicA.getId(), topicA.getId()))
                .isInstanceOf(KnowledgeGraphIntegrityException.class)
                .hasMessageContaining("Self-referential prerequisite is forbidden");
    }

    @Test
    @DisplayName("Service validation rejects duplicate prerequisite edge")
    void testServiceRejectsDuplicateEdge() {
        relationshipRepository.save(new KnowledgeTopicRelationship(topicA, topicB, RelationshipType.PREREQUISITE_OF, "A prereq B", BigDecimal.valueOf(1.0)));

        assertThatThrownBy(() -> integrityService.validatePrerequisiteEdge(topicA.getId(), topicB.getId()))
                .isInstanceOf(KnowledgeGraphIntegrityException.class)
                .hasMessageContaining("Duplicate prerequisite edge");
    }

    @Test
    @DisplayName("Service validation detects indirect cycle C -> A when A -> B -> C exists")
    void testServiceDetectsCycle() {
        relationshipRepository.save(new KnowledgeTopicRelationship(topicA, topicB, RelationshipType.PREREQUISITE_OF, "A prereq B", BigDecimal.valueOf(1.0)));
        relationshipRepository.save(new KnowledgeTopicRelationship(topicB, topicC, RelationshipType.PREREQUISITE_OF, "B prereq C", BigDecimal.valueOf(1.0)));

        // Attempting C -> A should throw cycle detection exception
        assertThatThrownBy(() -> integrityService.validatePrerequisiteEdge(topicC.getId(), topicA.getId()))
                .isInstanceOf(KnowledgeGraphIntegrityException.class)
                .hasMessageContaining("Prerequisite cycle detected");
    }

    @Test
    @DisplayName("Database trigger rejects cyclic insertion directly at the SQL level")
    void testDatabaseTriggerRejectsCycle() {
        // Insert A -> B
        jdbcTemplate.update(
                "INSERT INTO knowledge_topic_relationships (id, source_topic_id, target_topic_id, relationship_type) VALUES (gen_random_uuid(), ?, ?, 'PREREQUISITE_OF')",
                topicA.getId(), topicB.getId()
        );
        // Insert B -> C
        jdbcTemplate.update(
                "INSERT INTO knowledge_topic_relationships (id, source_topic_id, target_topic_id, relationship_type) VALUES (gen_random_uuid(), ?, ?, 'PREREQUISITE_OF')",
                topicB.getId(), topicC.getId()
        );

        // Attempt direct SQL insertion of C -> A, expecting DB trigger exception
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO knowledge_topic_relationships (id, source_topic_id, target_topic_id, relationship_type) VALUES (gen_random_uuid(), ?, ?, 'PREREQUISITE_OF')",
                topicC.getId(), topicA.getId()
        )).hasMessageContaining("Prerequisite cycle detected");
    }

    @Test
    @DisplayName("Database check constraint and trigger reject self-reference at the SQL level")
    void testDatabaseRejectsSelfReference() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO knowledge_topic_relationships (id, source_topic_id, target_topic_id, relationship_type) VALUES (gen_random_uuid(), ?, ?, 'PREREQUISITE_OF')",
                topicA.getId(), topicA.getId()
        )).isNotNull();
    }

    @Test
    @DisplayName("Verifies curriculum coverage completion with all 13 subjects and 65+ topics")
    void testCurriculumCoverageCompletion() {
        List<AcademicSubject> subjects = subjectRepository.findAll();
        assertThat(subjects.size()).isGreaterThanOrEqualTo(13);

        List<String> expectedCodes = List.of(
                "CS-PROG", "CS-DSA", "CS-DBMS", "CS-OS", "CS-CN", "CS-SE", "CS-AI", "CS-SYS",
                "CS-ARCH", "CS-COMP", "CS-TOC", "CS-SEC", "CS-WEB"
        );
        List<String> actualCodes = subjects.stream().map(AcademicSubject::getCode).toList();
        assertThat(actualCodes).containsAll(expectedCodes);

        long totalTopics = topicRepository.count();
        assertThat(totalTopics).isGreaterThanOrEqualTo(65);

        // Verify each new subject has at least 4 topics
        for (String code : List.of("CS-ARCH", "CS-COMP", "CS-TOC", "CS-SEC", "CS-WEB")) {
            AcademicSubject sub = subjectRepository.findByCode(code).orElseThrow();
            List<KnowledgeTopic> subTopics = topicRepository.findBySubjectIdOrderByImportanceScoreDesc(sub.getId());
            assertThat(subTopics.size()).isGreaterThanOrEqualTo(4);
        }
    }

    @Test
    @DisplayName("Global graph acyclicity verification passes on complete seeded curriculum")
    void testCompleteGraphAcyclicity() {
        KnowledgeGraphIntegrityService.IntegrityReport report = integrityService.verifyEntireGraphAcyclic();
        assertThat(report.isAcyclic()).isTrue();
        assertThat(report.totalTopics()).isGreaterThanOrEqualTo(65);
        assertThat(report.totalPrerequisiteEdges()).isGreaterThanOrEqualTo(60);
    }

    @Test
    @DisplayName("Resource mapping integrity: only PUBLISHED library resources are returned, suspended/rejected excluded")
    void testResourceMappingPublicationIntegrity() {
        User user = userRepository.save(new User("Test Uploader", "uploader_" + UUID.randomUUID() + "@example.com", "hash", Role.EDUCATOR));

        // Create 1 published resource and 1 suspended resource
        LibraryResource publishedRes = new LibraryResource();
        publishedRes.setId(UUID.randomUUID());
        publishedRes.setContributor(user);
        publishedRes.setTitle("Published Compiler Guide");
        publishedRes.setDescription("A comprehensive guide to LLVM AST.");
        publishedRes.setCategory(AcademicCategory.COMPILER_DESIGN);
        publishedRes.setResourceType(LibraryResourceType.TUTORIAL);
        publishedRes.setAcademicLevel(AcademicLevel.UNDERGRADUATE_UPPER);
        publishedRes.setRightsType(RightsType.CREATIVE_COMMONS);
        publishedRes.setLicenseName("CC BY 4.0");
        publishedRes.setSourceAttribution("Prof. Compiler");
        publishedRes.setStorageKey("library/" + UUID.randomUUID() + "/guide.pdf");
        publishedRes.setFileName("guide.pdf");
        publishedRes.setFileType("pdf");
        publishedRes.setMimeType("application/pdf");
        publishedRes.setFileSize(1024L);
        publishedRes.setStatus(LibraryResourceStatus.PUBLISHED);
        publishedRes = libraryResourceRepository.save(publishedRes);

        LibraryResource suspendedRes = new LibraryResource();
        suspendedRes.setId(UUID.randomUUID());
        suspendedRes.setContributor(user);
        suspendedRes.setTitle("Suspended Compiler Notes");
        suspendedRes.setDescription("Notes pending review.");
        suspendedRes.setCategory(AcademicCategory.COMPILER_DESIGN);
        suspendedRes.setResourceType(LibraryResourceType.LECTURE_NOTES);
        suspendedRes.setAcademicLevel(AcademicLevel.UNDERGRADUATE_UPPER);
        suspendedRes.setRightsType(RightsType.CREATIVE_COMMONS);
        suspendedRes.setLicenseName("CC BY 4.0");
        suspendedRes.setSourceAttribution("Author");
        suspendedRes.setStorageKey("library/" + UUID.randomUUID() + "/notes.pdf");
        suspendedRes.setFileName("notes.pdf");
        suspendedRes.setFileType("pdf");
        suspendedRes.setMimeType("application/pdf");
        suspendedRes.setFileSize(2048L);
        suspendedRes.setStatus(LibraryResourceStatus.SUSPENDED);
        suspendedRes = libraryResourceRepository.save(suspendedRes);

        KnowledgeTopic compilerTopic = topicRepository.findBySlug("intermediate-representation-ast").orElseThrow();

        // Link both resources to the topic
        topicResourceRepository.save(new KnowledgeTopicResource(compilerTopic, publishedRes, "Core AST reference"));
        topicResourceRepository.save(new KnowledgeTopicResource(compilerTopic, suspendedRes, "Suspended notes reference"));

        // Query topic detail at read time
        KnowledgeTopicDetailResponse detail = topicService.getTopicDetail(compilerTopic.getSlug());

        // Verify only PUBLISHED resource is returned
        assertThat(detail.publishedResources()).hasSize(1);
        assertThat(detail.publishedResources().get(0).resourceId()).isEqualTo(publishedRes.getId());
        assertThat(detail.publishedResources().get(0).title()).isEqualTo("Published Compiler Guide");

        // Now dynamically change publishedRes to SUSPENDED and verify exclusion at read time
        publishedRes.setStatus(LibraryResourceStatus.SUSPENDED);
        libraryResourceRepository.save(publishedRes);

        KnowledgeTopicDetailResponse updatedDetail = topicService.getTopicDetail(compilerTopic.getSlug());
        assertThat(updatedDetail.publishedResources()).isEmpty();
    }
}
