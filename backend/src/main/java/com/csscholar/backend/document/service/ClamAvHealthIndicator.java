package com.csscholar.backend.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Spring Boot Actuator HealthIndicator for the ClamAV Antivirus daemon.
 *
 * Health semantics:
 * - If ClamAV is required (clamav.required=true) and unreachable, reports DOWN (blocking readiness).
 * - If ClamAV is required but disabled, reports DOWN.
 * - If ClamAV is enabled and reachable (responds PONG to zPING), reports UP.
 * - If ClamAV is disabled or optional in development, reports UP with advisory details.
 */
@Component("clamav")
public class ClamAvHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(ClamAvHealthIndicator.class);

    private final MalwareScanningProperties properties;

    public ClamAvHealthIndicator(MalwareScanningProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (properties == null || properties.getClamav() == null || !properties.getClamav().isEnabled()) {
            if (properties != null && properties.getClamav() != null && properties.getClamav().isRequired()) {
                return Health.down()
                        .withDetail("clamav", "DISABLED_BUT_REQUIRED")
                        .withDetail("error", "ClamAV is required by production policy but disabled in configuration.")
                        .build();
            }
            return Health.up()
                    .withDetail("clamav", "DISABLED")
                    .withDetail("notice", "ClamAV daemon is disabled. Heuristic screening active.")
                    .build();
        }

        String host = properties.getClamav().getHost();
        int port = properties.getClamav().getPort();
        int timeout = Math.min(properties.getClamav().getTimeoutMs(), 3000);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            socket.setSoTimeout(timeout);

            try (OutputStream out = socket.getOutputStream();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))) {
                out.write("zPING\0".getBytes(StandardCharsets.US_ASCII));
                out.flush();

                String line = in.readLine();
                if (line != null && line.trim().equals("PONG")) {
                    return Health.up()
                            .withDetail("clamav", "CONNECTED")
                            .withDetail("host", host + ":" + port)
                            .build();
                } else {
                    return handleFailure("Unexpected PING response from daemon: " + line, host, port);
                }
            }
        } catch (Exception e) {
            return handleFailure("Connection failed: " + e.getMessage(), host, port);
        }
    }

    private Health handleFailure(String error, String host, int port) {
        boolean required = properties != null && properties.getClamav() != null && properties.getClamav().isRequired();
        if (required) {
            log.error("ClamAV daemon health check failed (policy requires active daemon): {}", error);
            return Health.down()
                    .withDetail("clamav", "UNAVAILABLE")
                    .withDetail("host", host + ":" + port)
                    .withDetail("error", error)
                    .build();
        } else {
            log.warn("ClamAV daemon health check failed (optional in development): {}", error);
            return Health.up()
                    .withDetail("clamav", "UNAVAILABLE_OPTIONAL")
                    .withDetail("host", host + ":" + port)
                    .withDetail("warning", error)
                    .build();
        }
    }
}

