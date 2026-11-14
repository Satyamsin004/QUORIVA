package com.csscholar.backend.library.dto;

import com.csscholar.backend.library.enums.AcademicCategory;
import com.csscholar.backend.library.enums.AcademicLevel;
import com.csscholar.backend.library.enums.LibraryResourceType;
import com.csscholar.backend.library.enums.RightsType;
import jakarta.validation.constraints.Size;

import java.util.Set;

public class UpdateLibrarySubmissionRequest {

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String description;

    private AcademicCategory category;

    private LibraryResourceType resourceType;

    private AcademicLevel academicLevel;

    private String language;

    private RightsType rightsType;

    private String licenseName;

    private String sourceAttribution;

    private Set<String> tags;

    public UpdateLibrarySubmissionRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AcademicCategory getCategory() {
        return category;
    }

    public void setCategory(AcademicCategory category) {
        this.category = category;
    }

    public LibraryResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(LibraryResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public AcademicLevel getAcademicLevel() {
        return academicLevel;
    }

    public void setAcademicLevel(AcademicLevel academicLevel) {
        this.academicLevel = academicLevel;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public RightsType getRightsType() {
        return rightsType;
    }

    public void setRightsType(RightsType rightsType) {
        this.rightsType = rightsType;
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public String getSourceAttribution() {
        return sourceAttribution;
    }

    public void setSourceAttribution(String sourceAttribution) {
        this.sourceAttribution = sourceAttribution;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}

