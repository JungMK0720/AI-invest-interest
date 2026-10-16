package com.finmate.admin;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlossaryTermRevisionRepository extends JpaRepository<GlossaryTermRevision, String> {
    List<GlossaryTermRevision> findBySlugOrderByCreatedAtDesc(String slug);
}
