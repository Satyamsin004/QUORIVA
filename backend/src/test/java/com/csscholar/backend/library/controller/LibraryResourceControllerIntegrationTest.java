package com.csscholar.backend.library.controller;

import com.csscholar.backend.auth.LoginRequest;
import com.csscholar.backend.auth.RegisterRequest;
import com.csscholar.backend.library.dto.CreateLibrarySubmissionRequest;
import com.csscholar.backend.library.enums.AcademicCategory;
import com.csscholar.backend.library.enums.AcademicLevel;
import com.csscholar.backend.library.enums.LibraryResourceType;
import com.csscholar.backend.library.enums.RightsType;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibrarySubmissionService submissionService;

    @MockBean
    private ObjectStorageService objectStorageService;

    private String studentToken;
    private User studentUser;
    private String educatorToken;
    private User educatorUser;
    private String secondStudentToken;
    private User secondStudentUser;

    @BeforeEach
    void setUp() throws Exception {
        when(objectStorageService.generatePresignedDownloadUrl(any(), any(), any()))
                .thenReturn("https://storage.example.com/mock-download-url");
        when(objectStorageService.objectExists(anyString())).thenReturn(true);
        when(objectStorageService.getObjectMetadata(anyString())).thenReturn(new StorageObjectMetadata(4096L, "application/pdf", "\"etag\"", Instant.now()));

        // 1. Create Student 1 (contributor)
        String studentEmail = "lib_student_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req1 = new RegisterRequest("Student Lib", studentEmail, "Password123!");
        MvcResult res1 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        studentToken = root1.path("data").path("accessToken").asText();
        studentUser = userRepository.findByEmail(studentEmail).orElseThrow();

        // 2. Create Student 2 (unrelated student)
        String studentEmail2 = "lib_student2_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req2 = new RegisterRequest("Second Student", studentEmail2, "Password123!");
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root2 = objectMapper.readTree(res2.getResponse().getContentAsString());
        secondStudentToken = root2.path("data").path("accessToken").asText();
        secondStudentUser = userRepository.findByEmail(studentEmail2).orElseThrow();

        // 3. Create Educator
        String educatorEmail = "lib_educator_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req3 = new RegisterRequest("Educator Lib", educatorEmail, "Password123!");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req3)))
                .andExpect(status().isCreated());
        educatorUser = userRepository.findByEmail(educatorEmail).orElseThrow();
        educatorUser.setRole(Role.EDUCATOR);
        userRepository.save(educatorUser);

        // Login as educator to get token with EDUCATOR role
        LoginRequest loginReq = new LoginRequest(educatorEmail, "Password123!");
        MvcResult loginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root3 = objectMapper.readTree(loginRes.getResponse().getContentAsString());
        educatorToken = root3.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Metadata discovery endpoints return available categories, resource types, and rights")
    void testMetadataEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/library/categories")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/library/resource-types")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/library/rights-types")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/library/academic-levels")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Published resource is discoverable and downloadable, draft resource is isolated")
    void testDiscoveryAndIsolation() throws Exception {
        // Create draft resource
        UUID resourceIdDraft = UUID.randomUUID();
        CreateLibrarySubmissionRequest draftReq = new CreateLibrarySubmissionRequest();
        draftReq.setId(resourceIdDraft);
        draftReq.setTitle("Private Draft Lecture Notes");
        draftReq.setDescription("These are draft notes");
        draftReq.setCategory(AcademicCategory.OPERATING_SYSTEMS);
        draftReq.setResourceType(LibraryResourceType.LECTURE_NOTES);
        draftReq.setStorageKey("library/" + resourceIdDraft + "/draft.pdf");
        draftReq.setFileName("draft.pdf");
        draftReq.setFileType("PDF");
        draftReq.setMimeType("application/pdf");
        draftReq.setFileSize(2048L);
        draftReq.setRightsType(RightsType.ORIGINAL_WORK);
        draftReq.setLicenseName("CC BY 4.0");
        draftReq.setWarrantyAgreed(true);
        draftReq.setSubmitForReview(false);

        submissionService.createSubmission(draftReq, studentUser.getId());

        // Verify draft does NOT appear in public library search
        mockMvc.perform(get("/api/v1/library/resources?q=Private Draft")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        // Submit another resource and approve it via educator
        UUID resourceIdPub = UUID.randomUUID();
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String uniqueTitle = "AdvAlgoBook_" + uniqueSuffix;
        CreateLibrarySubmissionRequest pubReq = new CreateLibrarySubmissionRequest();
        pubReq.setId(resourceIdPub);
        pubReq.setTitle(uniqueTitle);
        pubReq.setDescription("Comprehensive guide on graph theory and dynamic programming");
        pubReq.setCategory(AcademicCategory.ALGORITHMS);
        pubReq.setResourceType(LibraryResourceType.TEXTBOOK);
        pubReq.setAcademicLevel(AcademicLevel.UNDERGRADUATE_UPPER);
        pubReq.setStorageKey("library/" + resourceIdPub + "/algorithms.pdf");
        pubReq.setFileName("algorithms.pdf");
        pubReq.setFileType("PDF");
        pubReq.setMimeType("application/pdf");
        pubReq.setFileSize(4096L);
        pubReq.setRightsType(RightsType.CREATIVE_COMMONS);
        pubReq.setLicenseName("CC BY-SA 4.0");
        pubReq.setSourceAttribution("Prof. Turing Open Courseware");
        pubReq.setTags(Set.of("graphs", "dp", "algorithms"));
        pubReq.setWarrantyAgreed(true);
        pubReq.setSubmitForReview(true);

        submissionService.createSubmission(pubReq, studentUser.getId());

        // Educator approves it
        mockMvc.perform(post("/api/v1/library/moderation/resources/" + resourceIdPub + "/approve")
                        .header("Authorization", "Bearer " + educatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Excellent open-access handbook\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // Now public discovery returns it
        mockMvc.perform(get("/api/v1/library/resources?q=" + uniqueTitle)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value(uniqueTitle))
                .andExpect(jsonPath("$.data.content[0].category").value("ALGORITHMS"))
                .andExpect(jsonPath("$.data.content[0].licenseName").value("CC BY-SA 4.0"))
                // Storage key must NEVER be exposed in public search results
                .andExpect(jsonPath("$.data.content[0].storageKey").doesNotExist());

        // Check detail view
        mockMvc.perform(get("/api/v1/library/resources/" + resourceIdPub)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(uniqueTitle))
                .andExpect(jsonPath("$.data.canDownload").value(true))
                // Storage key must NEVER be exposed in public resource detail
                .andExpect(jsonPath("$.data.storageKey").doesNotExist());

        // Test download URL generation for published resource
        mockMvc.perform(get("/api/v1/library/resources/" + resourceIdPub + "/download")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").value("https://storage.example.com/mock-download-url"));
    }

    @Test
    @DisplayName("Download authorization matrix: non-contributor forbidden on unpublished, contributor and moderator allowed")
    void testDownloadAuthorizationMatrix() throws Exception {
        UUID resId = UUID.randomUUID();
        CreateLibrarySubmissionRequest req = new CreateLibrarySubmissionRequest();
        req.setId(resId);
        req.setTitle("Auth Matrix Document");
        req.setDescription("Testing download permissions");
        req.setCategory(AcademicCategory.COMPUTER_NETWORKS);
        req.setResourceType(LibraryResourceType.LECTURE_NOTES);
        req.setStorageKey("library/" + resId + "/auth_matrix.pdf");
        req.setFileName("auth_matrix.pdf");
        req.setFileType("PDF");
        req.setMimeType("application/pdf");
        req.setFileSize(2048L);
        req.setRightsType(RightsType.ORIGINAL_WORK);
        req.setLicenseName("MIT License");
        req.setWarrantyAgreed(true);
        req.setSubmitForReview(false); // DRAFT

        submissionService.createSubmission(req, studentUser.getId());

        // 1. Unrelated student is forbidden (403) from downloading draft
        mockMvc.perform(get("/api/v1/library/resources/" + resId + "/download")
                        .header("Authorization", "Bearer " + secondStudentToken))
                .andExpect(status().isForbidden());

        // 2. Contributor can download own draft
        mockMvc.perform(get("/api/v1/library/resources/" + resId + "/download")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").value("https://storage.example.com/mock-download-url"));

        // 3. Educator/moderator can download draft for review
        mockMvc.perform(get("/api/v1/library/resources/" + resId + "/download")
                        .header("Authorization", "Bearer " + educatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.downloadUrl").value("https://storage.example.com/mock-download-url"));

        // Submit for review -> PENDING_REVIEW
        submissionService.submitForReview(resId, studentUser.getId());

        // 4. Unrelated student is forbidden (403) from downloading pending review resource
        mockMvc.perform(get("/api/v1/library/resources/" + resId + "/download")
                        .header("Authorization", "Bearer " + secondStudentToken))
                .andExpect(status().isForbidden());

        // 5. Contributor can still download own pending resource
        mockMvc.perform(get("/api/v1/library/resources/" + resId + "/download")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());

        // 6. Educator can download pending resource for moderation review
        mockMvc.perform(get("/api/v1/library/resources/" + resId + "/download")
                        .header("Authorization", "Bearer " + educatorToken))
                .andExpect(status().isOk());
    }
}
