package com.csscholar.backend.library.enums;

public enum RightsType {
    ORIGINAL_WORK("Original Work (Contributor is Author)"),
    PUBLIC_DOMAIN("Public Domain (CC0 / Expired Copyright)"),
    CREATIVE_COMMONS("Creative Commons Licensed"),
    INSTITUTIONAL_AUTHORIZED("Institutionally Authorized for Sharing"),
    OPEN_ACCESS("Open Access Academic Publication"),
    OTHER("Other Permitted Educational Use");

    private final String displayName;

    RightsType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

