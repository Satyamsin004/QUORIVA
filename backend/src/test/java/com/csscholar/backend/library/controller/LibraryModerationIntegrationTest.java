package com.csscholar.backend.library.controller;

import com.csscholar.backend.auth.LoginRequest;
import com.csscholar.backend.auth.RegisterRequest;
import com.csscholar.backend.library.dto.CreateLibrarySubmissionRequest;
import com.csscholar.backend.library.dto.CreateReportRequest;
import com.csscholar.backend.library.dto.ReportResolutionRequest;
import com.csscholar.backend.library.enums.*;
import com.csscholar.backend.library.service.LibrarySubmissionService;
import com.csscholar.backend.storage.ObjectStorageService;
import com.csscholar.backend.storage.StorageObjectMetadata;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryModerationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibrarySubmissionService submissionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ObjectStorageService objectStorageService;

    private String studentToken;
    private User studentUser;
    private String educatorToken;
    private User educatorUser;
    private String adminToken;
    private User adminUser;
    private String researcherToken;
    private User researcherUser;

    @BeforeEach
    void setUp() throws Exception {
        when(objectStorageService.objectExists(anyString())).thenReturn(true);
        when(objectStorageService.getObjectMetadata(anyString())).thenReturn(new StorageObjectMetadata(51200L, "application/pdf", "\"etag\"", Instant.now()));

        // 1. Register Student
        String studentEmail = "mod_student_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req1 = new RegisterRequest("Student Moderation", studentEmail, "Password123!");
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        studentToken = root1.path("data").path("accessToken").asText();
        studentUser = userRepository.findByEmail(studentEmail).orElseThrow();

        // 2. Register Educator
        String educatorEmail = "mod_educator_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req2 = new RegisterRequest("Educator Moderation", educatorEmail, "Password123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated());
        educatorUser = userRepository.findByEmail(educatorEmail).orElseThrow();
        educatorUser.setRole(Role.EDUCATOR);
        userRepository.save(educatorUser);

        LoginRequest loginReqEducator = new LoginRequest(educatorEmail, "Password123!");
        MvcResult loginResEducator = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqEducator)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root2 = objectMapper.readTree(loginResEducator.getResponse().getContentAsString());
        educatorToken = root2.path("data").path("accessToken").asText();

        // 3. Register Admin
        String adminEmail = "mod_admin_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req3 = new RegisterRequest("Admin Moderation", adminEmail, "Password123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated());
        adminUser = userRepository.findByEmail(adminEmail).orElseThrow();
        adminUser.setRole(Role.ADMIN);
        userRepository.save(adminUser);

        LoginRequest loginReqAdmin = new LoginRequest(adminEmail, "Password123!");
        MvcResult loginResAdmin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqAdmin)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root3 = objectMapper.readTree(loginResAdmin.getResponse().getContentAsString());
        adminToken = root3.path("data").path("accessToken").asText();

        // 4. Register Researcher
        String researcherEmail = "mod_researcher_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req4 = new RegisterRequest("Researcher Moderation", researcherEmail, "Password123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req4)))
                .andExpect(status().isCreated());
        researcherUser = userRepository.findByEmail(researcherEmail).orElseThrow();
        researcherUser.setRole(Role.RESEARCHER);
        userRepository.save(researcherUser);

        LoginRequest loginReqResearcher = new LoginRequest(researcherEmail, "Password123!");
        MvcResult loginResResearcher = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReqResearcher)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root4 = objectMapper.readTree(loginResResearcher.getResponse().getContentAsString());
        researcherToken = root4.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Students and Researchers are forbidden (403) from moderation queue, while Educators and Admins are permitted (200)")
    void testModerationRoleMatrix() throws Exception {
        // Student forbidden
        mockMvc.perform(get("/api/v1/library/moderation/queue")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Researcher forbidden
        mockMvc.perform(get("/api/v1/library/moderation/queue")
                        .header("Authorization", "Bearer " + researcherToken))
                .andExpect(status().isForbidden());

        // Educator permitted
        mockMvc.perform(get("/api/v1/library/moderation/queue")
                        .header("Authorization", "Bearer " + educatorToken))
                .andExpect(status().isOk());

        // Admin permitted
        mockMvc.perform(get("/api/v1/library/moderation/queue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Educator can view queue, approve, reject with reason, suspend, and view audit trail")
    void testModeratorWorkflowAndAudit() throws Exception {
        // Create 2 submissions pending review
        UUID resId1 = UUID.randomUUID();
        CreateLibrarySubmissionRequest sub1 = new CreateLibrarySubmissionRequest();
        sub1.setId(resId1);
        sub1.setTitle("Database Management Systems Relational Algebra");
        sub1.setDescription("Study guide on relational algebra expressions");
        sub1.setCategory(AcademicCategory.DATABASE_SYSTEMS);
        sub1.setResourceType(LibraryResourceType.TUTORIAL);
        sub1.setStorageKey("library/" + resId1 + "/dbms.pdf");
        sub1.setFileName("dbms.pdf");
        sub1.setFileType("PDF");
        sub1.setMimeType("application/pdf");
        sub1.setFileSize(51200L);
        sub1.setRightsType(RightsType.ORIGINAL_WORK);
        sub1.setLicenseName("MIT License");
        sub1.setWarrantyAgreed(true);
        sub1.setSubmitForReview(true);
        submissionService.createSubmission(sub1, studentUser.getId());

        UUID resId2 = UUID.randomUUID();
        CreateLibrarySubmissionRequest sub2 = new CreateLibrarySubmissionRequest();
        sub2.setId(resId2);
        sub2.setTitle("Low Quality Scanned Notes");
        sub2.setDescription("Unreadable scan");
        sub2.setCategory(AcademicCategory.OTHER);
        sub2.setResourceType(LibraryResourceType.OTHER);
        sub2.setStorageKey("library/" + resId2 + "/lowqual.pdf");
        sub2.setFileName("lowqual.pdf");
        sub2.setFileType("PDF");
        sub2.setMimeType("application/pdf");
        sub2.setFileSize(1024L);
        sub2.setRightsType(RightsType.OTHER);
        sub2.setLicenseName("Unknown");
        sub2.setSourceAttribution("Donated notes from library archive");
        sub2.setWarrantyAgreed(true);
        sub2.setSubmitForReview(true);
        submissionService.createSubmission(sub2, studentUser.getId());

        // 1. Educator inspects moderation queue
        mockMvc.perform(get("/api/v1/library/moderation/queue")
                        .header("Authorization", "Bearer " + educatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        // 2. Educator approves res1
        mockMvc.perform(post("/api/v1/library/moderation/resources/" + resId1 + "/approve")
                        .header("Authorization", "Bearer " + educatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Approved: high quality verified academic material.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 3. Educator rejects res2 without reason -> 400 Bad Request
        mockMvc.perform(post("/api/v1/library/moderation/resources/" + resId2 + "/reject")
                        .header("Authorization", "Bearer " + educatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        // 4. Educator rejects res2 with valid reason
        mockMvc.perform(post("/api/v1/library/moderation/resources/" + resId2 + "/reject")
                        .header("Authorization", "Bearer " + educatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"File is illegible and license is unspecified. Please re-upload a clear copy with explicit rights.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").isNotEmpty());

        // 5. Suspend approved res1
        mockMvc.perform(post("/api/v1/library/moderation/resources/" + resId1 + "/suspend")
                        .header("Authorization", "Bearer " + educatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Investigating license discrepancy inquiry.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // 6. Restore suspended res1
        mockMvc.perform(post("/api/v1/library/moderation/resources/" + resId1 + "/restore")
                        .header("Authorization", "Bearer " + educatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"License verified authentic, resource restored.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 7. Verify audit logs for res1
        mockMvc.perform(get("/api/v1/library/moderation/resources/" + resId1 + "/logs")
                        .header("Authorization", "Bearer " + educatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));

        // 8. Ordinary student is forbidden from viewing moderation audit logs
        mockMvc.perform(get("/api/v1/library/moderation/resources/" + resId1 + "/logs")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Database-level trigger strictly prohibits UPDATE or DELETE on library_moderation_logs")
    void testDatabaseLevelAuditLogImmutability() {
        // Create an audit entry via moderation
        UUID resId = UUID.randomUUID();
        CreateLibrarySubmissionRequest sub = new CreateLibrarySubmissionRequest();
        sub.setId(resId);
        sub.setTitle("Audit Log Immutability Test Doc");
        sub.setDescription("Testing PostgreSQL immutability trigger");
        sub.setCategory(AcademicCategory.CYBERSECURITY);
        sub.setResourceType(LibraryResourceType.RESEARCH_PAPER);
        sub.setStorageKey("library/" + resId + "/audit_test.pdf");
        sub.setFileName("audit_test.pdf");
        sub.setFileType("PDF");
        sub.setMimeType("application/pdf");
        sub.setFileSize(20480L);
        sub.setRightsType(RightsType.ORIGINAL_WORK);
        sub.setLicenseName("CC BY 4.0");
        sub.setWarrantyAgreed(true);
        sub.setSubmitForReview(true);
        submissionService.createSubmission(sub, studentUser.getId());

        // Educator approves to create a log entry
        try {
            mockMvc.perform(post("/api/v1/library/moderation/resources/" + resId + "/approve")
                            .header("Authorization", "Bearer " + educatorToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"Approved for immutability test\"}"))
                    .andExpect(status().isOk());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Attempt direct UPDATE via SQL on library_moderation_logs -> must throw exception
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE library_moderation_logs SET reason = 'tampered reason' WHERE resource_id = ?",
                resId
        )).hasMessageContaining("strictly immutable and cannot be updated or deleted");

        // Attempt direct DELETE via SQL on library_moderation_logs -> must throw exception
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM library_moderation_logs WHERE resource_id = ?",
                resId
        )).hasMessageContaining("strictly immutable and cannot be updated or deleted");
    }

    @Test
    @DisplayName("Reporting resource and resolving report via moderation workflow")
    void testReportingWorkflow() throws Exception {
        UUID resId = UUID.randomUUID();
        CreateLibrarySubmissionRequest sub = new CreateLibrarySubmissionRequest();
        sub.setId(resId);
        sub.setTitle("Computer Networks Guide");
        sub.setDescription("TCP/IP overview");
        sub.setCategory(AcademicCategory.COMPUTER_NETWORKS);
        sub.setResourceType(LibraryResourceType.TUTORIAL);
        sub.setStorageKey("library/" + resId + "/networks.pdf");
        sub.setFileName("networks.pdf");
        sub.setFileType("PDF");
        sub.setMimeType("application/pdf");
        sub.setFileSize(40960L);
        sub.setRightsType(RightsType.ORIGINAL_WORK);
        sub.setLicenseName("CC BY 4.0");
        sub.setWarrantyAgreed(true);
        sub.setSubmitForReview(true);
        submissionService.createSubmission(sub, studentUser.getId());

        // Student submits report
        CreateReportRequest reportReq = new CreateReportRequest(
                ReportReasonCategory.COPYRIGHT_INFRINGEMENT,
                "This document appears copied from textbook chapter 4 without permission."
        );
        MvcResult reportRes = mockMvc.perform(post("/api/v1/library/resources/" + resId + "/reports")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        JsonNode reportNode = objectMapper.readTree(reportRes.getResponse().getContentAsString());
        String reportId = reportNode.path("data").path("id").asText();

        // Moderator retrieves reports
        mockMvc.perform(get("/api/v1/library/moderation/reports?status=PENDING")
                        .header("Authorization", "Bearer " + educatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // Moderator resolves report
        ReportResolutionRequest resolveReq = new ReportResolutionRequest(
                ReportStatus.RESOLVED,
                "Reviewed and confirmed valid educational summary created by contributor, attribution updated."
        );
        mockMvc.perform(post("/api/v1/library/moderation/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + educatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }
}

