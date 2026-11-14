package com.csscholar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * CS Scholar AI - Main Backend Application Entry Point.
 */
@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class CsScholarBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsScholarBackendApplication.class, args);
    }
}
