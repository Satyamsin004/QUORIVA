package com.csscholar.backend.document.service;

import com.csscholar.backend.document.enums.ScanStatus;

/**
 * Result of malware screening and antivirus inspection.
 */
public record ScanResult(
        ScanStatus status,
        String threatName,
        String details
) {
    public boolean isClean() {
        return status == ScanStatus.CLEAN;
    }

    public boolean isAcceptable() {
        return status == ScanStatus.CLEAN;
    }

    public static ScanResult clean() {
        return new ScanResult(ScanStatus.CLEAN, null, "No heuristic threats detected (heuristic screening only).");
    }

    public static ScanResult suspicious(String threatName, String details) {
        return new ScanResult(ScanStatus.SUSPICIOUS, threatName, details);
    }

    public static ScanResult infected(String threatName, String details) {
        return new ScanResult(ScanStatus.INFECTED, threatName, details);
    }

    public static ScanResult failed(String reason) {
        return new ScanResult(ScanStatus.SCAN_FAILED, null, reason);
    }

    public static ScanResult threat(String threatName, String details) {
        return new ScanResult(ScanStatus.INFECTED, threatName, details);
    }
}
