package com.finmate.glossary;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlossaryBookmarkRepository extends JpaRepository<GlossaryBookmark, GlossaryBookmarkId> {
    List<GlossaryBookmark> findByIdUserIdOrderByCreatedAtDesc(String userId);
}
