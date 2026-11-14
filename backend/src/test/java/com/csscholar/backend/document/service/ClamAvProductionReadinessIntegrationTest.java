package com.csscholar.backend.document.service;

import com.csscholar.backend.document.enums.ScanStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ClamAvProductionReadinessIntegrationTest {

    @Test
    @DisplayName("Health indicator reports DOWN when ClamAV is required in production but disabled")
    void shouldReportDownWhenRequiredButDisabled() {
        MalwareScanningProperties properties = new MalwareScanningProperties();
        properties.getClamav().setEnabled(false);
        properties.getClamav().setRequired(true);

        ClamAvHealthIndicator indicator = new ClamAvHealthIndicator(properties);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("DISABLED_BUT_REQUIRED", health.getDetails().get("clamav"));
    }

    @Test
    @DisplayName("Health indicator reports DOWN when ClamAV is required in production but daemon is unreachable")
    void shouldReportDownWhenRequiredAndDaemonUnreachable() {
        MalwareScanningProperties properties = new MalwareScanningProperties();
        properties.getClamav().setEnabled(true);
        properties.getClamav().setRequired(true);
        properties.getClamav().setHost("127.0.0.1");
        properties.getClamav().setPort(59998); // closed port
        properties.getClamav().setTimeoutMs(500);

        ClamAvHealthIndicator indicator = new ClamAvHealthIndicator(properties);
        Health health = indicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("UNAVAILABLE", health.getDetails().get("clamav"));
    }

    @Test
    @DisplayName("Health indicator reports UP with advisory when ClamAV is optional in development")
    void shouldReportUpWhenOptionalInDevelopment() {
        MalwareScanningProperties properties = new MalwareScanningProperties();
        properties.getClamav().setEnabled(false);
        properties.getClamav().setRequired(false);

        ClamAvHealthIndicator indicator = new ClamAvHealthIndicator(properties);
        Health health = indicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals("DISABLED", health.getDetails().get("clamav"));
    }

    @Test
    @DisplayName("Composite scanner fails closed when ClamAV is required in production but disabled")
    void shouldFailClosedInCompositeScannerWhenRequiredButDisabled() {
        MalwareScanningProperties properties = new MalwareScanningProperties();
        properties.getClamav().setEnabled(false);
        properties.getClamav().setRequired(true);

        ClamAvMalwareScanner clamScanner = new ClamAvMalwareScanner(properties);
        HeuristicMalwareScanner heuristicScanner = new HeuristicMalwareScanner();
        CompositeMalwareScanner compositeScanner = new CompositeMalwareScanner(clamScanner, heuristicScanner, properties);

        InputStream stream = new ByteArrayInputStream("Harmless academic content".getBytes(StandardCharsets.UTF_8));
        ScanResult result = compositeScanner.scan(stream, "paper.pdf");

        assertEquals(ScanStatus.SCAN_FAILED, result.status());
        assertFalse(result.isAcceptable());
        assertTrue(result.details().contains("required in production"));
    }

    @Test
    @DisplayName("ClamAV scanner streams complete file using zINSTREAM protocol and correctly handles clean response")
    void shouldStreamCompleteFileAndVerifyCleanResponse() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            CountDownLatch serverDone = new CountDownLatch(1);
            ByteArrayOutputStream receivedData = new ByteArrayOutputStream();

            Thread serverThread = new Thread(() -> {
                try (Socket client = serverSocket.accept();
                     InputStream in = client.getInputStream();
                     OutputStream out = client.getOutputStream()) {

                    // Read zINSTREAM\0 command
                    byte[] cmd = in.readNBytes(10);
                    assertEquals("zINSTREAM\0", new String(cmd, StandardCharsets.US_ASCII));

                    // Read chunks
                    DataInputStream din = new DataInputStream(in);
                    while (true) {
                        int length = din.readInt();
                        if (length == 0) break;
                        byte[] chunk = din.readNBytes(length);
                        receivedData.write(chunk);
                    }

                    // Respond OK
                    out.write("stream: OK\n".getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                } catch (Exception e) {
                    // test handling
                } finally {
                    serverDone.countDown();
                }
            });
            serverThread.start();

            ClamAvMalwareScanner scanner = new ClamAvMalwareScanner("127.0.0.1", port, 2000);
            String payload = "Comprehensive academic document data streamed to ClamAV";
            ScanResult result = scanner.scan(new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8)), "test.pdf");

            serverDone.await(5, TimeUnit.SECONDS);

            assertEquals(ScanStatus.CLEAN, result.status());
            assertTrue(result.isAcceptable());
            assertEquals(payload, receivedData.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    @DisplayName("ClamAV scanner parses threat detection response from daemon")
    void shouldParseThreatDetectionFromDaemon() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Thread serverThread = new Thread(() -> {
                try (Socket client = serverSocket.accept();
                     InputStream in = client.getInputStream();
                     OutputStream out = client.getOutputStream()) {

                    // Read zINSTREAM\0
                    in.readNBytes(10);
                    DataInputStream din = new DataInputStream(in);
                    while (true) {
                        int length = din.readInt();
                        if (length == 0) break;
                        din.readNBytes(length);
                    }

                    // Respond threat FOUND
                    out.write("stream: Win.Trojan.Agent-12345 FOUND\n".getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                } catch (Exception ignored) {}
            });
            serverThread.start();

            ClamAvMalwareScanner scanner = new ClamAvMalwareScanner("127.0.0.1", port, 2000);
            ScanResult result = scanner.scan(new ByteArrayInputStream("evil data".getBytes()), "malicious.pdf");

            assertEquals(ScanStatus.INFECTED, result.status());
            assertEquals("Win.Trojan.Agent-12345", result.threatName());
            assertFalse(result.isAcceptable());
        }
    }

    @Test
    @DisplayName("ClamAV scanner fails closed on socket timeout")
    void shouldFailClosedOnSocketTimeout() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();

            Thread hangingServer = new Thread(() -> {
                try (Socket client = serverSocket.accept()) {
                    // Do not respond; wait until client times out
                    Thread.sleep(2000);
                } catch (Exception ignored) {}
            });
            hangingServer.start();

            ClamAvMalwareScanner scanner = new ClamAvMalwareScanner("127.0.0.1", port, 500);
            ScanResult result = scanner.scan(new ByteArrayInputStream("data".getBytes()), "slow.pdf");

            assertEquals(ScanStatus.SCAN_FAILED, result.status());
            assertFalse(result.isAcceptable());
        }
    }
}

