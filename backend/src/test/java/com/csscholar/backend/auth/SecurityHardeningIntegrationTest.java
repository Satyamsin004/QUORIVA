package com.csscholar.backend.auth;

import com.csscholar.backend.common.BadRequestException;
import com.csscholar.backend.common.ForbiddenException;
import com.csscholar.backend.common.InternalRequestSigner;
import com.csscholar.backend.common.UnauthorizedException;
import com.csscholar.backend.config.CorrelationIdFilter;
import com.csscholar.backend.config.RateLimitingFilter;
import com.csscholar.backend.config.RateLimitingFilter.EndpointRisk;
import com.csscholar.backend.config.RateLimitingFilter.RateCheckStatus;
import com.csscholar.backend.document.entity.Document;
import com.csscholar.backend.document.enums.FileType;
import com.csscholar.backend.document.enums.ProcessingStatus;
import com.csscholar.backend.document.enums.ScanStatus;
import com.csscholar.backend.document.repository.DocumentRepository;
import com.csscholar.backend.document.service.ClamAvMalwareScanner;
import com.csscholar.backend.document.service.DocumentService;
import com.csscholar.backend.document.service.HeuristicMalwareScanner;
import com.csscholar.backend.document.service.ScanResult;
import com.csscholar.backend.user.Role;
import com.csscholar.backend.user.User;
import com.csscholar.backend.user.UserRepository;
import com.csscholar.backend.workspace.MemberRole;
import com.csscholar.backend.workspace.UpdateWorkspaceRequest;
import com.csscholar.backend.workspace.Workspace;
import com.csscholar.backend.workspace.WorkspaceMember;
import com.csscholar.backend.workspace.WorkspaceMemberRepository;
import com.csscholar.backend.workspace.WorkspaceRepository;
import com.csscholar.backend.workspace.WorkspaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class SecurityHardeningIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private CsrfTokenService csrfTokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        String email = "sec_" + UUID.randomUUID() + "@example.com";
        testUser = new User("Security Test User", email, "EncodedPassword123!", Role.STUDENT);
        testUser = userRepository.save(testUser);
    }

    @Test
    @Transactional
    @DisplayName("Token Family Semantics: Replaying revoked token revokes compromised family while preserving independent device sessions")
    void shouldRevokeCompromisedFamilyWhilePreservingOtherSessions() {
        UUID familyA = UUID.randomUUID();
        UUID familyB = UUID.randomUUID();

        // Device A (Family A): Token A1 (already used/revoked), Token A2 (current active)
        String tokenA1 = tokenProvider.generateRefreshToken(testUser);
        RefreshToken entityA1 = new RefreshToken(testUser, tokenProvider.hashToken(tokenA1), Instant.now().plusSeconds(3600), familyA);
        entityA1.setRevoked(true);
        refreshTokenRepository.save(entityA1);

        String tokenA2 = tokenProvider.generateRefreshToken(testUser);
        RefreshToken entityA2 = new RefreshToken(testUser, tokenProvider.hashToken(tokenA2), Instant.now().plusSeconds(3600), familyA);
        refreshTokenRepository.save(entityA2);

        // Device B (Family B): Token B1 (independent active session)
        String tokenB1 = tokenProvider.generateRefreshToken(testUser);
        RefreshToken entityB1 = new RefreshToken(testUser, tokenProvider.hashToken(tokenB1), Instant.now().plusSeconds(3600), familyB);
        refreshTokenRepository.save(entityB1);

        // Attacker replays revoked token A1 from Family A
        assertThrows(Exception.class, () -> authService.refreshToken(new RefreshTokenRequest(tokenA1)));

        // Verify Device A's remaining active token in Family A is revoked
        RefreshToken refreshedA2 = refreshTokenRepository.findById(entityA2.getId()).orElseThrow();
        assertTrue(refreshedA2.isRevoked(), "All tokens in compromised family A must be revoked");

        // Verify Device B's session in Family B is preserved and still active
        RefreshToken refreshedB1 = refreshTokenRepository.findById(entityB1.getId()).orElseThrow();
        assertFalse(refreshedB1.isRevoked(), "Independent device sessions in family B must not be affected");
    }

    @Test
    @DisplayName("Token Concurrency: Concurrent rotation requests on the same refresh token serialize safely")
    void shouldSerializeConcurrentRefreshTokenRotation() throws Exception {
        String rawToken = tokenProvider.generateRefreshToken(testUser);
        UUID familyId = UUID.randomUUID();
        String tokenHash = tokenProvider.hashToken(rawToken);
        RefreshToken initialToken = new RefreshToken(testUser, tokenHash, Instant.now().plusSeconds(3600), familyId);
        refreshTokenRepository.save(initialToken);

        int concurrency = 5;
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    authService.refreshToken(new RefreshTokenRequest(rawToken));
                    successCount.incrementAndGet();
                } catch (UnauthorizedException ue) {
                    failureCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            }));
        }

        // Release latch simultaneously
        latch.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Exactly one request must succeed in rotating the token
        assertEquals(1, successCount.get(), "Exactly one concurrent refresh request must succeed");
        assertEquals(concurrency - 1, failureCount.get(), "All other concurrent requests must be rejected");

        // Verify the original token is revoked
        RefreshToken reloaded = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        assertTrue(reloaded.isRevoked(), "Original token must be revoked");
    }

    @Test
    @Transactional
    @DisplayName("Workspace IDOR: VIEWER member cannot update workspace metadata")
    void shouldForbidViewerFromModifyingWorkspace() {
        Workspace workspace = new Workspace("CS Project", "Description", testUser);
        workspace = workspaceRepository.save(workspace);

        String viewerEmail = "viewer_" + UUID.randomUUID() + "@example.com";
        User viewer = new User("Viewer User", viewerEmail, "EncodedPassword123!", Role.STUDENT);
        viewer = userRepository.save(viewer);

        WorkspaceMember member = new WorkspaceMember(workspace, viewer, MemberRole.VIEWER);
        workspaceMemberRepository.save(member);

        UpdateWorkspaceRequest updateReq = new UpdateWorkspaceRequest();
        updateReq.setName("Tampered Name");

        final UUID wsId = workspace.getId();
        final UUID viewerId = viewer.getId();
        assertThrows(ForbiddenException.class, () ->
                workspaceService.updateWorkspace(viewerId, wsId, updateReq));
    }

    @Test
    @DisplayName("Internal HMAC Signer: Computes consistent cryptographic headers, canonical query, nonce, and correlation ID")
    void shouldComputeHmacSignatureAndHeaders() {
        String secretKey = "test_internal_secret_key_123";
        String body = "{\"documentId\": \"12345\"}";
        URI uri = URI.create("http://localhost:8000/internal/v1/test?z=last&a=first&m=middle");

        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(uri);
        InternalRequestSigner.signRequest(builder, "POST", uri, body, secretKey);
        HttpRequest request = builder.build();

        assertTrue(request.headers().firstValue(InternalRequestSigner.HEADER_INTERNAL_KEY).isPresent());
        assertTrue(request.headers().firstValue(InternalRequestSigner.HEADER_TIMESTAMP).isPresent());
        assertTrue(request.headers().firstValue(InternalRequestSigner.HEADER_NONCE).isPresent());
        assertTrue(request.headers().firstValue(InternalRequestSigner.HEADER_SIGNATURE).isPresent());

        // Verify deterministic query sorting
        assertEquals("a=first&m=middle&z=last", InternalRequestSigner.canonicalizeQuery(uri.getQuery()));

        String sig = request.headers().firstValue(InternalRequestSigner.HEADER_SIGNATURE).get();
        assertFalse(sig.isBlank());
        assertEquals(64, sig.length()); // SHA-256 hex length
    }

    @Test
    @DisplayName("RateLimitingFilter: Validates trusted proxy to prevent client IP spoofing")
    void shouldPreventIpSpoofingFromUntrustedProxies() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, "127.0.0.1,::1,localhost", null, objectMapper);

        // Untrusted proxy sending spoofed X-Forwarded-For: must be ignored and fallback to remoteAddr
        MockHttpServletRequest untrustedReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        untrustedReq.setRemoteAddr("203.0.113.195");
        untrustedReq.addHeader("X-Forwarded-For", "198.51.100.1");
        assertEquals("203.0.113.195", filter.extractClientIp(untrustedReq));

        // Trusted proxy sending X-Forwarded-For: client IP extracted correctly
        MockHttpServletRequest trustedReq = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        trustedReq.setRemoteAddr("127.0.0.1");
        trustedReq.addHeader("X-Forwarded-For", "198.51.100.22, 127.0.0.1");
        assertEquals("198.51.100.22", filter.extractClientIp(trustedReq));
    }

    @Test
    @DisplayName("RateLimitingFilter: Fails closed on Redis outage for high-cost endpoints, degrades safely for standard endpoints")
    void shouldHandleRedisOutageAccordingToRiskPolicy() {
        StringRedisTemplate mockRedis = mock(StringRedisTemplate.class);
        when(mockRedis.execute(org.mockito.ArgumentMatchers.<org.springframework.data.redis.core.script.RedisScript<Long>>any(), anyList(), any()))
                .thenThrow(new RedisConnectionFailureException("Simulated Redis Outage"));

        RateLimitingFilter filter = new RateLimitingFilter(true, "127.0.0.1", mockRedis, objectMapper);

        // 1. High-Cost endpoint (AI generation) MUST fail closed with STORE_OUTAGE_FAIL_CLOSED
        RateCheckStatus highCostStatus = filter.checkRateLimit("ai_gen:user:u123", 15, EndpointRisk.HIGH_COST);
        assertEquals(RateCheckStatus.STORE_OUTAGE_FAIL_CLOSED, highCostStatus);
        assertEquals(1, filter.getRedisFailureCount());
        assertFalse(filter.isRedisAvailable());

        // 2. Standard endpoint (Authentication) degrades to in-memory counter
        RateCheckStatus standardStatus = filter.checkRateLimit("auth:ip:10.0.0.1", 10, EndpointRisk.STANDARD);
        assertEquals(RateCheckStatus.ALLOWED, standardStatus);
        assertEquals(2, filter.getRedisFailureCount());
    }

    @Test
    @DisplayName("RateLimitingFilter: Enforces max threshold on auth requests with 429 response")
    void shouldEnforceRateLimitingThreshold() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(true, "127.0.0.1,::1,localhost", null, objectMapper);
        filter.reset();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.100");

        // Send 10 requests: all should pass through
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, new MockFilterChain());
            assertEquals(200, response.getStatus());
        }

        // 11th request: rejected with 429
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(request, blockedResponse, new MockFilterChain());
        assertEquals(429, blockedResponse.getStatus());
        assertEquals("60", blockedResponse.getHeader("Retry-After"));
        assertTrue(blockedResponse.getContentAsString().contains("Too many authentication attempts"));
    }

    @Test
    @DisplayName("CorrelationIdFilter: Propagates existing and generates missing correlation IDs")
    void shouldHandleCorrelationIdHeader() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();

        MockHttpServletRequest req1 = new MockHttpServletRequest("GET", "/api/v1/workspaces");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(req1, res1, new MockFilterChain());
        assertNotNull(res1.getHeader("X-Correlation-ID"));

        String customId = "trace-client-abc-123";
        MockHttpServletRequest req2 = new MockHttpServletRequest("GET", "/api/v1/workspaces");
        req2.addHeader("X-Correlation-ID", customId);
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(req2, res2, new MockFilterChain());
        assertEquals(customId, res2.getHeader("X-Correlation-ID"));
    }

    @Test
    @DisplayName("CSRF Token Service: Generates authentic signed tokens, validates session binding, and rejects forged or tampered tokens")
    void shouldVerifyCsrfTokenIntegrity() {
        CsrfTokenService service = new CsrfTokenService("cs_scholar_super_secure_test_csrf_secret_key_32_bytes!");

        String sessionA = UUID.randomUUID().toString();
        String sessionB = UUID.randomUUID().toString();

        // 1. Valid token generated for sessionA
        String tokenA = service.generateToken(sessionA);
        assertNotNull(tokenA);
        assertTrue(service.validateToken(tokenA, sessionA));
        assertTrue(service.validateToken(tokenA, null)); // anonymous/unbound validation

        // 2. Mismatched session binding fails
        assertFalse(service.validateToken(tokenA, sessionB));

        // 3. Forged token with invalid signature fails
        byte[] decoded = Base64.getUrlDecoder().decode(tokenA);
        String tokenStr = new String(decoded, StandardCharsets.UTF_8);
        String tamperedTokenStr = tokenStr.substring(0, tokenStr.length() - 5) + "00000";
        String forgedToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedTokenStr.getBytes(StandardCharsets.UTF_8));
        assertFalse(service.validateToken(forgedToken, null));

        // 4. Forged payload with altered session ID fails signature check
        String tamperedPayloadStr = tokenStr.replace(sessionA, sessionB);
        String forgedPayloadToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedPayloadStr.getBytes(StandardCharsets.UTF_8));
        assertFalse(service.validateToken(forgedPayloadToken, null));

        // 5. Blank/null/malformed token fails gracefully
        assertFalse(service.validateToken(null, null));
        assertFalse(service.validateToken("", null));
        assertFalse(service.validateToken("not:a:valid:token", null));
    }

    @Test
    @DisplayName("HeuristicMalwareScanner: Detects dangerous signatures beyond the first 4 MB")
    void shouldDetectThreatBeyondInitialBuffer() {
        HeuristicMalwareScanner scanner = new HeuristicMalwareScanner();

        // Generate an input stream where test signature is placed at offset 5 MB (beyond old 4 MB limit)
        int cleanPrefixSize = 5 * 1024 * 1024; // 5 MB clean prefix
        byte[] cleanPrefix = new byte[cleanPrefixSize];
        Arrays.fill(cleanPrefix, (byte) 'A');

        byte[] sigBytes = HeuristicMalwareScanner.TEST_VIRUS_SIG.getBytes(StandardCharsets.ISO_8859_1);
        byte[] suffix = "Clean trailing content".getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.writeBytes(cleanPrefix);
        baos.writeBytes(sigBytes);
        baos.writeBytes(suffix);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ScanResult result = scanner.scan(bais, "large_syllabus.pdf");

        assertFalse(result.isClean(), "Signature beyond 4MB must be detected");
        assertEquals(ScanStatus.INFECTED, result.status());
    }

    @Test
    @DisplayName("HeuristicMalwareScanner: Detects unauthorized macros in later OpenXML zip archive entries")
    void shouldDetectMacrosInLaterZipArchiveEntries() throws Exception {
        HeuristicMalwareScanner scanner = new HeuristicMalwareScanner();

        // Create a ZIP archive representing an OpenXML DOCX package
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Entry 1: clean content types
            zos.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zos.write("<Types></Types>".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Entry 2: clean document body
            zos.putNextEntry(new ZipEntry("word/document.xml"));
            zos.write("<document>Chapter 1: CS Theory</document>".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Entry 3: embedded malicious macro project
            zos.putNextEntry(new ZipEntry("word/vbaProject.bin"));
            zos.write(new byte[]{0x4D, 0x5A, 0x00, 0x00}); // dummy binary
            zos.closeEntry();
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ScanResult result = scanner.scan(bais, "coursework.docx");

        assertFalse(result.isClean());
        assertEquals(ScanStatus.SUSPICIOUS, result.status());
        assertTrue(result.threatName().contains("VbaProject"));
    }

    @Test
    @DisplayName("ClamAvMalwareScanner: Fails closed with SCAN_FAILED when daemon is unreachable")
    void shouldFailClosedOnClamAvDaemonUnavailability() {
        // Point to a non-listening port to simulate daemon crash/outage
        ClamAvMalwareScanner scanner = new ClamAvMalwareScanner("127.0.0.1", 65432, 500);

        byte[] cleanContent = "Clean academic research text".getBytes(StandardCharsets.UTF_8);
        ScanResult result = scanner.scan(new ByteArrayInputStream(cleanContent), "paper.pdf");

        assertFalse(result.isClean(), "Scanner must not report clean when daemon is unavailable");
        assertEquals(ScanStatus.SCAN_FAILED, result.status());
        assertFalse(result.isAcceptable());
        assertTrue(result.details().contains("unreachable") || result.details().contains("timed out") || result.details().contains("Connection refused"));
    }

    @Test
    @Transactional
    @DisplayName("Document Service: Blocks download when scan status is not CLEAN")
    void shouldBlockDownloadForNonCleanDocuments() {
        Workspace ws = new Workspace("Security WS", "Test", testUser);
        ws = workspaceRepository.save(ws);

        Document doc = new Document(
                testUser, ws, "Infected Paper", "paper.pdf",
                "workspaces/" + ws.getId() + "/paper.pdf",
                FileType.PDF, "application/pdf", 1024L, "dummyhash", "Description"
        );
        doc.setProcessingStatus(ProcessingStatus.FAILED);
        doc.setScanStatus(ScanStatus.INFECTED);
        doc = documentRepository.save(doc);

        final UUID docId = doc.getId();
        final UUID userId = testUser.getId();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                documentService.generateDownloadUrl(userId, docId));

        assertTrue(ex.getMessage().contains("security scan status"));
    }
}
