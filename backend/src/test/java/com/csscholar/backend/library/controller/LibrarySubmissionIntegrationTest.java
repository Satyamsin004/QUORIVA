package com.csscholar.backend.library.controller;

import com.csscholar.backend.auth.RegisterRequest;
import com.csscholar.backend.library.dto.CreateLibrarySubmissionRequest;
import com.csscholar.backend.library.dto.LibraryUploadSlotRequest;
import com.csscholar.backend.library.dto.UpdateLibrarySubmissionRequest;
import com.csscholar.backend.library.enums.AcademicCategory;
import com.csscholar.backend.library.enums.AcademicLevel;
import com.csscholar.backend.library.enums.LibraryResourceType;
import com.csscholar.backend.library.enums.RightsType;
import com.csscholar.backend.storage.ObjectStorageService;
import com.csscholar.backend.storage.StorageObjectMetadata;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibrarySubmissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ObjectStorageService objectStorageService;

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        when(objectStorageService.generatePresignedUploadUrl(anyString(), anyString(), anyLong(), any()))
                .thenReturn("https://storage.example.com/mock-upload-url");
        when(objectStorageService.objectExists(anyString())).thenReturn(true);
        when(objectStorageService.getObjectMetadata(anyString())).thenReturn(new StorageObjectMetadata(204800L, "application/pdf", "\"etag\"", Instant.now()));

        String email = "lib_author_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req = new RegisterRequest("Author User", email, "Password123!");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        userToken = root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Requesting upload slot generates isolated library storage key and presigned upload URL")
    void testUploadSlotGeneration() throws Exception {
        LibraryUploadSlotRequest slotReq = new LibraryUploadSlotRequest(
                "Operating_Systems_Concurrency.pdf",
                "PDF",
                1048576L,
                "application/pdf"
        );

        mockMvc.perform(post("/api/v1/library/submissions/upload-url")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resourceId").isNotEmpty())
                .andExpect(jsonPath("$.data.storageKey").value(org.hamcrest.Matchers.startsWith("library/")))
                .andExpect(jsonPath("$.data.uploadUrl").value("https://storage.example.com/mock-upload-url"));
    }

    @Test
    @DisplayName("Validation fails when mandatory rights declaration or license is omitted")
    void testMandatoryRightsValidation() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateLibrarySubmissionRequest req = new CreateLibrarySubmissionRequest();
        req.setId(resourceId);
        req.setTitle("Incomplete Submission");
        req.setDescription("No rights provided");
        req.setCategory(AcademicCategory.COMPUTER_NETWORKS);
        req.setResourceType(LibraryResourceType.ASSIGNMENT);
        req.setStorageKey("library/" + resourceId + "/test.pdf");
        req.setFileName("test.pdf");
        req.setFileType("PDF");
        req.setMimeType("application/pdf");
        req.setFileSize(100L);
        req.setWarrantyAgreed(true);
        // Omit rightsType and licenseName

        mockMvc.perform(post("/api/v1/library/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Validation fails when warrantyAgreed is false or null")
    void testWarrantyAgreedValidation() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateLibrarySubmissionRequest req = new CreateLibrarySubmissionRequest();
        req.setId(resourceId);
        req.setTitle("No Warranty Submission");
        req.setDescription("Test without warranty");
        req.setCategory(AcademicCategory.COMPUTER_NETWORKS);
        req.setResourceType(LibraryResourceType.ASSIGNMENT);
        req.setStorageKey("library/" + resourceId + "/test.pdf");
        req.setFileName("test.pdf");
        req.setFileType("PDF");
        req.setMimeType("application/pdf");
        req.setFileSize(100L);
        req.setRightsType(RightsType.ORIGINAL_WORK);
        req.setLicenseName("MIT License");
        req.setWarrantyAgreed(false);

        mockMvc.perform(post("/api/v1/library/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Contributor must explicitly confirm the copyright and rights declaration warranty")));
    }

    @Test
    @DisplayName("Rights type OTHER requires non-blank sourceAttribution")
    void testRightsTypeOtherRequiresAttribution() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateLibrarySubmissionRequest req = new CreateLibrarySubmissionRequest();
        req.setId(resourceId);
        req.setTitle("Other Rights Without Attribution");
        req.setDescription("Testing OTHER bypass check");
        req.setCategory(AcademicCategory.ALGORITHMS);
        req.setResourceType(LibraryResourceType.LECTURE_NOTES);
        req.setStorageKey("library/" + resourceId + "/other.pdf");
        req.setFileName("other.pdf");
        req.setFileType("PDF");
        req.setMimeType("application/pdf");
        req.setFileSize(5000L);
        req.setRightsType(RightsType.OTHER);
        req.setLicenseName("Custom Rights");
        req.setWarrantyAgreed(true);
        req.setSourceAttribution("   "); // blank

        mockMvc.perform(post("/api/v1/library/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Source attribution and authorization basis must be specified")));
    }

    @Test
    @DisplayName("Storage key with invalid format or path traversal is rejected")
    void testInvalidStorageKeyRejected() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateLibrarySubmissionRequest req = new CreateLibrarySubmissionRequest();
        req.setId(resourceId);
        req.setTitle("Path Traversal Attempt");
        req.setDescription("Testing storage key isolation");
        req.setCategory(AcademicCategory.CYBERSECURITY);
        req.setResourceType(LibraryResourceType.LAB_MANUAL);
        req.setStorageKey("workspaces/" + UUID.randomUUID() + "/hack.pdf"); // workspace key!
        req.setFileName("hack.pdf");
        req.setFileType("PDF");
        req.setMimeType("application/pdf");
        req.setFileSize(5000L);
        req.setRightsType(RightsType.ORIGINAL_WORK);
        req.setLicenseName("CC BY 4.0");
        req.setWarrantyAgreed(true);

        mockMvc.perform(post("/api/v1/library/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid storage key format")));
    }

    @Test
    @DisplayName("Rejects submission if file does not exist in object storage")
    void testMissingObjectInStorageRejectsSubmission() throws Exception {
        UUID resourceId = UUID.randomUUID();
        String missingKey = "library/" + resourceId + "/missing.pdf";
        when(objectStorageService.objectExists(missingKey)).thenReturn(false);

        CreateLibrarySubmissionRequest req = new CreateLibrarySubmissionRequest();
        req.setId(resourceId);
        req.setTitle("Missing S3 File");
        req.setDescription("Testing S3 verification check");
        req.setCategory(AcademicCategory.ALGORITHMS);
        req.setResourceType(LibraryResourceType.TUTORIAL);
        req.setStorageKey(missingKey);
        req.setFileName("missing.pdf");
        req.setFileType("PDF");
        req.setMimeType("application/pdf");
        req.setFileSize(5000L);
        req.setRightsType(RightsType.ORIGINAL_WORK);
        req.setLicenseName("CC BY 4.0");
        req.setWarrantyAgreed(true);

        mockMvc.perform(post("/api/v1/library/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Resource file object was not found in storage")));
    }

    @Test
    @DisplayName("Non-contributor cannot update, submit, or delete another user's submission")
    void testNonContributorCannotModifySubmission() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateLibrarySubmissionRequest req = new CreateLibrarySubmissionRequest();
        req.setId(resourceId);
        req.setTitle("Owner Exclusive Submission");
        req.setDescription("Testing ownership validation");
        req.setCategory(AcademicCategory.SOFTWARE_ENGINEERING);
        req.setResourceType(LibraryResourceType.ASSIGNMENT);
        req.setStorageKey("library/" + resourceId + "/exclusive.pdf");
        req.setFileName("exclusive.pdf");
        req.setFileType("PDF");
        req.setMimeType("application/pdf");
        req.setFileSize(2000L);
        req.setRightsType(RightsType.ORIGINAL_WORK);
        req.setLicenseName("MIT License");
        req.setWarrantyAgreed(true);

        mockMvc.perform(post("/api/v1/library/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Register a different user
        String email2 = "lib_other_" + UUID.randomUUID() + "@example.com";
        RegisterRequest req2 = new RegisterRequest("Other User", email2, "Password123!");
        MvcResult res2 = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root2 = objectMapper.readTree(res2.getResponse().getContentAsString());
        String otherUserToken = root2.path("data").path("accessToken").asText();

        // Other user attempts to update
        UpdateLibrarySubmissionRequest updateReq = new UpdateLibrarySubmissionRequest();
        updateReq.setTitle("Tampered Title");
        mockMvc.perform(put("/api/v1/library/submissions/" + resourceId)
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // Other user attempts to submit
        mockMvc.perform(post("/api/v1/library/submissions/" + resourceId + "/submit")
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        // Other user attempts to delete
        mockMvc.perform(delete("/api/v1/library/submissions/" + resourceId)
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Submission lifecycle: create draft, update, submit for review, withdraw, delete")
    void testSubmissionLifecycle() throws Exception {
        UUID resourceId = UUID.randomUUID();
        CreateLibrarySubmissionRequest req = new CreateLibrarySubmissionRequest();
        req.setId(resourceId);
        req.setTitle("Distributed Systems Architecture Notes");
        req.setDescription("Lecture notes covering consensus, Raft, and Paxos");
        req.setCategory(AcademicCategory.CLOUD_COMPUTING);
        req.setResourceType(LibraryResourceType.LECTURE_NOTES);
        req.setAcademicLevel(AcademicLevel.GRADUATE);
        req.setStorageKey("library/" + resourceId + "/notes.pdf");
        req.setFileName("notes.pdf");
        req.setFileType("PDF");
        req.setMimeType("application/pdf");
        req.setFileSize(204800L);
        req.setRightsType(RightsType.ORIGINAL_WORK);
        req.setLicenseName("CC BY 4.0");
        req.setSourceAttribution("Author original material");
        req.setWarrantyAgreed(true);
        req.setTags(Set.of("distributed", "consensus", "raft"));
        req.setSubmitForReview(false);

        // 1. Create Draft
        mockMvc.perform(post("/api/v1/library/submissions")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.rightsType").value("ORIGINAL_WORK"))
                .andExpect(jsonPath("$.data.licenseName").value("CC BY 4.0"));

        // 2. Update Draft
        UpdateLibrarySubmissionRequest updateReq = new UpdateLibrarySubmissionRequest();
        updateReq.setTitle("Distributed Systems Architecture - 2026 Edition");
        mockMvc.perform(put("/api/v1/library/submissions/" + resourceId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Distributed Systems Architecture - 2026 Edition"));

        // 3. Submit for Review
        mockMvc.perform(post("/api/v1/library/submissions/" + resourceId + "/submit")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        // 4. Duplicate submission attempt on already pending review resource must fail
        mockMvc.perform(post("/api/v1/library/submissions/" + resourceId + "/submit")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Resource is already in review or published")));

        // 5. Withdraw submission
        mockMvc.perform(post("/api/v1/library/submissions/" + resourceId + "/withdraw")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // 6. Check my-submissions listing
        mockMvc.perform(get("/api/v1/library/my-submissions")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(resourceId.toString()));

        // 7. Delete draft
        mockMvc.perform(delete("/api/v1/library/submissions/" + resourceId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

