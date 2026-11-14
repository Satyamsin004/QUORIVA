package com.csscholar.backend.library.repository;

import com.csscholar.backend.library.entity.LibraryResourceTag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LibraryResourceTagRepository extends JpaRepository<LibraryResourceTag, UUID> {

    List<LibraryResourceTag> findByResourceId(UUID resourceId);

    @Query("SELECT t.tag FROM LibraryResourceTag t GROUP BY t.tag ORDER BY COUNT(t) DESC")
    List<String> findPopularTags(Pageable pageable);
}

