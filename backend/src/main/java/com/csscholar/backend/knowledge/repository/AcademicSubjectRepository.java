package com.csscholar.backend.knowledge.repository;

import com.csscholar.backend.knowledge.entity.AcademicSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicSubjectRepository extends JpaRepository<AcademicSubject, UUID> {

    Optional<AcademicSubject> findBySlug(String slug);

    Optional<AcademicSubject> findByCode(String code);

    List<AcademicSubject> findAllByOrderByDisplayOrderAsc();
}

