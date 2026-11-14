package com.csscholar.backend.document.enums;

/**
 * Lifecycle status of document malware screening and antivirus inspection.
 */
public enum ScanStatus {
    PENDING,
    CLEAN,
    SUSPICIOUS,
    INFECTED,
    SCAN_FAILED
}

